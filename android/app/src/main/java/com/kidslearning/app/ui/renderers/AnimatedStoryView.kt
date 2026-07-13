package com.kidslearning.app.ui.renderers

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.domain.model.AnimatedStorySection
import com.kidslearning.app.ui.LocalSounds
import com.kidslearning.app.ui.Sounds
import kotlinx.coroutines.delay

private val StageTop = Color(0xFF3D55C4)
private val StageBottom = Color(0xFF7B5FD9)
private val HighlightAmber = Color(0xFFFFD166)

/** Strip the *highlight* markers for narration and plain contexts. */
private fun plain(text: String) = text.replace("*", "")

/**
 * The lesson's opening show, staged like a polished explainer page: a gradient
 * stage with drifting decorations, Professor Hoot narrating, a punchy headline
 * per scene, key vocabulary highlighted as it is spoken, real photos when the
 * scene has one, and tappable "Did you know?" chips that flip out bonus facts.
 * Auto-plays once; then the child explores.
 */
@Composable
fun AnimatedStoryView(
    section: AnimatedStorySection,
    speak: (String) -> Unit,
) {
    var sceneIndex by rememberSaveable(section.id) { mutableIntStateOf(0) }
    var playing by rememberSaveable(section.id) { mutableStateOf(true) }
    var finished by rememberSaveable(section.id) { mutableStateOf(false) }
    var detailShown by rememberSaveable(section.id) { mutableStateOf(false) }
    val sounds = LocalSounds.current

    val scenes = section.scenes
    if (scenes.isEmpty()) return
    val scene = scenes[sceneIndex.coerceIn(0, scenes.lastIndex)]

    LaunchedEffect(section.id, sceneIndex, playing) {
        if (!playing) return@LaunchedEffect
        sounds(Sounds.Effect.WHOOSH)
        speak(plain(scene.text))
        val words = plain(scene.text).split(Regex("\\s+")).count { it.isNotBlank() }
        delay(1800L + words * 420L)
        if (sceneIndex < scenes.lastIndex) {
            detailShown = false
            sceneIndex += 1
        } else {
            playing = false
            finished = true
            sounds(Sounds.Effect.TADA)
        }
    }

    Card(
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(StageTop, StageBottom))),
        ) {
            FloatingDecorations(scenes.mapNotNull { it.emoji })

            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Title row.
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${sceneIndex + 1}/${scenes.size}",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }

                // Progress bar.
                val progress by animateFloatAsState(
                    (sceneIndex + 1f) / scenes.size, tween(400), label = "storyProgress")
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .height(5.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress)
                            .height(5.dp)
                            .background(HighlightAmber, CircleShape),
                    )
                }

                // The scene.
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
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp).sizeIn(minHeight = 170.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        shown.label?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 8.dp),
                            )
                        }

                        if (shown.imageRef != null) {
                            SectionImage(shown.imageRef)
                        } else {
                            val pop by rememberInfiniteTransition(label = "pop").animateFloat(
                                initialValue = 0.94f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
                                label = "popScale",
                            )
                            Text(
                                shown.emoji ?: "✨",
                                fontSize = 62.sp,
                                modifier = Modifier.graphicsLayer { scaleX = pop; scaleY = pop },
                            )
                        }

                        Text(
                            highlighted(shown.text),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 23.sp),
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 12.dp),
                        )

                        // Explore: the tappable bonus fact.
                        shown.detail?.let { detail ->
                            if (!detailShown) {
                                Surface(
                                    onClick = {
                                        playing = false
                                        detailShown = true
                                        sounds(Sounds.Effect.CHIME)
                                        speak(detail)
                                    },
                                    color = HighlightAmber,
                                    shape = CircleShape,
                                    modifier = Modifier.padding(top = 12.dp),
                                ) {
                                    Text(
                                        "💡 Did you know? Tap!",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = Color(0xFF4A3403),
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                                    )
                                }
                            }
                            AnimatedVisibility(
                                visible = detailShown,
                                enter = fadeIn() + expandVertically() + scaleIn(
                                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)),
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = MaterialTheme.shapes.medium,
                                    modifier = Modifier.padding(top = 12.dp),
                                ) {
                                    Text(
                                        "💡 $detail",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // Controls + the professor.
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) {
                    OwlMascot(
                        talking = playing,
                        celebrate = finished && !playing,
                        modifier = Modifier.size(58.dp),
                    )
                    Box(modifier = Modifier.weight(1f))
                    StoryButton("⏮", enabled = sceneIndex > 0) {
                        playing = false
                        detailShown = false
                        sceneIndex -= 1
                    }
                    StoryButton(if (playing) "⏸" else if (finished) "🔁" else "▶") {
                        if (finished && !playing) {
                            sceneIndex = 0
                            finished = false
                            detailShown = false
                            playing = true
                        } else playing = !playing
                    }
                    StoryButton("⏭", enabled = sceneIndex < scenes.lastIndex) {
                        playing = false
                        detailShown = false
                        sceneIndex += 1
                        speak(plain(scenes[sceneIndex].text))
                    }
                }
            }
        }
    }
}

/** Key vocabulary marked as *word* renders in warm amber, extra bold. */
private fun highlighted(text: String) = buildAnnotatedString {
    text.split('*').forEachIndexed { index, part ->
        if (index % 2 == 1) {
            withStyle(SpanStyle(color = HighlightAmber, fontWeight = FontWeight.ExtraBold)) {
                append(part)
            }
        } else append(part)
    }
}

/** Soft drifting scene emojis behind the stage, like a parallax backdrop. */
@Composable
private fun FloatingDecorations(emojis: List<String>) {
    if (emojis.isEmpty()) return
    val drift by rememberInfiniteTransition(label = "drift").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4200), RepeatMode.Reverse),
        label = "driftValue",
    )
    Box(modifier = Modifier.fillMaxSize()) {
        emojis.take(4).forEachIndexed { i, emoji ->
            val x = listOf(0.06f, 0.82f, 0.14f, 0.74f)[i]
            val y = listOf(0.16f, 0.24f, 0.68f, 0.62f)[i]
            Text(
                emoji,
                fontSize = 26.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(
                        x = (x * 300).dp,
                        y = (y * 240 + drift * 10 * (if (i % 2 == 0) 1 else -1)).dp,
                    )
                    .graphicsLayer { alpha = 0.22f },
            )
        }
    }
}

@Composable
private fun StoryButton(glyph: String, enabled: Boolean = true, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        color = Color.White.copy(alpha = if (enabled) 0.22f else 0.08f),
        shape = CircleShape,
        modifier = Modifier.padding(start = 8.dp).size(44.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(glyph, fontSize = 17.sp, color = Color.White)
        }
    }
}
