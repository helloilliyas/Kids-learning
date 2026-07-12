package com.kidslearning.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.kidslearning.app.data.local.BundledLessons
import com.kidslearning.app.domain.model.Lesson
import com.kidslearning.app.ui.child.LessonPlayerScreen
import com.kidslearning.app.ui.child.Tts

/**
 * Entry point. V1 demo flow: a lesson list (the bundled schema fixtures) into the
 * child-mode player. Parent mode (create/preview/approve, PIN gate) mounts here
 * next, backed by Room and the backend client.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
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

    val tts = remember { Tts(context, active?.language ?: "en") }
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Kids Learning", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Pick a lesson to play. These are the bundled sample lessons; " +
                "creating lessons from your own material arrives with parent mode.",
            style = MaterialTheme.typography.bodyMedium,
        )
        lessons.forEach { lesson ->
            Card(
                onClick = { onOpen(lesson) },
                modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 72.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(lesson.title, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${lesson.subject} · age ${lesson.age} · ${lesson.sections.size} steps",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        if (lessons.isEmpty()) {
            Text("No lessons found in the bundle.")
        }
    }
}
