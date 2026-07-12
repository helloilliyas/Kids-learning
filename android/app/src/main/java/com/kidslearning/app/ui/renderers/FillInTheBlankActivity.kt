package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.FillInTheBlankSection
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.ui.theme.OptionPalette

/**
 * Fill in the blank, in two input modes decided by the data:
 *
 * - word_bank present (mandatory for ages <= 6): colourful tappable word chips
 *   fill the next empty blank; tapping a used chip takes the word back.
 * - no word_bank: one text field per blank.
 *
 * The template's {{id}} markers are shown as numbered slots inline in the sentence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillInTheBlankActivity(
    section: FillInTheBlankSection,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val answers = remember(section.id) { mutableStateMapOf<String, String>() }
    val blankIds = section.blanks.map { it.id }

    // Render the template with each marker replaced by the filled word or a slot.
    fun displayText(): String {
        var text = section.template
        section.blanks.forEachIndexed { index, blank ->
            val shown = answers[blank.id]?.takeIf { it.isNotBlank() }?.let { "✨$it✨" }
                ?: "［${index + 1}＿＿］"
            text = text.replace("{{${blank.id}}}", shown)
        }
        return text
    }

    ActivityScaffold(
        activity = section,
        prompt = displayText(),
        readAloud = readAloud,
        speak = speak,
        onAnswered = onAnswered,
        checkEnabled = blankIds.all { !answers[it].isNullOrBlank() },
        score = { LocalScoring.scoreFillInTheBlank(section, answers.toMap()) },
        onIncorrectAttempt = { if (section.wordBank != null) answers.clear() },
    ) {
        val wordBank = section.wordBank
        if (wordBank != null) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                wordBank.forEachIndexed { index, word ->
                    val used = answers.values.contains(word)
                    val (container, accent) = OptionPalette[index % OptionPalette.size]
                    Card(
                        onClick = {
                            if (used) {
                                answers.entries.firstOrNull { it.value == word }
                                    ?.let { answers.remove(it.key) }
                            } else {
                                blankIds.firstOrNull { answers[it].isNullOrBlank() }
                                    ?.let { answers[it] = word }
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (used) MaterialTheme.colorScheme.primaryContainer
                            else container,
                        ),
                        border = BorderStroke(2.dp, accent.copy(alpha = if (used) 0.1f else 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (used) 0.dp else 3.dp),
                        modifier = Modifier.sizeIn(minHeight = 52.dp),
                    ) {
                        Text(
                            word,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                section.blanks.forEachIndexed { index, blank ->
                    OutlinedTextField(
                        value = answers[blank.id].orEmpty(),
                        onValueChange = { answers[blank.id] = it },
                        label = { Text("Blank ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}
