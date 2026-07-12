package com.kidslearning.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.kidslearning.app.ui.theme.subjectEmoji
import com.kidslearning.app.ui.theme.subjectGradient

/**
 * Entry point. V1 demo flow: a colourful lesson list (the bundled schema
 * fixtures) into the child-mode player. Parent mode (create/preview/approve, PIN
 * gate) mounts here next, backed by Room and the backend client.
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
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Kids Learning 🚀", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Pick a lesson to play!",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        lessons.forEach { lesson ->
            LessonCard(lesson, onOpen)
        }
        if (lessons.isEmpty()) {
            Text("No lessons found in the bundle.")
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, onOpen: (Lesson) -> Unit) {
    val gradient = subjectGradient(lesson.subject)
    Card(
        onClick = { onOpen(lesson) },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 110.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(Brush.horizontalGradient(gradient)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
            ) {
                Text(subjectEmoji(lesson.subject), fontSize = 28.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    "${lesson.subject} · age ${lesson.age}",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.titleLarge)
            Text(
                "🧩 ${lesson.sections.size} steps · ⏱ ${lesson.estimatedDurationMinutes ?: 10} min",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
