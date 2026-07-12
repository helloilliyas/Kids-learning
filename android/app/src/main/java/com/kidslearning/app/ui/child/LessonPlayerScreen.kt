package com.kidslearning.app.ui.child

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.Activity
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.ConceptMastery
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.MasteryEngine
import com.kidslearning.app.ui.renderers.RenderSection
import com.kidslearning.app.ui.theme.subjectGradient

/**
 * Child-mode lesson player: a colourful gradient header with animated progress,
 * one section at a time, read-aloud, hints and animated feedback (owned by the
 * activity scaffold), and a confetti celebration with per-concept results.
 */
@Composable
fun LessonPlayerScreen(
    lesson: Lesson,
    speak: (String) -> Unit,
    onExit: () -> Unit,
) {
    var index by remember(lesson.lessonId) { mutableIntStateOf(0) }
    var answeredCurrent by remember(lesson.lessonId, index) { mutableStateOf(false) }
    val results = remember(lesson.lessonId) { mutableStateMapOf<String, AnswerResult>() }
    val mastery = remember(lesson.lessonId) { mutableStateMapOf<String, ConceptMastery>() }
    // V1: speaker buttons always shown. Auto-play for under-8s is a settings item.
    val readAloud = true

    val finished = index >= lesson.sections.size
    if (finished) {
        CompletionScreen(lesson, results.values.toList(), mastery, onExit)
        return
    }

    val section = lesson.sections[index]
    val isActivity = section is Activity
    val gradient = subjectGradient(lesson.subject)
    val progress by animateFloatAsState(
        targetValue = index.toFloat() / lesson.sections.size,
        animationSpec = tween(500),
        label = "lessonProgress",
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Gradient header with title, step counter, and animated progress bar.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(gradient),
                    shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                ),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(
                    lesson.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                Text(
                    "Step ${index + 1} of ${lesson.sections.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.height(8.dp))
            RenderSection(
                section = section,
                readAloud = readAloud,
                speak = speak,
                onAnswered = { result ->
                    results[result.sectionId] = result
                    result.conceptId?.let { cid ->
                        mastery[cid] = MasteryEngine.update(
                            mastery[cid] ?: MasteryEngine.initial(cid),
                            result,
                        )
                    }
                    answeredCurrent = true
                },
            )
        }

        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Button(
                onClick = { index += 1 },
                // Explanations advance freely; activities unlock Continue when solved.
                enabled = !isActivity || answeredCurrent,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 58.dp),
            ) {
                Text(
                    if (index == lesson.sections.size - 1) "Finish 🏁" else "Continue →",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            TextButton(onClick = onExit, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Exit lesson")
            }
        }
    }
}

@Composable
private fun CompletionScreen(
    lesson: Lesson,
    results: List<AnswerResult>,
    mastery: Map<String, ConceptMastery>,
    onExit: () -> Unit,
) {
    val firstTry = results.count { it.attempts == 1 && !it.usedHint }
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("🎉", fontSize = 84.sp)
            Text(
                lesson.completionMessage,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "⭐ You solved $firstTry of ${results.size} activities on the first try!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            lesson.concepts.forEach { concept ->
                val m = mastery[concept.conceptId] ?: return@forEach
                val barProgress by animateFloatAsState(
                    targetValue = m.mastery.toFloat(),
                    animationSpec = tween(900),
                    label = "masteryBar",
                )
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            m.isMastered -> MaterialTheme.colorScheme.tertiaryContainer
                            m.needsPractice -> MaterialTheme.colorScheme.errorContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "${if (m.isMastered) "🏆" else if (m.needsPractice) "🌱" else "📈"} ${concept.name}",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        LinearProgressIndicator(
                            progress = { barProgress },
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(10.dp),
                        )
                        Text(
                            when {
                                m.isMastered -> "Mastered!"
                                m.needsPractice -> "Let's practise this more"
                                else -> "Getting there"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 58.dp),
            ) { Text("Done ✨", style = MaterialTheme.typography.titleMedium) }
        }
        ConfettiOverlay(modifier = Modifier.fillMaxSize())
    }
}
