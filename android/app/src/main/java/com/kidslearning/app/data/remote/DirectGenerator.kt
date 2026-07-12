package com.kidslearning.app.data.remote

import android.content.Context
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.LessonChecks
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

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
        val lessonJson = client.generateStructured(
            model = AnthropicClient.MODEL_STRONG,
            system = LESSON_SYSTEM,
            prompt = lessonPrompt(plan.toString(), topic, subject, age, objective, numQuestions, language),
            schema = lessonSchema(),
            images = images,
            maxTokens = 8192,
        )

        val lesson = runCatching {
            LessonJson.codec.decodeFromJsonElement(Lesson.serializer(), lessonJson)
        }.getOrElse { failure ->
            return DirectResult(null, listOf("The lesson did not match the schema: ${failure.message?.take(200)}"))
        }

        return DirectResult(lesson, LessonChecks.validate(lesson, images.size))
    }

    /** The shared lesson JSON Schema, bundled as an asset — one contract everywhere. */
    private fun lessonSchema(): JsonObject =
        context.assets.open("lesson.schema.json").bufferedReader().use { it.readText() }
            .let { LessonJson.codec.parseToJsonElement(it).jsonObject }

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
            - Uploaded images ARE the source material: read them fully. When a
              section teaches from a specific image, set image_ref to its 0-based
              index. Only reference images actually provided; never invent one.
            - Give every section a relevant emoji, and options an emoji where a
              picture helps young children. NEVER use an emoji that reveals the
              answer (no numbers on ordering items, no check marks on options).
        """.trimIndent()
    }
}
