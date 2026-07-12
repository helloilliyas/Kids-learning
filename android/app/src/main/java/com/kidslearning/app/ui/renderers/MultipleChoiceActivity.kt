package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.domain.model.MultipleChoiceSection
import androidx.compose.ui.unit.dp

/**
 * Single- and multi-select choice questions. The mode comes from the data
 * (selection_mode), not a separate component. Each option is a colourful
 * OptionCard with its emoji picture.
 */
@Composable
fun MultipleChoiceActivity(
    section: MultipleChoiceSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val multiple = section.selectionMode == "multiple"
    val selected = remember(section.id) { mutableStateListOf<String>() }

    ActivityScaffold(
        activity = section,
        number = number,
        prompt = section.question,
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = selected.isNotEmpty(),
        score = { LocalScoring.scoreMultipleChoice(section, selected.toSet()) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            section.options.forEachIndexed { index, option ->
                val isSelected = option.id in selected
                OptionCard(
                    text = option.text,
                    emoji = option.emoji,
                    index = index,
                    selected = isSelected,
                    onClick = {
                        if (multiple) {
                            if (isSelected) selected.remove(option.id) else selected.add(option.id)
                        } else {
                            selected.clear()
                            selected.add(option.id)
                        }
                    },
                )
            }
            if (multiple) {
                Text(
                    "👆 Choose all the right answers.",
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}
