package com.kidslearning.app.ui.renderers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.Activity
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.ui.theme.Workbook

/**
 * One numbered exercise block on a worksheet page, in the digital-workbook style
 * of the reference design: a blue circular question-number badge, the prompt, the
 * answer area, and a compact inline Check with hint and feedback pills.
 *
 * Owns attempts/hint bookkeeping and emits the standardized [AnswerResult] exactly
 * once, when the child gets it right. Incorrect answers show teaching feedback in
 * a gentle amber pill and allow retry; success turns the block's badge into a ✓.
 */
@Composable
fun ActivityScaffold(
    activity: Activity,
    number: Int?,
    prompt: String,
    readAloud: Boolean,
    speak: (String) -> Unit,
    onAnswered: (AnswerResult) -> Unit,
    checkEnabled: Boolean,
    onIncorrectAttempt: () -> Unit = {},
    score: () -> Boolean,
    answerArea: @Composable () -> Unit,
) {
    var attempts by rememberSaveable(activity.id) { mutableIntStateOf(0) }
    var hintShown by rememberSaveable(activity.id) { mutableStateOf(false) }
    var solved by rememberSaveable(activity.id) { mutableStateOf(false) }
    var feedback by remember(activity.id) { mutableStateOf<String?>(null) }
    var feedbackIsPositive by remember(activity.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        QuestionBadge(number = number, solved = solved)

        Column(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                activity.emoji?.let {
                    Text(it, fontSize = 24.sp, modifier = Modifier.padding(end = 8.dp))
                }
                SpeakableText(
                    text = prompt,
                    style = MaterialTheme.typography.titleMedium,
                    readAloud = readAloud,
                    speak = speak,
                )
            }

            answerArea()

            AnimatedVisibility(visible = hintShown, enter = fadeIn() + expandVertically()) {
                FeedbackPill(
                    emoji = "💡",
                    text = activity.hint,
                    container = Workbook.Cream,
                    border = Workbook.CreamBorder,
                )
            }

            AnimatedVisibility(visible = feedback != null, enter = fadeIn() + expandVertically()) {
                FeedbackPill(
                    emoji = if (feedbackIsPositive) "✓" else "💪",
                    text = feedback.orEmpty(),
                    container = if (feedbackIsPositive) Workbook.GreenPill else Workbook.AmberPill,
                    border = if (feedbackIsPositive) Workbook.GreenBorder else Workbook.AmberBorder,
                )
            }

            if (!solved) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        enabled = checkEnabled,
                        onClick = {
                            attempts += 1
                            val correct = score()
                            feedbackIsPositive = correct
                            feedback = if (correct) activity.correctFeedback else activity.incorrectFeedback
                            haptic.performHapticFeedback(
                                if (correct) HapticFeedbackType.LongPress
                                else HapticFeedbackType.TextHandleMove,
                            )
                            if (correct) {
                                solved = true
                                onAnswered(
                                    AnswerResult(
                                        sectionId = activity.id,
                                        conceptId = activity.conceptId,
                                        correct = true,
                                        attempts = attempts,
                                        usedHint = hintShown,
                                    )
                                )
                            } else {
                                onIncorrectAttempt()
                            }
                        },
                        modifier = Modifier.sizeIn(minHeight = 46.dp),
                    ) { Text("Check") }
                    if (!hintShown) {
                        TextButton(onClick = { hintShown = true }) { Text("💡 Hint") }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionBadge(number: Int?, solved: Boolean) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .background(
                if (solved) MaterialTheme.colorScheme.tertiary else Workbook.Blue,
                CircleShape,
            ),
    ) {
        Text(
            if (solved) "✓" else (number?.toString() ?: "•"),
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.titleSmall,
        )
    }
}

@Composable
private fun FeedbackPill(
    emoji: String,
    text: String,
    container: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = container),
        border = BorderStroke(1.5.dp, border),
        shape = MaterialTheme.shapes.small,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
