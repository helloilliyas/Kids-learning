package com.kidslearning.app.ui.renderers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.AnimatedStorySection
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import com.kidslearning.app.ui.theme.Workbook
import kotlinx.coroutines.delay

/**
 * The lesson's opening "little show": scenes auto-advance with slide animations,
 * a big gently-bobbing emoji or a real photo, voice narration through the chosen
 * narrator voice, and soft sound effects between scenes. Plays once
 * automatically; the child can pause, step, or replay.
 */
@Composable
fun AnimatedStoryView(
    section: AnimatedStorySection,
    speak: (String) -> Unit,
) {
    var sceneIndex by rememberSaveable(section.id) { mutableIntStateOf(0) }
    var playing by rememberSaveable(section.id) { mutableStateOf(true) }
    var finished by rememberSaveable(section.id) { mutableStateOf(false) }
    val sounds = LocalSounds.current

    val scenes = section.scenes
    if (scenes.isEmpty()) return
    val scene = scenes[sceneIndex.coerceIn(0, scenes.lastIndex)]

    // Narrate the scene, hold long enough to hear it, then advance.
    LaunchedEffect(section.id, sceneIndex, playing) {
        if (!playing) return@LaunchedEffect
        sounds(Sounds.Effect.WHOOSH)
        speak(scene.text)
        val words = scene.text.split(Regex("\\s+")).count { it.isNotBlank() }
        delay(1800L + words * 420L)
        if (sceneIndex < scenes.lastIndex) {
            sceneIndex += 1
        } else {
            playing = false
            finished = true
            sounds(Sounds.Effect.TADA)
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Workbook.Blue),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("🎬", fontSize = 18.sp)
                Text(
                    section.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                )
            }

            // The stage.
            Card(
                colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                AnimatedContent(
                    targetState = sceneIndex,
                    transitionSpec = {
                        (slideInHorizontally(tween(450)) { it / 2 } + fadeIn(tween(450)))
                            .togetherWith(
                                slideOutHorizontally(tween(300)) { -it / 2 } + fadeOut(tween(300))
                            )
                    },
                    label = "storyScene",
                ) { index ->
                    val shown = scenes[index.coerceIn(0, scenes.lastIndex)]
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth().padding(16.dp).sizeIn(minHeight = 150.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        if (shown.imageRef != null) {
                            SectionImage(shown.imageRef)
                        } else {
                            val bob by rememberInfiniteTransition(label = "bob").animateFloat(
                                initialValue = 0.95f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                                label = "bobScale",
                            )
                            Text(
                                shown.emoji ?: "✨",
                                fontSize = 64.sp,
                                modifier = Modifier.graphicsLayer { scaleX = bob; scaleY = bob },
                            )
                        }
                        Text(
                            shown.text,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            // Scene dots.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                scenes.forEachIndexed { i, _ ->
                    Box(
                        modifier = Modifier
                            .size(if (i == sceneIndex) 10.dp else 7.dp)
                            .background(
                                if (i <= sceneIndex) androidx.compose.ui.graphics.Color.White
                                else Workbook.BlueDark,
                                CircleShape,
                            ),
                    )
                }
            }

            // Controls.
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    enabled = sceneIndex > 0,
                    onClick = { playing = false; sceneIndex -= 1 },
                ) { Text("⏮", fontSize = 18.sp) }
                TextButton(onClick = {
                    if (finished && !playing) {
                        sceneIndex = 0
                        finished = false
                        playing = true
                    } else playing = !playing
                }) {
                    Text(
                        if (playing) "⏸" else if (finished) "🔁" else "▶",
                        fontSize = 20.sp,
                    )
                }
                TextButton(
                    enabled = sceneIndex < scenes.lastIndex,
                    onClick = { playing = false; sceneIndex += 1; speak(scenes[sceneIndex].text) },
                ) { Text("⏭", fontSize = 18.sp) }
            }
        }
    }
}
