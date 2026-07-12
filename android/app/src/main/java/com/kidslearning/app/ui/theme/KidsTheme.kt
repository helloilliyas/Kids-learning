package com.kidslearning.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Kid-friendly design system: a bright, high-contrast palette, big rounded shapes,
 * and generous type. Child mode always uses the light scheme -- young readers do
 * better on light backgrounds, and it keeps the colour system predictable.
 */

private val KidsColors = lightColorScheme(
    primary = Color(0xFF5E60CE),          // friendly indigo
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCDDFF),
    onPrimaryContainer = Color(0xFF23245B),
    secondary = Color(0xFFFF6B9D),        // playful pink
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE1EC),
    onSecondaryContainer = Color(0xFF5C0A2E),
    tertiary = Color(0xFF2A9D8F),         // success teal
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFC9F2E4),
    onTertiaryContainer = Color(0xFF00382E),
    error = Color(0xFFE76F51),
    errorContainer = Color(0xFFFFE3C9),   // warm "let's try again" amber, not scary red
    onErrorContainer = Color(0xFF6A3200),
    background = Color(0xFFFDFBFF),
    surface = Color(0xFFFDFBFF),
    surfaceVariant = Color(0xFFF0EEF8),
    onSurfaceVariant = Color(0xFF47464F),
)

private val KidsShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

private val KidsTypography = Typography().let { base ->
    base.copy(
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, lineHeight = 30.sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
    )
}

@Composable
fun KidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = KidsColors,
        shapes = KidsShapes,
        typography = KidsTypography,
        content = content,
    )
}

/**
 * Rotating pastel palette for answer cards: every option gets its own colour, the
 * "HTML design" feel the flat grey cards lacked. Pairs are (container, accent).
 */
val OptionPalette: List<Pair<Color, Color>> = listOf(
    Color(0xFFE3F2FD) to Color(0xFF1976D2), // blue
    Color(0xFFFFF3E0) to Color(0xFFEF6C00), // orange
    Color(0xFFF3E5F5) to Color(0xFF8E24AA), // purple
    Color(0xFFE8F5E9) to Color(0xFF2E7D32), // green
    Color(0xFFFFEBEE) to Color(0xFFC62828), // red-pink
    Color(0xFFE0F7FA) to Color(0xFF00838F), // cyan
)

/** Gradient header colours per subject, with a stable fallback per lesson. */
fun subjectGradient(subject: String): List<Color> {
    val s = subject.lowercase()
    return when {
        "math" in s || "maths" in s -> listOf(Color(0xFF5E60CE), Color(0xFF9D4EDD))
        "science" in s -> listOf(Color(0xFF2A9D8F), Color(0xFF4CC9F0))
        "english" in s || "read" in s || "language" in s ->
            listOf(Color(0xFFFF6B9D), Color(0xFFFFA07A))
        "history" in s || "geo" in s -> listOf(Color(0xFFE9973E), Color(0xFFDB5375))
        else -> listOf(Color(0xFF5E60CE), Color(0xFF48BFE3))
    }
}

/** Emoji badge per subject for the home screen cards. */
fun subjectEmoji(subject: String): String {
    val s = subject.lowercase()
    return when {
        "math" in s -> "🔢"
        "science" in s -> "🔬"
        "english" in s || "read" in s || "language" in s -> "📖"
        "history" in s -> "🏺"
        "geo" in s -> "🌍"
        "art" in s -> "🎨"
        "music" in s -> "🎵"
        else -> "⭐"
    }
}
