package com.kidslearning.app.ui.child

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

/**
 * Lightweight falling-confetti overlay for the completion celebration. Pure
 * Compose Canvas -- no dependency, negligible cost, and it makes finishing a
 * lesson feel like an event.
 */
@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    data class Particle(
        val x: Float,        // 0..1 horizontal position
        val phase: Float,    // 0..1 vertical start offset
        val color: Color,
        val radius: Float,
        val sway: Float,
        val isRect: Boolean,
    )

    val colors = listOf(
        Color(0xFF5E60CE), Color(0xFFFF6B9D), Color(0xFF2A9D8F),
        Color(0xFFFFB703), Color(0xFF4CC9F0), Color(0xFFE76F51),
    )
    val particles = remember {
        val rng = Random(42)
        List(90) {
            Particle(
                x = rng.nextFloat(),
                phase = rng.nextFloat(),
                color = colors[rng.nextInt(colors.size)],
                radius = 6f + rng.nextFloat() * 10f,
                sway = 20f + rng.nextFloat() * 40f,
                isRect = rng.nextBoolean(),
            )
        }
    }

    val progress by rememberInfiniteTransition(label = "confetti").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing)),
        label = "confettiFall",
    )

    Canvas(modifier = modifier) {
        particles.forEach { p ->
            val y = ((progress + p.phase) % 1f) * (size.height + 40f) - 20f
            val x = p.x * size.width + sin(y / 90f) * p.sway
            if (p.isRect) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x, y),
                    size = Size(p.radius * 1.6f, p.radius),
                )
            } else {
                drawCircle(p.color, p.radius, Offset(x, y))
            }
        }
    }
}
