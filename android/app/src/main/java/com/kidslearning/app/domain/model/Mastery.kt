package com.kidslearning.app.domain.model

/**
 * Per-concept mastery tracking with deterministic, on-device scoring.
 *
 * Implements the adaptive rules from the plan without any AI call: fixed-answer
 * activities update a confidence score locally. AI assessment is reserved for
 * open-ended responses, of which V1 has none.
 *
 * Mastery is a 0.0-1.0 confidence value per concept. The update rules reward
 * unaided correct answers most, reward hinted-correct answers a little, and reduce
 * confidence on mistakes -- so a concept the child keeps getting wrong drops and is
 * surfaced as "needs practice" and prioritised by the next Extend Lesson request.
 */
data class ConceptMastery(
    val conceptId: String,
    val mastery: Double,          // 0.0 - 1.0
    val consecutiveCorrect: Int,
    val consecutiveWrong: Int,
) {
    val needsPractice: Boolean get() = mastery < 0.5
    val isMastered: Boolean get() = mastery >= 0.85
}

object MasteryEngine {
    private const val STRONG_GAIN = 0.20   // correct on first attempt, no hint
    private const val SLIGHT_GAIN = 0.08   // correct but after a hint
    private const val PENALTY = 0.15       // incorrect

    fun initial(conceptId: String) = ConceptMastery(conceptId, mastery = 0.3, 0, 0)

    fun update(current: ConceptMastery, result: AnswerResult): ConceptMastery {
        require(result.conceptId == current.conceptId)

        return if (result.correct) {
            val gain = if (result.usedHint || result.attempts > 1) SLIGHT_GAIN else STRONG_GAIN
            current.copy(
                mastery = (current.mastery + gain).coerceAtMost(1.0),
                consecutiveCorrect = current.consecutiveCorrect + 1,
                consecutiveWrong = 0,
            )
        } else {
            current.copy(
                mastery = (current.mastery - PENALTY).coerceAtLeast(0.0),
                consecutiveCorrect = 0,
                consecutiveWrong = current.consecutiveWrong + 1,
            )
        }
    }

    /**
     * Instructional response the player should take next, mirroring the plan's rules:
     * two mistakes -> simpler explanation; repeated mistakes -> worked example;
     * several correct -> raise difficulty.
     */
    fun recommendation(m: ConceptMastery): AdaptiveAction = when {
        m.consecutiveWrong >= 3 -> AdaptiveAction.SHOW_WORKED_EXAMPLE
        m.consecutiveWrong == 2 -> AdaptiveAction.SHOW_SIMPLER_EXPLANATION
        m.consecutiveCorrect >= 3 -> AdaptiveAction.INCREASE_DIFFICULTY
        else -> AdaptiveAction.CONTINUE
    }
}

enum class AdaptiveAction {
    CONTINUE,
    SHOW_SIMPLER_EXPLANATION,
    SHOW_WORKED_EXAMPLE,
    INCREASE_DIFFICULTY,
}
