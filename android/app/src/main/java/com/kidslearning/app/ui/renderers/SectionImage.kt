package com.kidslearning.app.ui.renderers

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.kidslearning.app.ui.theme.Workbook

/**
 * Resolves a section's image_ref (0-based index into the lesson's stored source
 * images) to a bitmap. The player provides a real resolver for lessons generated
 * from photos/PDFs; bundled lessons fall back to the default (no images).
 */
val LocalLessonImageResolver = compositionLocalOf<(Int) -> ImageBitmap?> { { null } }

/** Shows the actual source photo/scan/PDF page a section teaches from, if any. */
@Composable
fun SectionImage(imageRef: Int?) {
    if (imageRef == null) return
    val bitmap = LocalLessonImageResolver.current(imageRef) ?: return
    Card(
        border = BorderStroke(1.5.dp, Workbook.PinkBorder),
        colors = CardDefaults.cardColors(containerColor = Workbook.CardWhite),
        shape = MaterialTheme.shapes.medium,
    ) {
        Image(
            bitmap = bitmap,
            contentDescription = "Picture from your material",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
        )
    }
}
