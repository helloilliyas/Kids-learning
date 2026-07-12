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
 * collects the activities that follow it. Next unlocks when every exercise on the
 * page is solved; tabs allow revisiting any unlocked page.
 */
@Composable
fun LessonPlayerScreen(
    lesson: Lesson,
    speak: (String) -> Unit,
    onExit: () -> Unit,
) {
    val pages = remember(lesson.lessonId) { buildPages(lesson.sections) }
    val activityNumbers = remember(lesson.lessonId) {
        lesson.sections.filterIsInstance<Activity>()
            .mapIndexed { i, a -> a.id to i + 1 }.toMap()
    }
    val totalActivities = activityNumbers.size

    var pageIndex by remember(lesson.lessonId) { mutableIntStateOf(0) }
    var maxUnlocked by remember(lesson.lessonId) { mutableIntStateOf(0) }
    var showCompletion by remember(lesson.lessonId) { mutableStateOf(false) }
    val results = remember(lesson.lessonId) { mutableStateMapOf<String, AnswerResult>() }
    val mastery = remember(lesson.lessonId) { mutableStateMapOf<String, ConceptMastery>() }

    if (showCompletion) {
        CompletionScreen(lesson, results.values.toList(), mastery, onExit)
        return
    }

    val page = pages[pageIndex]
    val pageDone = page.filterIsInstance<Activity>().all { results.containsKey(it.id) }
    val isLastPage = pageIndex == pages.size - 1

    Column(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        HeaderBar(lesson, correct = results.size, total = totalActivities)

        PageTabs(
            pages = pages,
            current = pageIndex,
            maxUnlocked = maxUnlocked,
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
                        onAnswered = { result ->
                            results[result.sectionId] = result
                            result.conceptId?.let { cid ->
                                mastery[cid] = MasteryEngine.update(
                                    mastery[cid] ?: MasteryEngine.initial(cid),
                                    result,
                                )
                            }
                        },
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
                    if (isLastPage) showCompletion = true
                    else {
                        pageIndex += 1
                        if (pageIndex > maxUnlocked) maxUnlocked = pageIndex
                    }
                },
                enabled = pageDone,
                modifier = Modifier.sizeIn(minHeight = 50.dp),
            ) { Text(if (isLastPage) "Finish 🏁" else "Next →") }
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
    maxUnlocked: Int,
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
            val unlocked = index <= maxUnlocked
            val selected = index == current
            Surface(
                onClick = { if (unlocked) onSelect(index) },
                enabled = unlocked,
                color = if (selected) Workbook.CardWhite else Workbook.BlueLight,
                contentColor = if (unlocked) Workbook.TextDark else Workbook.TextMuted,
                border = if (selected) BorderStroke(1.5.dp, Workbook.Blue) else null,
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    (if (unlocked) "" else "🔒 ") + pageLabel(page, index),
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
    mastery: Map<String, ConceptMastery>,
    onExit: () -> Unit,
) {
    val firstTry = results.count { it.attempts == 1 && !it.usedHint }
    Box(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
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
                colors = CardDefaults.cardColors(containerColor = Workbook.GreenPill),
                border = BorderStroke(1.5.dp, Workbook.GreenBorder),
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

            Button(
                onClick = onExit,
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 56.dp),
            ) { Text("Done ✨", style = MaterialTheme.typography.titleMedium) }
        }
        ConfettiOverlay(modifier = Modifier.fillMaxSize())
    }
}
