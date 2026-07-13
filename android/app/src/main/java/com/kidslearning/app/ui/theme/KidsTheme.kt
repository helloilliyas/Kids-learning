package com.kidslearning.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kidslearning.app.R

/**
 * Digital-workbook design system, matched to the reference screenshots: a soft
 * blue page background, white rounded worksheet cards, cream teaching panels,
 * pink data panels, a friendly blue for primary actions and question badges, and
 * coral for back/secondary accents. Calm, professional, print-workbook feel.
 */
object Workbook {
    val PageBackground = Color(0xFFD9E6F4)   // soft blue behind everything
    val CardWhite = Color(0xFFFEFEFC)        // worksheet card
    val Cream = Color(0xFFFAF3DE)            // instruction / definition panels
    val CreamBorder = Color(0xFFEADFBE)
    val Pink = Color(0xFFF9E2E7)             // data-set panels
    val PinkBorder = Color(0xFFEFC3CD)
    val Blue = Color(0xFF4C7FE0)             // header, primary buttons, badges
    val BlueDark = Color(0xFF3B66BD)
    val BlueLight = Color(0xFFDCE9FB)        // selected fills, chips
    val Coral = Color(0xFFF08476)            // back button, drag accents
    val GreenPill = Color(0xFFEAF2D9)        // correct-answer highlight
    val GreenBorder = Color(0xFF9DBF6E)
    val AmberPill = Color(0xFFFDF0D5)        // gentle try-again highlight
    val AmberBorder = Color(0xFFE5C27C)
    val TextDark = Color(0xFF33415C)         // main ink
    val TextMuted = Color(0xFF6B7A99)
}

private val WorkbookColors = lightColorScheme(
    primary = Workbook.Blue,
    onPrimary = Color.White,
    primaryContainer = Workbook.BlueLight,
    onPrimaryContainer = Workbook.TextDark,
    secondary = Workbook.Coral,
    onSecondary = Color.White,
    secondaryContainer = Workbook.Pink,
    onSecondaryContainer = Color(0xFF6B2737),
    tertiary = Color(0xFF6E9B3D),
    onTertiary = Color.White,
    tertiaryContainer = Workbook.GreenPill,
    onTertiaryContainer = Color(0xFF33470F),
    error = Color(0xFFD96A5B),
    errorContainer = Workbook.AmberPill,
    onErrorContainer = Color(0xFF6A4A00),
    background = Workbook.PageBackground,
    onBackground = Workbook.TextDark,
    surface = Workbook.CardWhite,
    onSurface = Workbook.TextDark,
    surfaceVariant = Color(0xFFEFF3FA),
    onSurfaceVariant = Workbook.TextMuted,
    outline = Color(0xFFC4D2E6),
)

private val KidsShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * The app's typeface: Nunito (OFL), the rounded-but-professional family used by
 * top children's education apps. Bundled so it renders identically on every
 * device, offline.
 */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

// Deliberately smaller than Material defaults: the workbook look is dense and
// lean, closer to print than to a marketing page. Every style is Nunito.
private val KidsTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
        displayMedium = base.displayMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
        displaySmall = base.displaySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
        headlineLarge = base.headlineLarge.copy(
            fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, lineHeight = 28.sp),
        headlineSmall = base.headlineSmall.copy(
            fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 19.sp, lineHeight = 25.sp),
        titleLarge = base.titleLarge.copy(
            fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, lineHeight = 23.sp),
        titleMedium = base.titleMedium.copy(
            fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 21.sp),
        titleSmall = base.titleSmall.copy(
            fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 21.sp),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, lineHeight = 19.sp),
        bodySmall = base.bodySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
        labelLarge = base.labelLarge.copy(
            fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp),
        labelMedium = base.labelMedium.copy(
            fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 11.sp),
        labelSmall = base.labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    )
}

@Composable
fun KidsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WorkbookColors,
        shapes = KidsShapes,
        typography = KidsTypography,
        content = content,
    )
}

/**
 * Soft tint palette for answer cards -- calm workbook pastels, each option gets a
 * distinct (container, accent) pair.
 */
val OptionPalette: List<Pair<Color, Color>> = listOf(
    Color(0xFFEFF5FC) to Color(0xFF4C7FE0), // blue
    Color(0xFFFDF2E3) to Color(0xFFE8A13C), // amber
    Color(0xFFF6EEF9) to Color(0xFF9B59B6), // violet
    Color(0xFFEDF7EE) to Color(0xFF57A05A), // green
    Color(0xFFFDEEF0) to Color(0xFFD96A76), // rose
    Color(0xFFEAF6F8) to Color(0xFF3D96A8), // teal
)

/** Gradient header colours per subject. */
fun subjectGradient(subject: String): List<Color> {
    val s = subject.lowercase()
    return when {
        "math" in s || "maths" in s -> listOf(Color(0xFF4C7FE0), Color(0xFF7B5FD9))
        "science" in s -> listOf(Color(0xFF2A9D8F), Color(0xFF4CA8E0))
        "english" in s || "read" in s || "language" in s ->
            listOf(Color(0xFFE07A9B), Color(0xFFEC9A6D))
        "history" in s || "geo" in s -> listOf(Color(0xFFE0913E), Color(0xFFCB5B75))
        else -> listOf(Color(0xFF4C7FE0), Color(0xFF48A8D8))
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
