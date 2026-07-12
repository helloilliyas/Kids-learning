package com.kidslearning.app.ui.renderers

import androidx.compose.runtime.Composable
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.DragIntoOrderSection
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.domain.model.FillInTheBlankSection
import com.kidslearning.app.domain.model.MatchPairsSection
import com.kidslearning.app.domain.model.MultipleChoiceSection
import com.kidslearning.app.domain.model.Section
import com.kidslearning.app.domain.model.TrueFalseSection

/**
 * Dispatches a [Section] to its native renderer. This is the heart of the
 * "AI generates data, not UI" design: the renderer library is fixed and reusable,
 * and any valid lesson JSON drives it. Adding a new activity type means adding one
 * branch here and one composable -- never a change to lesson content or the model.
 *
 * Every activity renderer reports completion through [onAnswered] with the
 * standardized [AnswerResult], so the lesson player is agnostic to activity type.
 */
@Composable
fun RenderSection(
    section: Section,
    readAloud: Boolean,
    onAnswered: (AnswerResult) -> Unit,
) {
    when (section) {
        is ExplanationSection -> ExplanationCard(section, readAloud)
        is MultipleChoiceSection -> MultipleChoiceActivity(section, readAloud, onAnswered)
        is TrueFalseSection -> TrueFalseActivity(section, readAloud, onAnswered)
        is FillInTheBlankSection -> FillInTheBlankActivity(section, readAloud, onAnswered)
        is MatchPairsSection -> MatchPairsActivity(section, readAloud, onAnswered)
        is DragIntoOrderSection -> DragIntoOrderActivity(section, readAloud, onAnswered)
    }
}
