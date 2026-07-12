package com.kidslearning.app.ui.child

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.Activity
import com.kidslearning.app.domain.model.AnswerKey
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.ConceptMastery
import com.kidslearning.app.domain.model.ExplanationSection
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.MasteryEngine
import com.kidslearning.app.domain.model.Section
import com.kidslearning.app.ui.renderers.RenderSection
import com.kidslearning.app.ui.theme.Workbook
import com.kidslearning.app.ui.theme.subjectEmoji

/**
 * Digital-workbook lesson player, matched to the reference design:
 *
 * - Blue header bar with a unit badge, lesson title, and a live "X of N correct"
 *   progress pill.
 * - A row of page tabs (sections of the lesson grouped into worksheet pages).
 * - A white worksheet card holding cream teaching panels and numbered exercises,
 *   scrolled as one page like a printed workbook.
 * - Coral Back / blue Next buttons with the subject-age breadcrumb between them.
 *
 * Pages are derived from the lesson data: every explanation starts a new page and
 * collects the activities that follow it. Like a paper workbook, the child can
 * skip ahead and come back to any question via the tabs; on the last page,
 * Submit closes the lesson and reveals an answer key for anything left
 * unanswered. Finished lessons can spin up a follow-up practice set (count and
 * difficulty chosen on the completion screen) targeting the weakest concepts.
 */

/** What the completion screen asks for when "more practice" is requested. */
data class PracticeRequest(
    val baseLesson: Lesson,
    val weakConceptIds: List<String>,
    val difficulty: String, // "easier" | "same" | "harder"
    val numQuestions: Int,
)

@Composable
fun LessonPlayerScreen(
    lesson: Lesson,
    speak: (String) -> Unit,
    onExit: () -> Unit,
    onResult: (AnswerResult) -> Unit = {},
    onPractice: ((PracticeRequest) -> Unit)? = null,
) {
    val pages = remember(lesson.lessonId) { buildPages(lesson.sections) }
    val activityNumbers = remember(lesson.lessonId) {
        lesson.sections.filterIsInstance<Activity>()
            .mapIndexed { i, a -> a.id to i + 1 }.toMap()
    }
    val totalActivities = activityNumbers.size

    var pageIndex by remember(lesson.lessonId) { mutableIntStateOf(0) }
    var showCompletion by remember(lesson.lessonId) { mutableStateOf(false) }
    val results = remember(lesson.lessonId) { mutableStateMapOf<String, AnswerResult>() }
    val mastery = remember(lesson.lessonId) { mutableStateMapOf<String, ConceptMastery>() }

    val allActivities = remember(lesson.lessonId) {
        lesson.sections.filterIsInstance<Activity>()
    }

    fun recordResult(result: AnswerResult) {
        results[result.sectionId] = result
        result.conceptId?.let { cid ->
            mastery[cid] = MasteryEngine.update(
                mastery[cid] ?: MasteryEngine.initial(cid),
                result,
            )
        }
        onResult(result) // persistence hook
    }

    if (showCompletion) {
        val skipped = allActivities.filter { it.id !in results }
        CompletionScreen(
            lesson = lesson,
            results = results.values.toList(),
            skipped = skipped,
            activityNumbers = activityNumbers,
            mastery = mastery,
            onPractice = onPractice,
            onExit = onExit,
        )
        return
    }

    val page = pages[pageIndex]
    val pageDone = page.filterIsInstance<Activity>().all { results.containsKey(it.id) }
    val isLastPage = pageIndex == pages.size - 1
    val allDone = results.size == totalActivities

    Column(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        HeaderBar(lesson, correct = results.size, total = totalActivities)

        PageTabs(
            pages = pages,
            current = pageIndex,
            doneIds = results.keys,
            onSelect = { pageIndex = it },
        )

        // The worksheet card.
        Card(
            colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                page.forEachIndexed { i, section ->
                    RenderSection(
                        section = section,
                        number = (section as? Activity)?.let { activityNumbers[it.id] },
                        readAloud = true,
                        speak = speak,
                        onAnswered = ::recordResult,
                    )
                    if (i < page.size - 1 && section is Activity) {
                        HorizontalDivider(color = Workbook.PageBackground, thickness = 1.5.dp)
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }

        // Footer: coral Back, breadcrumb, blue Next.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Button(
                onClick = { if (pageIndex > 0) pageIndex -= 1 else onExit() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Workbook.Coral,
                    contentColor = Color.White,
                ),
                modifier = Modifier.sizeIn(minHeight = 50.dp),
            ) { Text(if (pageIndex > 0) "← Back" else "← Exit") }

            Text(
                "${lesson.subject} • Age ${lesson.age}",
                style = MaterialTheme.typography.labelMedium,
                color = Workbook.TextMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = {
                    if (isLastPage) {
                        // Submitting with questions still open counts each of them
                        // as not-yet-known for mastery, and the completion screen
                        // reveals their answers.
                        allActivities.filter { it.id !in results }.forEach { activity ->
                            onResult(
                                AnswerResult(
                                    sectionId = activity.id,
                                    conceptId = activity.conceptId,
                                    correct = false,
                                    attempts = 1,
                                    usedHint = false,
                                )
                            )
                            activity.conceptId?.let { cid ->
                                mastery[cid] = MasteryEngine.update(
                                    mastery[cid] ?: MasteryEngine.initial(cid),
                                    AnswerResult(activity.id, cid, false, 1, false),
                                )
                            }
                        }
                        showCompletion = true
                    } else pageIndex += 1
                },
                modifier = Modifier.sizeIn(minHeight = 50.dp),
            ) {
                Text(
                    when {
                        !isLastPage && pageDone -> "Next →"
                        !isLastPage -> "Skip →"
                        allDone -> "Finish 🏁"
                        else -> "Submit ✅"
                    }
                )
            }
        }
    }
}

