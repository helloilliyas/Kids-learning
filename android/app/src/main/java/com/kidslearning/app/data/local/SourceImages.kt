package com.kidslearning.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Source-image handling, all on-device:
 *
 * - camera shots / picked photos / rendered PDF pages are downscaled and
 *   JPEG-compressed before upload, keeping vision costs and request sizes small;
 * - approved lessons keep their source images in app storage so sections with an
 *   image_ref can show the child the actual photo/scan, fully offline.
 */
object SourceImages {

    private const val MAX_DIMENSION = 1280
    private const val JPEG_QUALITY = 75
    const val MAX_IMAGES = 8
    const val MAX_PDF_PAGES = 5

    fun compress(source: Bitmap): ByteArray {
        val scale = MAX_DIMENSION.toFloat() / maxOf(source.width, source.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else source
        return ByteArrayOutputStream().use { buffer ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, buffer)
            buffer.toByteArray()
        }
    }

    /** Decode a picked gallery/document image, downsampled to a sane size. */
    fun fromUri(context: Context, uri: Uri): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_DIMENSION * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
        bitmap?.let(::compress)
    }.getOrNull()

    /** Render the first pages of a PDF as images — scans and text pages alike. */
    fun fromPdf(context: Context, uri: Uri, maxPages: Int = MAX_PDF_PAGES): List<ByteArray> =
        runCatching {
            val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return emptyList()
            descriptor.use { pfd ->
                val renderer = PdfRenderer(pfd)
                try {
                    (0 until minOf(renderer.pageCount, maxPages)).map { index ->
                        val page = renderer.openPage(index)
                        try {
                            val scale = MAX_DIMENSION.toFloat() / maxOf(page.width, page.height)
                            val width = (page.width * scale).toInt().coerceAtLeast(1)
                            val height = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            compress(bitmap)
                        } finally {
                            page.close()
                        }
                    }
                } finally {
                    renderer.close()
                }
            }
        }.getOrDefault(emptyList())

    fun toBase64(jpeg: ByteArray): String = Base64.encodeToString(jpeg, Base64.NO_WRAP)

    fun decode(jpeg: ByteArray): Bitmap? =
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

    // --- Persistent storage for approved lessons -------------------------------

    private fun dir(context: Context, lessonId: String): File =
        File(context.filesDir, "lesson_images/$lessonId")

    fun save(context: Context, lessonId: String, images: List<ByteArray>) {
        val directory = dir(context, lessonId)
        directory.mkdirs()
        images.forEachIndexed { index, bytes ->
            File(directory, "$index.jpg").writeBytes(bytes)
        }
    }

    fun load(context: Context, lessonId: String, index: Int): Bitmap? {
        val file = File(dir(context, lessonId), "$index.jpg")
        return if (file.exists()) BitmapFactory.decodeFile(file.path) else null
    }
}
