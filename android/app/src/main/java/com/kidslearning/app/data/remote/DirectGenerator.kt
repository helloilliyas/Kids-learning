package com.kidslearning.app.data.remote

import android.content.Context
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

    data class DirectResult(val lesson: Lesson?, val errors: List<String>)

    suspend fun generate(
        topic: String,
        subject: String,
        age: Int,
        objective: String,
        sourceText: String,
        images: List<String>,
        numQuestions: Int = 6,
        language: String = "en",
    ): DirectResult {
        // Stage 1: read the source (text + images) and produce a teaching plan.
        val plan = client.generateStructured(
            model = AnthropicClient.MODEL_SMALL,
            system = ANALYZE_SYSTEM,
            prompt = analyzePrompt(topic, subject, age, objective, sourceText, language),
            schema = planSchema(),
            images = images,
            maxTokens = 4096,
        )

        // Stage 2: turn the plan into a full lesson conforming to the shared schema.
        val imageIndexNote = if (images.isEmpty()) "" else
            "\n\nThere are ${images.size} attached image(s), numbered 0 to ${images.size - 1} " +
                "in the order attached. Sections about a graph, chart, table, or picture " +
                "visible in one of them MUST set image_ref to that number."
        val rawLesson = client.generateStructured(
            model = AnthropicClient.MODEL_STRONG,
            system = LESSON_SYSTEM,
            prompt = lessonPrompt(plan.toString(), topic, subject, age, objective, numQuestions, language) +
                imageIndexNote,
            schema = lessonSchema(),
            images = images,
            maxTokens = 16384,
        )

        // Recover the recoverable: unwrap, fill known metadata, derive concepts.
        var candidate = normalize(rawLesson, topic, subject, age, objective, language)
        var lesson = tryDecode(candidate)

        if (lesson == null) {
            // One repair pass: show the model its own output and the exact failure.
            val problem = decodeFailure(candidate) ?: "unknown validation problem"
            val repaired = client.generateStructured(
                model = AnthropicClient.MODEL_STRONG,
                system = LESSON_SYSTEM,
                prompt = repairPrompt(rawLesson.toString(), problem),
                schema = lessonSchema(),
                maxTokens = 16384,
            )
            candidate = normalize(repaired, topic, subject, age, objective, language)
            lesson = tryDecode(candidate)
                ?: return DirectResult(
                    null,
                    listOf("The AI produced an invalid lesson twice: ${decodeFailure(candidate)?.take(200)}"),
                )
        }

        return DirectResult(lesson, LessonChecks.validate(lesson, images.size))
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

        Structure the lesson as: a short introduction, then alternating explanation
        cards and activities building from difficulty_level 1 upward, ending with a
        challenge activity and a warm completion_message. Use only these section
        types: explanation, multiple_choice, true_false, fill_in_the_blank,
        match_pairs, drag_into_order. Give every section a unique snake_case id.
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
            - Give every section a relevant emoji, and options an emoji where a
              picture helps young children. NEVER use an emoji that reveals the
              answer (no numbers on ordering items, no check marks on options).
        """.trimIndent()
    }
}
