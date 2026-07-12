package com.kidslearning.app.domain.model

/**
 * Human-readable question and answer text for any section — the single source
 * used by the parent's preview (to judge a lesson) and by the player's answer
 * key (revealed when the child submits with questions left unanswered).
 */
object AnswerKey {

    fun questionText(section: Section): String? = when (section) {
        is ExplanationSection -> null
        is AnimatedStorySection -> null
        is ChartSection -> null
        is BuildBarChartSection -> section.instruction
        is TapImageSection -> section.question
        is SortIntoCategoriesSection -> section.instruction
        is NumberLineSection -> section.question
        is MultipleChoiceSection -> section.question
        is TrueFalseSection -> section.statement
        is FillInTheBlankSection -> section.template.replace(Regex("\\{\\{[^}]+\\}\\}"), "____")
        is MatchPairsSection -> section.instruction
        is DragIntoOrderSection -> section.instruction
    }

    fun answerText(section: Section): String? = when (section) {
        is MultipleChoiceSection -> section.options
            .filter { it.id in section.correctAnswerIds }.joinToString { it.text }
        is TrueFalseSection -> if (section.correctAnswer) "True" else "False"
        is FillInTheBlankSection -> section.blanks.joinToString { it.acceptedAnswers.first() }
        is MatchPairsSection -> section.pairs.joinToString { "${it.left.text} → ${it.right.text}" }
        is DragIntoOrderSection -> {
            val byId = section.items.associateBy { it.id }
            section.correctOrder.mapNotNull { byId[it]?.text }.joinToString(" → ")
        }
        is BuildBarChartSection -> section.items.joinToString { "${it.label}: ${it.target}" }
        is TapImageSection -> section.options.firstOrNull { it.id == section.correctOptionId }
            ?.let { it.label ?: it.emoji ?: it.id }
        is SortIntoCategoriesSection -> {
            val cats = section.categories.associate { it.id to it.label }
            section.items.joinToString { "${it.text} → ${cats[it.categoryId] ?: it.categoryId}" }
        }
        is NumberLineSection -> "${section.correctValue}${section.unit?.let { " $it" } ?: ""}"
        is ChartSection -> null
        is ExplanationSection -> null
        is AnimatedStorySection -> null
    }
}
