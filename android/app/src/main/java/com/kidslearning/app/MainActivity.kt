package com.kidslearning.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.data.local.AttemptEntity
import com.kidslearning.app.data.local.BundledLessons
import com.kidslearning.app.data.local.ConceptMasteryEntity
import com.kidslearning.app.data.local.DatabaseProvider
import com.kidslearning.app.data.local.LessonEntity
import com.kidslearning.app.data.local.Prefs
import com.kidslearning.app.data.local.SourceImages
import com.kidslearning.app.data.remote.DirectGenerator
import com.kidslearning.app.data.remote.LessonJson
import com.kidslearning.app.domain.model.AnswerResult
import com.kidslearning.app.domain.model.ConceptMastery
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.domain.model.MasteryEngine
import com.kidslearning.app.ui.child.LessonPlayerScreen
import com.kidslearning.app.ui.child.PracticeRequest
import com.kidslearning.app.ui.child.Tts
import com.kidslearning.app.ui.parent.ParentGate
import com.kidslearning.app.ui.parent.ParentScreen
import com.kidslearning.app.ui.parent.PreviewScreen
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import com.kidslearning.app.ui.renderers.LocalLessonImageResolver
import com.kidslearning.app.ui.theme.KidsTheme
import com.kidslearning.app.ui.theme.Workbook
import com.kidslearning.app.ui.theme.subjectEmoji
import com.kidslearning.app.ui.theme.subjectGradient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Entry point and navigation. Child mode is the default; parent mode sits behind
 * an adult gate and is where lessons are created (via the backend), reviewed, and
 * approved. Approved lessons join the bundled ones in the child's list. Progress
 * and per-concept mastery persist to Room on every answer.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            KidsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    App()
                }
            }
        }
    }
}

private sealed interface Screen {
    data object Home : Screen
    data class Playing(val lesson: Lesson) : Screen
    data object Gate : Screen
    data object Parent : Screen
    data class Preview(val lesson: Lesson, val images: List<ByteArray>) : Screen
    data class GeneratingPractice(val request: PracticeRequest) : Screen
}

@Composable
private fun App() {
    val context = LocalContext.current
    val db = remember { DatabaseProvider.get(context) }
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()

    val bundled = remember { BundledLessons.load(context) }
    val stored by db.lessonDao().observeLessons().collectAsState(initial = emptyList())
    val generated = remember(stored) {
        stored.filter { it.approved }.mapNotNull { entity ->
            runCatching { LessonJson.decode(entity.lessonJson) }.getOrNull()
        }
    }
    val lessons = remember(generated) {
        (bundled + generated).distinctBy { it.lessonId }
    }
    val solvedCounts by db.progressDao().observeSolvedCounts()
        .collectAsState(initial = emptyList())
    val solvedByLesson = remember(solvedCounts) {
        solvedCounts.associate { it.lessonId to it.solved }
    }

    var screen by remember { mutableStateOf<Screen>(Screen.Home) }

    val tts = remember { Tts(context, "en", prefs.voiceName, prefs.speechRate) }
    val sounds = remember { Sounds(context) }
    DisposableEffect(Unit) {
        onDispose {
            tts.shutdown()
            sounds.release()
        }
    }

    fun persistResult(lesson: Lesson, result: AnswerResult) {
        scope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            db.progressDao().insertAttempt(
                AttemptEntity(
                    lessonId = lesson.lessonId,
                    sectionId = result.sectionId,
                    conceptId = result.conceptId,
                    correct = result.correct,
                    attempts = result.attempts,
                    usedHint = result.usedHint,
                    timeSpentMs = 0,
                    answeredAt = now,
                )
            )
            val conceptId = result.conceptId ?: return@launch
            val current = db.progressDao().getMastery(lesson.lessonId, conceptId)
                ?.let { ConceptMastery(it.conceptId, it.mastery, it.consecutiveCorrect, it.consecutiveWrong) }
                ?: MasteryEngine.initial(conceptId)
            val updated = MasteryEngine.update(current, result)
            db.progressDao().upsertMastery(
                ConceptMasteryEntity(
                    lessonId = lesson.lessonId,
                    conceptId = updated.conceptId,
                    mastery = updated.mastery,
                    consecutiveCorrect = updated.consecutiveCorrect,
                    consecutiveWrong = updated.consecutiveWrong,
                    updatedAt = now,
                )
            )
        }
    }

    CompositionLocalProvider(
        LocalSounds provides { effect -> if (prefs.soundEffects) sounds.play(effect) }
    ) {
    when (val current = screen) {
        is Screen.Home -> HomeScreen(
            lessons = lessons,
            solvedByLesson = solvedByLesson,
            onOpen = { screen = Screen.Playing(it) },
            onParent = { screen = Screen.Gate },
        )

        is Screen.Playing -> {
            // LRU-capped: display images are now 2048px (~16MB decoded), so only
            // a handful stay in memory while the child pages through the lesson.
            val imageCache = remember(current.lesson.lessonId) {
                object : LinkedHashMap<Int, androidx.compose.ui.graphics.ImageBitmap?>(8, 0.75f, true) {
                    override fun removeEldestEntry(
                        eldest: MutableMap.MutableEntry<Int, androidx.compose.ui.graphics.ImageBitmap?>,
                    ) = size > 5
                }
            }
            CompositionLocalProvider(
                LocalLessonImageResolver provides { index ->
                    imageCache.getOrPut(index) {
                        SourceImages.load(context, current.lesson.lessonId, index)?.asImageBitmap()
                    }
                }
            ) {
                LessonPlayerScreen(
                    lesson = current.lesson,
                    speak = tts::speak,
                    onExit = { screen = Screen.Home },
                    onResult = { persistResult(current.lesson, it) },
                    onPractice = if (prefs.anthropicKey.isNotBlank()) {
                        { request -> screen = Screen.GeneratingPractice(request) }
                    } else null,
                )
            }
        }

        is Screen.GeneratingPractice -> PracticeGeneratingScreen(
            request = current.request,
            apiKey = prefs.anthropicKey,
            persist = { lesson, images ->
                val now = System.currentTimeMillis()
                SourceImages.save(context, lesson.lessonId, images)
                db.lessonDao().upsertLesson(
                    LessonEntity(
                        lessonId = lesson.lessonId,
                        version = 1,
                        title = lesson.title,
                        subject = lesson.subject,
                        age = lesson.age,
                        approved = true,
                        schemaVersion = lesson.schemaVersion,
                        lessonJson = LessonJson.encode(lesson),
                        createdAt = now,
                        updatedAt = now,
                    )
                )
            },
            onPlay = { screen = Screen.Playing(it) },
            onCancel = { screen = Screen.Home },
        )

        is Screen.Gate -> ParentGate(
            onUnlock = { screen = Screen.Parent },
            onCancel = { screen = Screen.Home },
        )

        is Screen.Parent -> ParentScreen(
            prefs = prefs,
            tts = tts,
            onPreview = { lesson, images -> screen = Screen.Preview(lesson, images) },
            onExit = { screen = Screen.Home },
        )

        is Screen.Preview -> PreviewScreen(
            lesson = current.lesson,
            onApprove = {
                scope.launch(Dispatchers.IO) {
                    val now = System.currentTimeMillis()
                    SourceImages.save(context, current.lesson.lessonId, current.images)
                    db.lessonDao().upsertLesson(
                        LessonEntity(
                            lessonId = current.lesson.lessonId,
                            version = 1,
                            title = current.lesson.title,
                            subject = current.lesson.subject,
                            age = current.lesson.age,
                            approved = true,
                            schemaVersion = current.lesson.schemaVersion,
                            lessonJson = LessonJson.encode(current.lesson),
                            createdAt = now,
                            updatedAt = now,
                        )
                    )
                }
                screen = Screen.Home
            },
            onDiscard = { screen = Screen.Parent },
        )
    }
    }
}

