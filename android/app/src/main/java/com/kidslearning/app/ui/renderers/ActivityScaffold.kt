package com.kidslearning.app.ui.renderers

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.Activity
import com.kidslearning.app.domain.model.AnswerResult

/**
 * State + chrome every interactive activity shares: a big emoji visual, the
 * prompt, an optional hint (revealed on request), a Check button, animated
 * feedback with haptics, and retry-until-correct.
 *
 * Renderers supply only their answer area and a scoring lambda; this scaffold owns
 * attempts/hint bookkeeping and emits the standardized [AnswerResult] exactly once,
 * when the child gets it right. Incorrect answers show the teaching feedback in a
 * warm (not scary) colour and let the child try again.
 */
@Composable
fun ActivityScaffold(
    activity: Activity,
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

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        activity.emoji?.let {
            Text(
                it,
                fontSize = 52.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }

        SpeakableText(
            text = prompt,
            style = MaterialTheme.typography.titleLarge,
            readAloud = readAloud,
            speak = speak,
        )

        answerArea()

        AnimatedVisibility(visible = hintShown, enter = fadeIn() + expandVertically()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("💡", fontSize = 24.sp, modifier = Modifier.padding(end = 10.dp))
                    Text(activity.hint, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        AnimatedVisibility(visible = feedback != null, enter = fadeIn() + expandVertically()) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (feedbackIsPositive) MaterialTheme.colorScheme.tertiaryContainer
                    else MaterialTheme.colorScheme.errorContainer,
                ),
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (feedbackIsPositive) "🌟" else "💪",
                        fontSize = 26.sp,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                    Text(feedback.orEmpty(), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (!solved) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (!hintShown) {
                    OutlinedButton(
                        onClick = { hintShown = true },
                        modifier = Modifier.sizeIn(minHeight = 52.dp),
                    ) { Text("💡 Hint") }
                }
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
                    modifier = Modifier.sizeIn(minHeight = 52.dp).weight(1f),
                ) { Text("Check ✓", style = MaterialTheme.typography.titleMedium) }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
