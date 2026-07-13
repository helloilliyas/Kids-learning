package com.kidslearning.app.ui.renderers

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import kotlin.math.sin

/**
 * Professor Hoot — the app's hand-drawn mascot, rendered as pure vector Canvas
 * (no emoji, no image assets, crisp at any size). He bobs and waves a wing while
 * narrating, blinks now and then, and raises both wings to celebrate.
 */
@Composable
fun OwlMascot(
    modifier: Modifier = Modifier,
    talking: Boolean = false,
    celebrate: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "owl")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
        label = "bob",
    )
    // Mostly-open eyes with a quick blink every ~3.4s.
    val blink by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3400
                1f at 0
                1f at 3000
                0.12f at 3120 using LinearEasing
                1f at 3320
            }
        ),
        label = "blink",
    )

    Canvas(modifier = modifier) {
        val bodyBob = if (talking || celebrate) bob * size.height * 0.04f else 0f
        translate(top = bodyBob) {
            val wingLift = when {
                celebrate -> -55f
                talking -> sin(bob * Math.PI).toFloat() * 18f
                else -> 0f
            }
            drawOwl(wingLift, blink)
        }
    }
}

private fun DrawScope.drawOwl(wingLift: Float, blink: Float) {
    val w = size.width
    val h = size.height
    val body = Color(0xFF5B67CA)        // deep blue-violet
    val bodyDark = Color(0xFF4A54AF)
    val belly = Color(0xFFFDF3DD)       // cream
    val amber = Color(0xFFF2A03D)       // beak + feet
    val ink = Color(0xFF33415C)

    // Ear tufts
    val tuft = Path().apply {
        moveTo(w * 0.22f, h * 0.26f); lineTo(w * 0.16f, h * 0.06f); lineTo(w * 0.38f, h * 0.18f); close()
        moveTo(w * 0.78f, h * 0.26f); lineTo(w * 0.84f, h * 0.06f); lineTo(w * 0.62f, h * 0.18f); close()
    }
    drawPath(tuft, bodyDark)

    // Wings (behind the body, lifted when waving/celebrating)
    rotate(-wingLift, pivot = Offset(w * 0.14f, h * 0.52f)) {
        drawOval(bodyDark, topLeft = Offset(w * 0.00f, h * 0.38f), size = Size(w * 0.24f, h * 0.42f))
    }
    rotate(wingLift, pivot = Offset(w * 0.86f, h * 0.52f)) {
        drawOval(bodyDark, topLeft = Offset(w * 0.76f, h * 0.38f), size = Size(w * 0.24f, h * 0.42f))
    }

    // Body + belly
    drawOval(body, topLeft = Offset(w * 0.10f, h * 0.14f), size = Size(w * 0.80f, h * 0.78f))
    drawOval(belly, topLeft = Offset(w * 0.26f, h * 0.44f), size = Size(w * 0.48f, h * 0.44f))

    // Eyes: white rings + blinking pupils
    drawCircle(Color.White, radius = w * 0.155f, center = Offset(w * 0.35f, h * 0.36f))
    drawCircle(Color.White, radius = w * 0.155f, center = Offset(w * 0.65f, h * 0.36f))
    scale(scaleX = 1f, scaleY = blink, pivot = Offset(w * 0.5f, h * 0.36f)) {
        drawCircle(ink, radius = w * 0.07f, center = Offset(w * 0.37f, h * 0.37f))
        drawCircle(ink, radius = w * 0.07f, center = Offset(w * 0.63f, h * 0.37f))
        drawCircle(Color.White, radius = w * 0.022f, center = Offset(w * 0.39f, h * 0.345f))
        drawCircle(Color.White, radius = w * 0.022f, center = Offset(w * 0.65f, h * 0.345f))
    }

    // Beak
    val beak = Path().apply {
        moveTo(w * 0.46f, h * 0.44f); lineTo(w * 0.54f, h * 0.44f); lineTo(w * 0.50f, h * 0.53f); close()
    }
    drawPath(beak, amber)

    // Graduation cap: board + base + tassel
    val cap = Path().apply {
        moveTo(w * 0.50f, h * 0.02f)
        lineTo(w * 0.88f, h * 0.13f)
        lineTo(w * 0.50f, h * 0.24f)
        lineTo(w * 0.12f, h * 0.13f)
        close()
    }
    drawPath(cap, ink)
    drawLine(ink, Offset(w * 0.84f, h * 0.14f), Offset(w * 0.86f, h * 0.30f), strokeWidth = w * 0.02f)
    drawCircle(amber, radius = w * 0.035f, center = Offset(w * 0.86f, h * 0.32f))

    // Feet
    drawOval(amber, topLeft = Offset(w * 0.28f, h * 0.88f), size = Size(w * 0.16f, h * 0.09f))
    drawOval(amber, topLeft = Offset(w * 0.56f, h * 0.88f), size = Size(w * 0.16f, h * 0.09f))
}
