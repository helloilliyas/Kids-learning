package com.kidslearning.app.domain.model

/**
 * The standardized result every activity renderer returns.
 *
 * The plan's central idea is that activity components receive structured data and
 * return a standardized answer result. This is that result. The lesson player does
 * not need to know how a given activity was scored -- it just records this.
 *
 * Scoring for fixed-answer activities is deterministic and happens on-device (free,
 * instant, offline). Only open-ended answer types would ever need AI evaluation,
 * and none of the six V1 activity types do.
 */
data class AnswerResult(
    val sectionId: String,
    val conceptId: String?,
    val correct: Boolean,
    val attempts: Int,
    val usedHint: Boolean,
)

/**
 * Deterministic scorers. Each takes the activity plus the child's raw response and
 * decides correctness locally. Kept as pure functions so they are trivially unit
 * testable and identical in behaviour to the backend's validation assumptions.
 */
object LocalScoring {

    fun scoreMultipleChoice(
        section: MultipleChoiceSection,
        selectedIds: Set<String>,
    ): Boolean = selectedIds == section.correctAnswerIds.toSet()

    fun scoreTrueFalse(section: TrueFalseSection, answer: Boolean): Boolean =
        answer == section.correctAnswer

    fun scoreFillInTheBlank(
        section: FillInTheBlankSection,
        answers: Map<String, String>,
    ): Boolean = section.blanks.all { blank ->
        val given = answers[blank.id]?.trim().orEmpty()
        blank.acceptedAnswers.any { accepted ->
            if (blank.caseSensitive) accepted == given
            else accepted.equals(given, ignoreCase = true)
        }
    }

    fun scoreMatchPairs(
        section: MatchPairsSection,
        matches: Map<String, String>, // leftId -> rightId chosen by the child
    ): Boolean = section.pairs.all { pair ->
        matches[pair.left.id] == pair.right.id
    }

    fun scoreDragIntoOrder(
        section: DragIntoOrderSection,
        orderedItemIds: List<String>,
    ): Boolean = orderedItemIds == section.correctOrder
}
