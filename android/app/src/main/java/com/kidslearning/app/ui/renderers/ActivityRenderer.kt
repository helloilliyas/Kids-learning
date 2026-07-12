package com.kidslearning.app.ui.renderers

import androidx.compose.runtime.Composable
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.BuildBarChartSection
import com.kidslearning.app.domain.model.ChartSection
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
 * and any valid lesson JSON drives it.
 *
 * [number] is the exercise's position in the lesson's continuous numbering (like a
 * printed workbook); explanations have none. Every activity reports completion
 * through [onAnswered] with the standardized [AnswerResult].
 */
@Composable
fun RenderSection(
    section: Section,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    when (section) {
        is ExplanationSection -> ExplanationCard(section, readAloud, speak)
        is ChartSection -> ChartView(section, readAloud, speak)
        is BuildBarChartSection -> BuildBarChartActivity(section, number, readAloud, speak, onAnswered)
        is MultipleChoiceSection -> MultipleChoiceActivity(section, number, readAloud, speak, onAnswered)
        is TrueFalseSection -> TrueFalseActivity(section, number, readAloud, speak, onAnswered)
        is FillInTheBlankSection -> FillInTheBlankActivity(section, number, readAloud, speak, onAnswered)
        is MatchPairsSection -> MatchPairsActivity(section, number, readAloud, speak, onAnswered)
        is DragIntoOrderSection -> DragIntoOrderActivity(section, number, readAloud, speak, onAnswered)
    }
}
