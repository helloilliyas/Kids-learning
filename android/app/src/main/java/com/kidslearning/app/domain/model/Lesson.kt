package com.kidslearning.app.domain.model

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonClassDiscriminator

/**
 * Kotlin mirror of lesson-schema/lesson.schema.json (schema_version 1.0).
 *
 * The backend generates data that conforms to the JSON schema; this file is the
 * Android side of that same contract. The polymorphic [Section] hierarchy is keyed
 * on the "type" discriminator, exactly matching the schema's oneOf, so a lesson
 * deserialised here has the same shape the validator guaranteed on the backend.
 *
 * Keep this file and the JSON schema in lock-step. If they drift, lessons that
 * pass backend validation will fail to render.
 */
@Serializable
data class Lesson(
    @SerialName("schema_version") val schemaVersion: String,
    @SerialName("lesson_id") val lessonId: String,
    val title: String,
    val subject: String,
    val language: String,
    val age: Int,
    val difficulty: String,
    @SerialName("estimated_duration_minutes") val estimatedDurationMinutes: Int? = null,
    @SerialName("learning_objectives") val learningObjectives: List<String>,
    val concepts: List<Concept>,
    @SerialName("source_references") val sourceReferences: List<SourceReference> = emptyList(),
    val sections: List<Section>,
    @SerialName("completion_message") val completionMessage: String,
)

@Serializable
data class Concept(
    @SerialName("concept_id") val conceptId: String,
    val name: String,
)

@Serializable
data class SourceReference(
    @SerialName("source_id") val sourceId: String,
    val title: String? = null,
    val pages: List<Int> = emptyList(),
)

@Serializable
data class Grounding(
    val basis: String, // source_explicit | general_knowledge | interpretation
    @SerialName("source_id") val sourceId: String? = null,
    val pages: List<Int> = emptyList(),
)

@Serializable
data class Option(
    val id: String,
    val text: String,
    val emoji: String? = null,
)

/**
 * Common fields shared by every section, plus the polymorphic discriminator.
 * kotlinx.serialization reads the "type" property to pick the concrete subtype.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("type")
sealed interface Section {
    val id: String
    val conceptId: String?
    val difficultyLevel: Int?
    val emoji: String?
    val imageRef: Int?
    val grounding: Grounding?
}

@Serializable
@SerialName("explanation")
data class ExplanationSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val title: String,
    val content: String,
    @SerialName("image_description") val imageDescription: String? = null,
) : Section

/**
 * Fields every interactive activity carries: the feedback the child sees.
 */
sealed interface Activity : Section {
    val hint: String
    val correctFeedback: String
    val incorrectFeedback: String
}

@Serializable
@SerialName("multiple_choice")
data class MultipleChoiceSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val question: String,
    @SerialName("selection_mode") val selectionMode: String, // single | multiple
    val options: List<Option>,
    @SerialName("correct_answer_ids") val correctAnswerIds: List<String>,
    override val hint: String,
    @SerialName("correct_feedback") override val correctFeedback: String,
    @SerialName("incorrect_feedback") override val incorrectFeedback: String,
) : Activity

@Serializable
@SerialName("true_false")
data class TrueFalseSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val statement: String,
    @SerialName("correct_answer") val correctAnswer: Boolean,
    override val hint: String,
    @SerialName("correct_feedback") override val correctFeedback: String,
    @SerialName("incorrect_feedback") override val incorrectFeedback: String,
) : Activity

@Serializable
@SerialName("fill_in_the_blank")
data class FillInTheBlankSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val template: String,
    val blanks: List<Blank>,
    @SerialName("word_bank") val wordBank: List<String>? = null,
    override val hint: String,
    @SerialName("correct_feedback") override val correctFeedback: String,
    @SerialName("incorrect_feedback") override val incorrectFeedback: String,
) : Activity {
    @Serializable
    data class Blank(
        val id: String,
        @SerialName("accepted_answers") val acceptedAnswers: List<String>,
        @SerialName("case_sensitive") val caseSensitive: Boolean = false,
    )
}

@Serializable
@SerialName("match_pairs")
data class MatchPairsSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val instruction: String,
    val pairs: List<Pair>,
    override val hint: String,
    @SerialName("correct_feedback") override val correctFeedback: String,
    @SerialName("incorrect_feedback") override val incorrectFeedback: String,
) : Activity {
    @Serializable
    data class Pair(val left: Option, val right: Option)
}

@Serializable
@SerialName("chart")
data class ChartSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val title: String,
    @SerialName("chart_type") val chartType: String, // bar | pictograph
    val items: List<ChartItem>,
    val unit: String? = null,
    @SerialName("symbol_value") val symbolValue: Int = 1,
) : Section {
    @Serializable
    data class ChartItem(
        val label: String,
        val value: Int,
        val emoji: String? = null,
    )
}

@Serializable
@SerialName("build_bar_chart")
data class BuildBarChartSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val instruction: String,
    val items: List<BarTarget>,
    @SerialName("max_value") val maxValue: Int,
    val step: Int = 1,
    override val hint: String,
    @SerialName("correct_feedback") override val correctFeedback: String,
    @SerialName("incorrect_feedback") override val incorrectFeedback: String,
) : Activity {
    @Serializable
    data class BarTarget(
        val label: String,
        val target: Int,
        val emoji: String? = null,
    )
}

@Serializable
@SerialName("drag_into_order")
data class DragIntoOrderSection(
    override val id: String,
    @SerialName("concept_id") override val conceptId: String? = null,
    @SerialName("difficulty_level") override val difficultyLevel: Int? = null,
    override val emoji: String? = null,
    @SerialName("image_ref") override val imageRef: Int? = null,
    override val grounding: Grounding? = null,
    val instruction: String,
    val items: List<Option>,
    @SerialName("correct_order") val correctOrder: List<String>,
    override val hint: String,
    @SerialName("correct_feedback") override val correctFeedback: String,
    @SerialName("incorrect_feedback") override val incorrectFeedback: String,
) : Activity
