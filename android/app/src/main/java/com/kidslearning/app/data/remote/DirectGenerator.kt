package com.kidslearning.app.data.remote

import android.content.Context
import com.kidslearning.app.data.local.SourceImages
import com.kidslearning.app.domain.model.AnswerKey
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.LessonChecks
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * On-device lesson generation for personal-use mode: the same two-stage pipeline
 * as the backend (analyze+plan on the cheap model, generate on the strong model,
 * then deterministic validation), running directly against the Anthropic API with
 * the parent's own key. No server anywhere; images and text go straight from the
 * phone to the API.
 *
 * The prompts are a faithful port of backend/app/prompts/lesson.py -- if you tune
 * one, tune the other.
 */
class DirectGenerator(apiKey: String, private val context: Context) {

    private val client = AnthropicClient(apiKey)

    // images is the FULL list to store with the lesson: the parent's uploads first,
    // then any photos fetched for image_search terms. image_ref indices point into it.
    data class DirectResult(
        val lesson: Lesson?,
        val errors: List<String>,
        val images: List<ByteArray> = emptyList(),
    )

    /**
     * Follow-up practice set for a finished lesson: a handful of NEW questions on
     * the concepts the child found hard, at a difficulty the parent/child picked.
     * Reuses the full two-stage pipeline with the base lesson's own explanations
     * as the source material, so practice stays grounded in what was taught.
     */
    suspend fun generatePractice(
        base: Lesson,
        weakConceptIds: List<String>,
        difficulty: String,
        numQuestions: Int,
    ): DirectResult {
        val weakNames = base.concepts
            .filter { it.conceptId in weakConceptIds }
            .map { it.name }
            .ifEmpty { base.concepts.map { it.name } }

        val difficultyNote = when (difficulty) {
            "easier" -> "noticeably EASIER than the original lesson: smaller steps, " +
                "more scaffolding, simpler wording"
            "harder" -> "noticeably HARDER than the original lesson: less scaffolding, " +
                "multi-step thinking, trickier distractors"
            else -> "the same difficulty as the original lesson"
        }

        val taught = base.sections.filterIsInstance<ExplanationSection>()
            .joinToString("\n\n") { "${it.title}\n${it.content}" }
        val alreadyAsked = base.sections
            .mapNotNull { AnswerKey.questionText(it) }
            .joinToString("\n") { "- $it" }

        return generate(
            topic = "Practice: ${base.title}",
            subject = base.subject,
            age = base.age,
            objective = "Focused practice on: ${weakNames.joinToString(", ")}. " +
                "Make every question $difficultyNote. Open with ONE very short " +
                "animated_story recap (2-3 scenes), then go straight into " +
                "activities. Never repeat a question from the ALREADY ASKED list.",
            sourceText = "WHAT WAS TAUGHT:\n$taught\n\nQUESTIONS ALREADY ASKED " +
                "(write different ones):\n$alreadyAsked",
            uploadedImages = emptyList(),
            numQuestions = numQuestions,
        )
    }

