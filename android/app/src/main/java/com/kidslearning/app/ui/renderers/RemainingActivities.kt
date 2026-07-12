package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.DragIntoOrderSection
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.domain.model.FillInTheBlankSection
import com.kidslearning.app.domain.model.MatchPairsSection
import com.kidslearning.app.domain.model.TrueFalseSection

/**
 * Renderer stubs for the remaining V1 activity types.
 *
 * Each has a defined signature and scoring hook so the dispatcher compiles and the
 * lesson-player flow can be wired and demoed against fixture lessons. Filling in
 * the interactive Compose UI (drag gestures, tap-to-fill word banks, connectors) is
 * the next implementation step; the deterministic scoring for each already exists
 * in LocalScoring, so these only need their UI bodies.
 *
 * Explanation cards are read-only and complete as-is.
 */

@Composable
fun ExplanationCard(section: ExplanationSection, readAloud: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(section.title)
        Text(section.content)
        section.imageDescription?.let { Text("[illustration: $it]") }
    }
}

@Composable
fun TrueFalseActivity(
    section: TrueFalseSection,
    readAloud: Boolean,
    onAnswered: (AnswerResult) -> Unit,
) {
    // TODO: two large True/False buttons; score with LocalScoring.scoreTrueFalse.
    Text(section.statement, modifier = Modifier.padding(16.dp))
}

@Composable
fun FillInTheBlankActivity(
    section: FillInTheBlankSection,
    readAloud: Boolean,
    onAnswered: (AnswerResult) -> Unit,
) {
    // TODO: render template with blanks; tap-to-fill word bank when present,
    // otherwise a text field; score with LocalScoring.scoreFillInTheBlank.
    Text(section.template, modifier = Modifier.padding(16.dp))
}

@Composable
fun MatchPairsActivity(
    section: MatchPairsSection,
    readAloud: Boolean,
    onAnswered: (AnswerResult) -> Unit,
) {
    // TODO: two columns, tap-left-then-right to connect; score with scoreMatchPairs.
    Text(section.instruction, modifier = Modifier.padding(16.dp))
}

@Composable
fun DragIntoOrderActivity(
    section: DragIntoOrderSection,
    readAloud: Boolean,
    onAnswered: (AnswerResult) -> Unit,
) {
    // TODO: reorderable list; score with LocalScoring.scoreDragIntoOrder.
    Text(section.instruction, modifier = Modifier.padding(16.dp))
}