/**
 * Full-screen wait while the practice set is generated on-device, then saves it
 * (auto-approved: it is derived from an already-approved lesson) and plays it.
 */
@Composable
private fun PracticeGeneratingScreen(
    request: PracticeRequest,
    apiKey: String,
    persist: suspend (Lesson, List<ByteArray>) -> Unit,
    onPlay: (Lesson) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(request) {
        val result = runCatching {
            DirectGenerator(apiKey, context).generatePractice(
                base = request.baseLesson,
                weakConceptIds = request.weakConceptIds,
                difficulty = request.difficulty,
                numQuestions = request.numQuestions,
            )
        }.getOrElse {
            DirectGenerator.DirectResult(null, listOf(it.message ?: "Generation failed."))
        }
        val lesson = result.lesson
        if (lesson != null && result.errors.isEmpty()) {
            withContext(Dispatchers.IO) { persist(lesson, result.images) }
            onPlay(lesson)
        } else {
            error = result.errors.firstOrNull() ?: "Could not make practice questions."
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Workbook.PageBackground).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (error == null) {
            Text("✏️", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
            Spacer(Modifier.height(16.dp))
            Text(
                "Making ${request.numQuestions} new practice questions…",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                "About: ${request.baseLesson.title}",
                style = MaterialTheme.typography.bodyMedium,
                color = Workbook.TextMuted,
                modifier = Modifier.padding(top = 6.dp),
            )
        } else {
            Text("😕", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "Could not make the practice questions:\n${error?.take(200)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onCancel) {
            Text(if (error == null) "Cancel" else "← Back to lessons")
        }
    }
}

@Composable
private fun HomeScreen(
    lessons: List<Lesson>,
    solvedByLesson: Map<String, Int>,
    onOpen: (Lesson) -> Unit,
    onParent: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        Surface(
            color = Workbook.Blue,
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Surface(
                    color = Workbook.Coral,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(34.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("📚", fontSize = 17.sp) }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(
                        "Kids Learning",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                    )
                    Text(
                        "${lessons.size} lessons ready",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                    )
                }
                TextButton(onClick = onParent) {
                    Text("Parent", color = Color.White)
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
            shape = MaterialTheme.shapes.large,
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    "Pick a lesson to play",
                    style = MaterialTheme.typography.labelLarge,
                    color = Workbook.TextMuted,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                lessons.forEachIndexed { index, lesson ->
                    LessonRow(lesson, solvedByLesson[lesson.lessonId], onOpen)
                    if (index < lessons.size - 1) {
                        HorizontalDivider(color = Workbook.PageBackground, thickness = 1.5.dp)
                    }
                }
                if (lessons.isEmpty()) {
                    Text(
                        "No lessons yet. A grown-up can create one in Parent mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun LessonRow(lesson: Lesson, solved: Int?, onOpen: (Lesson) -> Unit) {
    Surface(
        onClick = { onOpen(lesson) },
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 12.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        Brush.linearGradient(subjectGradient(lesson.subject)),
                        MaterialTheme.shapes.small,
                    ),
            ) {
                Text(subjectEmoji(lesson.subject), fontSize = 20.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(lesson.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                val progress = solved?.let { " · ✓ $it solved" }.orEmpty()
                Text(
                    "${lesson.subject} · age ${lesson.age} · ${lesson.sections.size} steps · " +
                        "${lesson.estimatedDurationMinutes ?: 10} min" + progress,
                    style = MaterialTheme.typography.labelMedium,
                    color = Workbook.TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text("›", fontSize = 24.sp, color = Workbook.TextMuted)
        }
    }
}
