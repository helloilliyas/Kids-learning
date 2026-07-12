package com.kidslearning.app.ui.parent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.Activity
import com.kidslearning.app.domain.model.BuildBarChartSection
import com.kidslearning.app.domain.model.ChartSection
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.domain.model.FillInTheBlankSection
import com.kidslearning.app.domain.model.DragIntoOrderSection
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.MatchPairsSection
import com.kidslearning.app.domain.model.MultipleChoiceSection
import com.kidslearning.app.domain.model.Section
import com.kidslearning.app.domain.model.TrueFalseSection
import com.kidslearning.app.ui.theme.Workbook

/**
 * Parent review before a lesson can reach the child -- the human quality gate from
 * the plan. Shows every section with its content and correct answers so the parent
 * can judge it, then Approve (saves it for the child) or Discard.
 */
@Composable
fun PreviewScreen(
    lesson: Lesson,
    onApprove: () -> Unit,
    onDiscard: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Workbook.Cream),
                border = BorderStroke(1.5.dp, Workbook.CreamBorder),
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Review before your child sees it", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "${lesson.title} · ${lesson.subject} · age ${lesson.age} · " +
                            "${lesson.sections.size} steps",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "Objectives: " + lesson.learningObjectives.joinToString("; "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Workbook.TextMuted,
                    )
                }
            }

            lesson.sections.forEachIndexed { index, section ->
                Card(colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite)) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "${index + 1}. ${sectionKind(section)}",
                            style = MaterialTheme.typography.labelLarge,
                            color = Workbook.TextMuted,
                        )
                        Text(sectionText(section), style = MaterialTheme.typography.bodyLarge)
                        answerSummary(section)?.let {
                            Text(
                                "Answer: $it",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                        if (section is Activity) {
                            Text(
                                "Hint: ${section.hint}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Workbook.TextMuted,
                            )
                        }
                    }
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Button(
                onClick = onDiscard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Workbook.Coral, contentColor = Color.White),
                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
            ) { Text("Discard") }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f).sizeIn(minHeight = 48.dp),
            ) { Text("Approve ✓") }
        }
    }
}

private fun sectionKind(section: Section): String = when (section) {
    is ExplanationSection -> "Explanation"
    is ChartSection -> if (section.chartType == "pictograph") "Pictograph" else "Bar chart"
    is BuildBarChartSection -> "Build a bar chart"
    is MultipleChoiceSection ->
        if (section.selectionMode == "multiple") "Multiple selection" else "Multiple choice"
    is TrueFalseSection -> "True or false"
    is FillInTheBlankSection -> "Fill in the blank"
    is MatchPairsSection -> "Match pairs"
    is DragIntoOrderSection -> "Put in order"
}

private fun sectionText(section: Section): String = when (section) {
    is ExplanationSection -> "${section.title} — ${section.content}"
    is ChartSection -> section.title + ": " +
        section.items.joinToString { "${it.label} ${it.value}" }
    is BuildBarChartSection -> section.instruction
    is MultipleChoiceSection -> section.question
    is TrueFalseSection -> section.statement
    is FillInTheBlankSection -> section.template.replace(Regex("\\{\\{[^}]+\\}\\}"), "____")
    is MatchPairsSection -> section.instruction
    is DragIntoOrderSection -> section.instruction
}

private fun answerSummary(section: Section): String? = when (section) {
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
    else -> null
}
