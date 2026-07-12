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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.data.local.BundledLessons
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.ui.child.LessonPlayerScreen
import com.kidslearning.app.ui.child.Tts
import com.kidslearning.app.ui.theme.KidsTheme
import com.kidslearning.app.ui.theme.Workbook
import com.kidslearning.app.ui.theme.subjectEmoji
import com.kidslearning.app.ui.theme.subjectGradient

/**
 * Entry point. The home screen follows the same workbook pattern as the player:
 * blue header bar with a coral badge, one white card holding a lean lesson list.
 * Parent mode (create/preview/approve, PIN gate) mounts here next.
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

@Composable
private fun App() {
    val context = LocalContext.current
    val lessons = remember { BundledLessons.load(context) }
    var active by remember { mutableStateOf<Lesson?>(null) }

    val tts = remember { Tts(context, "en") }
    DisposableEffect(Unit) { onDispose { tts.shutdown() } }

    val current = active
    if (current == null) {
        HomeScreen(lessons, onOpen = { active = it })
    } else {
        LessonPlayerScreen(
            lesson = current,
            speak = tts::speak,
            onExit = { active = null },
        )
    }
}

@Composable
private fun HomeScreen(lessons: List<Lesson>, onOpen: (Lesson) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().background(Workbook.PageBackground)) {
        // Same header pattern as the lesson player.
        Surface(
            color = Workbook.Blue,
            shape = RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                Surface(
                    color = Workbook.Coral,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) { Text("📚", fontSize = 18.sp) }
                }
                Column(modifier = Modifier.padding(start = 12.dp)) {
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
                    LessonRow(lesson, onOpen)
                    if (index < lessons.size - 1) {
                        HorizontalDivider(color = Workbook.PageBackground, thickness = 1.5.dp)
                    }
                }
                if (lessons.isEmpty()) {
                    Text(
                        "No lessons found in the bundle.",
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
private fun LessonRow(lesson: Lesson, onOpen: (Lesson) -> Unit) {
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
                Text(
                    "${lesson.subject} · age ${lesson.age} · ${lesson.sections.size} steps · " +
                        "${lesson.estimatedDurationMinutes ?: 10} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = Workbook.TextMuted,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Text("›", fontSize = 24.sp, color = Workbook.TextMuted)
        }
    }
}
