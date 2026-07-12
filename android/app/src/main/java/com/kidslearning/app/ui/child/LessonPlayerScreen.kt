package com.kidslearning.app.ui.child

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.Activity
import com.kidslearning.app.domain.model.ConceptMastery
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.MasteryEngine
import com.kidslearning.app.ui.renderers.RenderSection

/**
 * Child-mode lesson player: one section at a time, a progress bar, read-aloud,
 * hints and feedback (owned by the activity scaffold), and a celebration screen
 * with per-concept results. Mastery updates run through the deterministic
 * [MasteryEngine] as answers arrive; persisting them to Room is wired next.
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
        CompletionScreen(lesson, results.values.toList(), mastery, speak, onExit)
        return
    }

    val section = lesson.sections[index]
    val isActivity = section is Activity

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { index.toFloat() / lesson.sections.size },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
        ) {
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

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Button(
                onClick = { index += 1 },
                // Explanations advance freely; activities unlock Continue when solved.
                enabled = !isActivity || answeredCurrent,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
            ) {
                Text(if (index == lesson.sections.size - 1) "Finish" else "Continue")
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
    speak: (String) -> Unit,
    onExit: () -> Unit,
) {
    val firstTry = results.count { it.attempts == 1 && !it.usedHint }
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("🎉", style = MaterialTheme.typography.displayLarge)
        Text(
            lesson.completionMessage,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            "You solved $firstTry of ${results.size} activities on the first try!",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )

        lesson.concepts.forEach { concept ->
            val m = mastery[concept.conceptId] ?: return@forEach
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
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(concept.name, style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = { m.mastery.toFloat() },
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    )
                    Text(
                        when {
                            m.isMastered -> "Mastered!"
                            m.needsPractice -> "Let's practise this more"
                            else -> "Getting there"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }

        Button(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
        ) { Text("Done") }
    }
}