    suspend fun generate(
        topic: String,
        subject: String,
        age: Int,
        objective: String,
        sourceText: String,
        uploadedImages: List<ByteArray>,
        numQuestions: Int = 6,
        language: String = "en",
    ): DirectResult {
        val uploadedBase64 = uploadedImages.map(SourceImages::toVisionBase64)

        // Stage 1: read the source (text + images) and produce a teaching plan.
        val plan = client.generateStructured(
            model = AnthropicClient.MODEL_SMALL,
            system = ANALYZE_SYSTEM,
            prompt = analyzePrompt(topic, subject, age, objective, sourceText, language),
            schema = planSchema(),
            images = uploadedBase64,
            maxTokens = 4096,
        )

        // Stage 2: turn the plan into a full lesson conforming to the shared schema.
        val imageIndexNote = if (uploadedBase64.isEmpty()) "" else
            "\n\nThere are ${uploadedBase64.size} attached image(s), numbered 0 to " +
                "${uploadedBase64.size - 1} in the order attached. Sections about a graph, " +
                "chart, table, or picture visible in one of them MUST set image_ref to that number."
        val rawLesson = client.generateStructured(
            model = AnthropicClient.MODEL_STRONG,
            system = LESSON_SYSTEM,
            prompt = lessonPrompt(plan.toString(), topic, subject, age, objective, numQuestions, language) +
                imageIndexNote,
            schema = lessonSchema(),
            images = uploadedBase64,
            maxTokens = 16384,
        )

        // Recover the recoverable: unwrap, fill known metadata, derive concepts.
        var candidate = normalize(rawLesson, topic, subject, age, objective, language)

        // Fetch real photos for image_search terms and rewrite them to image_ref,
        // appending the fetched images after the uploaded ones.
        val (resolved, allImages) = resolveImageSearches(candidate, uploadedImages)
        candidate = resolved

        var lesson = tryDecode(candidate)
        if (lesson == null) {
            // One repair pass: show the model its own output and the exact failure.
            val problem = decodeFailure(candidate) ?: "unknown validation problem"
            val repaired = client.generateStructured(
                model = AnthropicClient.MODEL_STRONG,
                system = LESSON_SYSTEM,
                prompt = repairPrompt(candidate.toString(), problem),
                schema = lessonSchema(),
                maxTokens = 16384,
            )
            candidate = normalize(repaired, topic, subject, age, objective, language)
            // The repair keeps already-resolved image_ref values; do not refetch.
            lesson = tryDecode(candidate)
                ?: return DirectResult(
                    null,
                    listOf("The AI produced an invalid lesson twice: ${decodeFailure(candidate)?.take(200)}"),
                    allImages,
                )
        }

        return DirectResult(lesson, LessonChecks.validate(lesson, allImages.size), allImages)
    }

    /**
     * Walk the lesson JSON, fetch a Wikipedia lead photo for each distinct
     * image_search term (on sections and on tap_image options), append them after
     * the uploaded images, and rewrite each image_search into an image_ref index.
     * Terms that fetch nothing are left as-is (renderers fall back to emoji/label).
     */
    private suspend fun resolveImageSearches(
        candidate: JsonObject,
        uploadedImages: List<ByteArray>,
    ): Pair<JsonObject, List<ByteArray>> {
        val terms = collectSearchTerms(candidate)
        if (terms.isEmpty()) return candidate to uploadedImages

        val fetched = LinkedHashMap<String, Int>() // term -> image index
        val images = uploadedImages.toMutableList()
        for (term in terms) {
            val bytes = WikipediaImages.fetch(term) ?: continue
            fetched[term] = images.size
            images.add(bytes)
        }
        if (fetched.isEmpty()) return candidate to uploadedImages

        val sections = (candidate["sections"] as? JsonArray) ?: return candidate to images
        val rewrittenSections = buildJsonArray {
            sections.forEach { element ->
                val section = element.jsonObject
                add(rewriteSectionImages(section, fetched))
            }
        }
        val rewritten = buildJsonObject {
            candidate.forEach { (k, v) -> if (k != "sections") put(k, v) }
            put("sections", rewrittenSections)
        }
        return rewritten to images
    }

    private fun collectSearchTerms(candidate: JsonObject): List<String> {
        val terms = LinkedHashSet<String>()
        (candidate["sections"] as? JsonArray)?.forEach { element ->
            val section = element.jsonObject
            (section["image_search"] as? JsonPrimitive)?.content
                ?.takeIf { it.isNotBlank() }?.let { terms.add(it) }
            (section["options"] as? JsonArray)?.forEach { opt ->
                (opt.jsonObject["image_search"] as? JsonPrimitive)?.content
                    ?.takeIf { it.isNotBlank() }?.let { terms.add(it) }
            }
            (section["scenes"] as? JsonArray)?.forEach { scene ->
                (scene.jsonObject["image_search"] as? JsonPrimitive)?.content
                    ?.takeIf { it.isNotBlank() }?.let { terms.add(it) }
            }
        }
        return terms.toList()
    }

