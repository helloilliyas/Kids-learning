package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.domain.model.MultipleChoiceSection

/**
 * Multiple choice / multiple selection, deterministically scored on-device.
 *
 * Design targets from the age-adaptation rules: large touch targets (min 56dp
 * rows), one clear action, immediate feedback, and a hint that reveals only on
 * request. The single/multiple distinction comes from the data, not a separate
 * component.
 */
@Composable
fun MultipleChoiceActivity(
    section: MultipleChoiceSection,
    readAloud: Boolean,
    onAnswered: (AnswerResult) -> Unit,
) {
    val multiple = section.selectionMode == "multiple"
    val selected = remember { mutableStateListOf<String>() }
    var attempts by remember { mutableStateOf(0) }
    var usedHint by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(section.question)

        section.options.forEach { option ->
            val isSelected = option.id in selected
            Card(
                colors = CardDefaults.cardColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 56.dp) // large control for small fingers
                    .selectable(selected = isSelected) {
                        if (multiple) {
                            if (isSelected) selected.remove(option.id) else selected.add(option.id)
                        } else {
                            selected.clear()
                            selected.add(option.id)
                        }
                    },
            ) {
                Text(option.text, modifier = Modifier.padding(16.dp))
            }
        }

        Button(onClick = { usedHint = true }) { Text("Hint") }
        if (usedHint) Text(section.hint)

        Button(
            enabled = selected.isNotEmpty(),
            onClick = {
                attempts += 1
                val correct = LocalScoring.scoreMultipleChoice(section, selected.toSet())
                feedback = if (correct) section.correctFeedback else section.incorrectFeedback
                if (correct) {
                    onAnswered(
                        AnswerResult(
                            sectionId = section.id,
                            conceptId = section.conceptId,
                            correct = true,
                            attempts = attempts,
                            usedHint = usedHint,
                        )
                    )
                }
            },
        ) { Text("Check") }

        feedback?.let { Text(it) }
    }
}
