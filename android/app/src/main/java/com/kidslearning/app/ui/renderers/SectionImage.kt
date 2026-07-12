package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kidslearning.app.ui.theme.Workbook

/**
 * Resolves a section's image_ref (0-based index into the lesson's stored source
 * images) to a bitmap. The player provides a real resolver for lessons generated
 * from photos/PDFs; bundled lessons fall back to the default (no images).
 */
val LocalLessonImageResolver = compositionLocalOf<(Int) -> ImageBitmap?> { { null } }

/**
 * Shows the actual source photo/scan/PDF page a section teaches from. Tapping
 * opens a full-screen pinch-to-zoom viewer -- a whole textbook page needs zoom to
 * read the graph the question is about.
 */
@Composable
fun SectionImage(imageRef: Int?) {
    if (imageRef == null) return
    val bitmap = LocalLessonImageResolver.current(imageRef) ?: return
    var showViewer by remember { mutableStateOf(false) }

    Card(
        onClick = { showViewer = true },
        border = BorderStroke(1.5.dp, Workbook.PinkBorder),
        colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            Image(
                bitmap = bitmap,
                contentDescription = "Picture from your material",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
            )
            Text(
                "🔍 Tap to look closer",
                style = MaterialTheme.typography.labelMedium,
                color = Workbook.TextMuted,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 4.dp),
            )
        }
    }

    if (showViewer) {
        Dialog(
            onDismissRequest = { showViewer = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            var scale by remember { mutableFloatStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformState = rememberTransformableState { zoom, pan, _ ->
                scale = (scale * zoom).coerceIn(1f, 6f)
                offset += pan * scale
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xE6000000))
                    .transformable(transformState),
            ) {
                Image(
                    bitmap = bitmap,
                    contentDescription = "Zoomed picture",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        },
                )
                TextButton(
                    onClick = { showViewer = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
                ) {
                    Text("✕ Close", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