    private fun rewriteSectionImages(section: JsonObject, fetched: Map<String, Int>): JsonObject =
        buildJsonObject {
            section.forEach { (key, value) ->
                when {
                    key == "image_search" -> {
                        val idx = (value as? JsonPrimitive)?.content?.let { fetched[it] }
                        if (idx != null) put("image_ref", JsonPrimitive(idx)) else put(key, value)
                    }
                    (key == "options" || key == "scenes") && value is JsonArray -> {
                        putJsonArray(key) {
                            value.forEach { opt ->
                                val o = opt.jsonObject
                                add(buildJsonObject {
                                    o.forEach { (ok, ov) ->
                                        if (ok == "image_search") {
                                            val idx = (ov as? JsonPrimitive)?.content?.let { fetched[it] }
                                            if (idx != null) put("image_ref", JsonPrimitive(idx))
                                            else put(ok, ov)
                                        } else put(ok, ov)
                                    }
                                })
                            }
                        }
                    }
                    else -> put(key, value)
                }
            }
        }

    private fun tryDecode(candidate: JsonObject): Lesson? = runCatching {
        LessonJson.codec.decodeFromJsonElement(Lesson.serializer(), candidate)
    }.getOrNull()

    private fun decodeFailure(candidate: JsonObject): String? = runCatching {
        LessonJson.codec.decodeFromJsonElement(Lesson.serializer(), candidate)
    }.exceptionOrNull()?.message

    /**
     * Make the model's output decodable wherever that is safe to do mechanically:
     * unwrap a nested {"lesson": {...}} shape, always assign a fresh unique
     * lesson_id (prevents a generated id colliding with a saved lesson), fill
     * metadata we already know from the form, and derive the concepts list from
     * the sections if the model forgot it. Anything pedagogical stays untouched --
     * real content problems still fail into the repair pass.
     */
    private fun normalize(
        raw: JsonObject, topic: String, subject: String,
        age: Int, objective: String, language: String,
    ): JsonObject {
        var obj = raw
        if ("sections" !in obj) {
            obj.values.filterIsInstance<JsonObject>()
                .firstOrNull { "sections" in it }
                ?.let { obj = it }
        }
        val sections = obj["sections"] as? JsonArray

        return buildJsonObject {
            obj.forEach { (key, value) ->
                if (key !in setOf("schema_version", "lesson_id", "age")) put(key, value)
            }
            put("schema_version", "1.0")
            put("lesson_id", "gen_${System.currentTimeMillis()}")
            put("age", age)
            if ("title" !in obj) put("title", topic)
            if ("subject" !in obj) put("subject", subject)
            if ("language" !in obj) put("language", language)
            if ("difficulty" !in obj) put("difficulty", "beginner")
            if ("completion_message" !in obj) {
                put("completion_message", "Great job — you finished the lesson! 🎉")
            }
            if ("learning_objectives" !in obj) {
                putJsonArray("learning_objectives") {
                    add(JsonPrimitive(objective.ifBlank { "Learn about $topic" }))
                }
            }
            val hasConcepts = (obj["concepts"] as? JsonArray)?.isNotEmpty() == true
            if (!hasConcepts) {
                val conceptIds = sections
                    ?.mapNotNull { (it as? JsonObject)?.get("concept_id") }
                    ?.mapNotNull { (it as? JsonPrimitive)?.content }
                    ?.distinct()
                    .orEmpty()
                    .ifEmpty { listOf("main_idea") }
                putJsonArray("concepts") {
                    conceptIds.forEach { id ->
                        addJsonObject {
                            put("concept_id", id)
                            put("name", id.replace('_', ' ')
                                .replaceFirstChar { c -> c.uppercase() })
                        }
                    }
                }
            }
        }
    }

