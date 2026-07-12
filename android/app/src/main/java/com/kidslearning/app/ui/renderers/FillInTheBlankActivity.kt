package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.FillInTheBlankSection
import com.kidslearning.app.domain.model.LocalScoring
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import com.kidslearning.app.ui.theme.OptionPalette
import com.kidslearning.app.ui.theme.Workbook

/**
 * Fill in the blank, in two input modes decided by the data:
 *
 * - word_bank present (mandatory for ages <= 6): the child DRAGS a word chip onto
 *   a numbered slot (or taps a chip to fill the next empty slot). Tapping a
 *   filled slot empties it; tapping a used chip takes the word back.
 * - no word_bank: one text field per blank.
 *
 * The template's {{id}} markers are shown as numbered slots inline in the sentence.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FillInTheBlankActivity(
    section: FillInTheBlankSection,
    number: Int?,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
) {
    val answers = remember(section.id) { mutableStateMapOf<String, String>() }
    val blankIds = section.blanks.map { it.id }
    val dragState = rememberDragDropState(section.id)
    val sounds = LocalSounds.current

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
        number = number,
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
            // The numbered drop slots, one per blank.
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 10.dp),
            ) {
                section.blanks.forEachIndexed { index, blank ->
                    val filled = answers[blank.id]?.takeIf { it.isNotBlank() }
                    val hover = dragState.draggingId != null && filled == null
                    Card(
                        onClick = { answers.remove(blank.id) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (filled != null) Workbook.GreenPill else Workbook.Cream,
                        ),
                        border = BorderStroke(
                            2.dp,
                            if (hover) Workbook.Blue
                            else if (filled != null) Workbook.GreenBorder else Workbook.CreamBorder,
                        ),
                        modifier = Modifier
                            .sizeIn(minHeight = 44.dp, minWidth = 72.dp)
                            .dropTarget(dragState, blank.id),
                    ) {
                        Text(
                            filled ?: "${index + 1} ＿＿",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
            Text(
                "🖐 Drag a word onto its slot (or just tap the word)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(bottom = 6.dp),
            )
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
                                    ?.let {
                                        answers[it] = word
                                        sounds(Sounds.Effect.POP)
                                    }
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = if (used) MaterialTheme.colorScheme.primaryContainer
                            else container,
                        ),
                        border = BorderStroke(2.dp, accent.copy(alpha = if (used) 0.1f else 0.4f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = if (used) 0.dp else 3.dp),
                        modifier = Modifier
                            .sizeIn(minHeight = 42.dp)
                            .dragSource(
                                state = dragState,
                                id = "word_$index",
                                onPickUp = { sounds(Sounds.Effect.POP) },
                                onDrop = { target ->
                                    if (target != null && target in blankIds && !used) {
                                        answers[target] = word
                                        sounds(Sounds.Effect.POP)
                                    }
                                },
                            ),
                    ) {
                        Text(
                            word,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