/** Every explanation starts a new page; following activities join that page. */
private fun buildPages(sections: List<Section>): List<List<Section>> {
    val pages = mutableListOf<MutableList<Section>>()
    sections.forEach { section ->
        if (section is ExplanationSection || pages.isEmpty()) {
            pages.add(mutableListOf(section))
        } else {
            pages.last().add(section)
        }
    }
    return pages
}

private fun pageLabel(page: List<Section>, index: Int): String {
    val title = (page.firstOrNull() as? ExplanationSection)?.title
        ?.trimEnd('?', '!', '.')
        ?: return "Part ${index + 1}"
    return if (title.length <= 16) title else title.take(15).trimEnd() + "…"
}

@Composable
private fun HeaderBar(lesson: Lesson, correct: Int, total: Int) {
    val progress by animateFloatAsState(
        targetValue = if (total == 0) 0f else correct.toFloat() / total,
        animationSpec = tween(500),
        label = "headerProgress",
    )
    Surface(
        color = Workbook.Blue,
        shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Unit badge, like the orange number square in the reference design.
            Surface(
                color = Workbook.Coral,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.size(34.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(subjectEmoji(lesson.subject), fontSize = 17.sp)
                }
            }
            Text(
                lesson.title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                maxLines = 2,
            )
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$correct of $total correct",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
                LinearProgressIndicator(
                    progress = { progress },
                    color = Color(0xFFB8E986),
                    trackColor = Workbook.BlueDark,
                    modifier = Modifier.width(110.dp).height(8.dp).padding(top = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun PageTabs(
    pages: List<List<Section>>,
    current: Int,
    doneIds: Set<String>,
    onSelect: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        pages.forEachIndexed { index, page ->
            val selected = index == current
            val activities = page.filterIsInstance<Activity>()
            val done = activities.isNotEmpty() && activities.all { it.id in doneIds }
            Surface(
                onClick = { onSelect(index) },
                color = if (selected) Workbook.CardWhite else Workbook.BlueLight,
                contentColor = Workbook.TextDark,
                border = if (selected) BorderStroke(1.5.dp, Workbook.Blue) else null,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    (if (done) "✓ " else "") + pageLabel(page, index),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CompletionScreen(
    lesson: Lesson,
    results: List<AnswerResult>,
    skipped: List<Activity>,
    activityNumbers: Map<String, Int>,
    mastery: Map<String, ConceptMastery>,
    onPractice: ((PracticeRequest) -> Unit)?,
    onExit: () -> Unit,
) {
    val firstTry = results.count { it.attempts == 1 && !it.usedHint }
    val total = results.size + skipped.size
    Box(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(if (skipped.isEmpty()) "🎉" else "📋", fontSize = 84.sp)
            Text(
                lesson.completionMessage,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Card(
                colors = CardDefaults.cardColors(containerColor = Workbook.GreenPill),
                border = BorderStroke(1.5.dp, Workbook.GreenBorder),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "⭐ You solved $firstTry of $total activities on the first try!",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                )
            }

            if (skipped.isNotEmpty()) {
                AnswerKeyCard(skipped, activityNumbers)
            }

            lesson.concepts.forEach { concept ->
                val m = mastery[concept.conceptId] ?: return@forEach
                val barProgress by animateFloatAsState(
                    targetValue = m.mastery.toFloat(),
                    animationSpec = tween(900),
                    label = "masteryBar",
                )
                Card(
                    colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
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

            if (onPractice != null) {
                PracticeSetupCard(lesson, mastery, onPractice)
            }

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
            ) { Text("Done ✨", style = MaterialTheme.typography.titleMedium) }
        }
        if (skipped.isEmpty()) ConfettiOverlay(modifier = Modifier.fillMaxSize())
    }
}

/** Back-of-the-book answer key for questions submitted without an answer. */
@Composable
private fun AnswerKeyCard(skipped: List<Activity>, activityNumbers: Map<String, Int>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Workbook.Cream),
        border = BorderStroke(1.5.dp, Workbook.CreamBorder),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🔑 Answer key", style = MaterialTheme.typography.titleMedium)
            Text(
                "You skipped these — here are the answers to learn from:",
                style = MaterialTheme.typography.bodyMedium,
                color = Workbook.TextMuted,
            )
            skipped.forEach { activity ->
                Column {
                    Text(
                        "${activityNumbers[activity.id] ?: "•"}. " +
                            (AnswerKey.questionText(activity) ?: ""),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        "✔ ${AnswerKey.answerText(activity) ?: "See the lesson"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Workbook.GreenBorder,
                    )
                }
            }
        }
    }
}

/**
 * "More practice" setup: pick how many questions and how hard, then generate a
 * fresh set aimed at the concepts that need work. Only offered when the app has
 * an AI connection (the parent's key is configured).
 */
@Composable
private fun PracticeSetupCard(
    lesson: Lesson,
    mastery: Map<String, ConceptMastery>,
    onPractice: (PracticeRequest) -> Unit,
) {
    var difficulty by remember { mutableStateOf("same") }
    var count by remember { mutableIntStateOf(5) }

    val weakIds = remember(mastery) {
        val weak = mastery.values.filter { it.needsPractice }.map { it.conceptId }
        weak.ifEmpty {
            mastery.values.sortedBy { it.mastery }.take(2).map { it.conceptId }
        }.ifEmpty { lesson.concepts.map { it.conceptId } }
    }
    val weakNames = lesson.concepts
        .filter { it.conceptId in weakIds }.map { it.name }
        .ifEmpty { lesson.concepts.map { it.name } }

    Card(
        colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("✏️ More practice?", style = MaterialTheme.typography.titleMedium)
            Text(
                "New questions about: ${weakNames.joinToString(", ")}",
                style = MaterialTheme.typography.bodyMedium,
                color = Workbook.TextMuted,
            )

            Text("How hard?", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("😌 Easier", difficulty == "easier") { difficulty = "easier" }
                ChoiceChip("🙂 Same", difficulty == "same") { difficulty = "same" }
                ChoiceChip("🔥 Harder", difficulty == "harder") { difficulty = "harder" }
            }

            Text("How many questions?", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3, 5, 8).forEach { n ->
                    ChoiceChip("$n", count == n) { count = n }
                }
            }

            Button(
                onClick = {
                    onPractice(PracticeRequest(lesson, weakIds, difficulty, count))
                },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
            ) { Text("✨ Make my practice questions") }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = if (selected) Workbook.Blue else Workbook.BlueLight,
        contentColor = if (selected) Color.White else Workbook.TextDark,
        border = if (selected) null else BorderStroke(1.dp, Workbook.Blue.copy(alpha = 0.3f)),
        shape = MaterialTheme.shapes.small,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
        )
    }
}