    private fun repairPrompt(badJson: String, problem: String) = """
        The lesson JSON below failed validation with this problem:
        $problem

        Return the complete corrected lesson object via the tool. Keep all the
        teaching content, fix only the structure: every required field present,
        every section following the schema exactly (correct "type" values, all
        required per-type fields, hint / correct_feedback / incorrect_feedback on
        every activity).

        INVALID LESSON JSON:
        $badJson
    """.trimIndent()

    /**
     * The shared lesson JSON Schema, bundled as an asset — one contract everywhere.
     * Keywords the API's tool-following handles poorly (unevaluatedProperties,
     * document identifiers) are stripped; the strictness they encode is enforced
     * by decoding + LessonChecks on the result instead.
     */
    private fun lessonSchema(): JsonObject =
        context.assets.open("lesson.schema.json").bufferedReader().use { it.readText() }
            .let { LessonJson.codec.parseToJsonElement(it) }
            .let(::sanitizeSchema).jsonObject

    private fun sanitizeSchema(element: JsonElement): JsonElement = when (element) {
        is JsonObject -> buildJsonObject {
            element.forEach { (key, value) ->
                if (key !in setOf("unevaluatedProperties", "${'$'}schema", "${'$'}id")) {
                    put(key, sanitizeSchema(value))
                }
            }
        }
        is JsonArray -> buildJsonArray { element.forEach { add(sanitizeSchema(it)) } }
        else -> element
    }

    private fun planSchema(): JsonObject = LessonJson.codec.parseToJsonElement(
        """
        {
          "type": "object",
          "required": ["concepts", "misconceptions", "teaching_order", "activity_plan"],
          "properties": {
            "concepts": {"type": "array", "items": {"type": "object"}},
            "misconceptions": {"type": "array", "items": {"type": "string"}},
            "teaching_order": {"type": "array", "items": {"type": "string"}},
            "activity_plan": {"type": "array", "items": {"type": "object"}}
          }
        }
        """.trimIndent()
    ).jsonObject

    private fun ageRules(age: Int): String {
        val (maxOptions, sentenceWords, wordBank) = when {
            age <= 6 -> Triple(3, 12, "Do NOT ask the child to type. For fill-in-the-blank always provide a word_bank of tappable choices.")
            age <= 9 -> Triple(4, 18, "Short typed answers are acceptable where appropriate.")
            age <= 13 -> Triple(5, 25, "Short typed answers are acceptable where appropriate.")
            else -> Triple(6, 35, "Short typed answers are acceptable where appropriate.")
        }
        return """
            Age adaptation (child is $age):
            - Use at most $maxOptions answer options per question.
            - Keep sentences under about $sentenceWords words.
            - $wordBank
        """.trimIndent()
    }

    private fun analyzePrompt(
        topic: String, subject: String, age: Int,
        objective: String, sourceText: String, language: String,
    ) = """
        Analyse the material below and produce a teaching plan for a $age-year-old.

        Subject: $subject
        Topic: $topic
        Learning objective: $objective
        Language for the lesson: $language

        ${ageRules(age)}

        Produce:
        1. The main concepts (each with a short stable snake_case id).
        2. Key facts, vocabulary, and examples, tagged source-explicit vs general knowledge.
        3. The common misconceptions a child this age holds about these concepts —
           these become the wrong answers, so be specific.
        4. The order concepts should be taught.
        5. A progression of activities from easy to challenging.

        SOURCE MATERIAL (any attached images are also source material — read them
        fully, including diagrams, tables, and handwriting):
        ""${'"'}
        $sourceText
        ""${'"'}
    """.trimIndent()

    private fun lessonPrompt(
        plan: String, topic: String, subject: String, age: Int,
        objective: String, numQuestions: Int, language: String,
    ) = """
        Using the teaching plan below, write a complete lesson for a $age-year-old.

        Subject: $subject
        Topic: $topic
        Learning objective: $objective
        Language: $language
        Aim for about $numQuestions interactive activities.

        ${ageRules(age)}

        OPEN the lesson with ONE "animated_story" section: 3-6 short scenes that
        tell the core idea as a tiny story the app plays with animation, narration,
        and sounds. Each scene is one or two short sentences plus a visual (a big
        emoji, an image_search photo, or an uploaded image_ref). Make it warm and
        concrete — meet a character, watch something happen — not a list of facts.

        After the story, structure the lesson as: alternating explanation cards
        and activities building from difficulty_level 1 upward, ending with a
        challenge activity and a warm completion_message. Use only these section
        types: animated_story, explanation, chart, multiple_choice, true_false,
        fill_in_the_blank, match_pairs, drag_into_order, build_bar_chart,
        tap_image, sort_into_categories, number_line. Vary the activity types so
        the lesson stays fresh. Give every section a unique snake_case id.
        Every activity must reference a concept_id declared in the concepts list.

        TEACHING PLAN:
        ""${'"'}
        $plan
        ""${'"'}
    """.trimIndent()

    private companion object {
        val ANALYZE_SYSTEM = """
            You are an expert curriculum analyst and instructional designer for
            children. You read supplied educational material and produce a
            structured analysis and teaching plan. You are precise about what the
            source actually says versus general knowledge.
        """.trimIndent()

        val LESSON_SYSTEM = """
            You are an expert children's lesson author. You turn a teaching plan
            into a complete, structured, interactive lesson. You output only data
            that conforms to the provided schema.

            Quality rules you must follow:
            - Ground every fact: grounding.basis is "source_explicit" for facts in
              the material, "general_knowledge" for standard facts, or
              "interpretation" when reasoned from the source. Never invent facts.
            - Wrong answers must be plausible mistakes a learner of this age would
              actually make, drawn from the misconceptions. Never joke options.
            - incorrect_feedback must gently teach toward the correct idea, never a
              bare "try again". correct_feedback briefly reinforces WHY.
            - Every question needs a hint that nudges without revealing the answer.
            - No duplicate questions; each activity tests something distinct.
            - Uploaded images ARE the source material: read them fully.
            - CRITICAL image rule: when a section teaches from or asks about a
              graph, pictograph, chart, table, diagram, or picture that appears
              in an uploaded image, you MUST set that section's image_ref to the
              image's 0-based index — the child has to SEE it to answer. Use
              image_description ONLY for things NOT visible in any uploaded
              image, and never set both on the same section. Only reference
              images actually provided; never invent an index.
            - You can CREATE data visuals natively — no image needed. Use a "chart"
              section (chart_type "bar" or "pictograph"; items with
              label/value/emoji; for pictographs set symbol_value, give every item
              an emoji symbol, and make each value a multiple of symbol_value) to
              present data as a colourful graph, then ask questions about it.
              PREFER recreating a graph from the source as a chart section over
              attaching its photo — it is clearer for the child. Use the
              "build_bar_chart" activity (items with label/target, plus max_value)
              to let the child BUILD a graph by dragging bars to the right heights.
            - REAL PHOTOS on demand: any section may set "image_search" to a
              concrete noun phrase matching a Wikipedia article title ("Jupiter",
              "Bengal tiger"). The app fetches that article's lead photo and shows
              it. Use it to SHOW real things (planets, animals, landmarks) when no
              uploaded image covers them. Keep terms unambiguous.
            - "tap_image": the child taps the correct picture. Give each option an
              "image_search" for a real photo AND an "emoji" fallback.
            - "sort_into_categories": drag items into 2-4 labelled buckets
              (living/non-living, inner/outer planets, nouns/verbs).
            - "number_line": the child slides a marker to the answer (min_value,
              max_value, step, correct_value).
            - Give every section a relevant emoji, and options an emoji where a
              picture helps young children. NEVER use an emoji that reveals the
              answer (no numbers on ordering items, no check marks on options).
        """.trimIndent()
    }
}
