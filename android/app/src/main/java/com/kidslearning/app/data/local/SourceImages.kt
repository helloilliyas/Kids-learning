package com.kidslearning.app.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Source-image handling, all on-device, with a split resolution policy:
 *
 * - DISPLAY copies (what is stored with the lesson and what the child sees and
 *   zooms into) are kept sharp: max 2048px, JPEG q85 — a photographed book page
 *   stays readable under pinch-zoom.
 * - VISION copies (what is uploaded to the AI) are derived per-request at
 *   1568px/q70 — plenty for the model to read, and keeps request size and
 *   vision cost sensible. The stored copy is never degraded by the upload path.
 * - THUMBNAILS decode sampled-down so attachment strips never hold full bitmaps.
 *
 * Camera and gallery images are EXIF-rotated upright before storage.
 */
object SourceImages {

    private const val DISPLAY_MAX_DIM = 2048
    private const val DISPLAY_QUALITY = 85
    private const val VISION_MAX_DIM = 1568
    private const val VISION_QUALITY = 70
    const val MAX_IMAGES = 8
    const val MAX_PDF_PAGES = 5

    /** Compress to the stored/display quality. */
    fun compress(source: Bitmap): ByteArray =
        compress(source, DISPLAY_MAX_DIM, DISPLAY_QUALITY)

    private fun compress(source: Bitmap, maxDim: Int, quality: Int): ByteArray {
        val scale = maxDim.toFloat() / maxOf(source.width, source.height)
        val bitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                source,
                (source.width * scale).toInt().coerceAtLeast(1),
                (source.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        } else source
        return ByteArrayOutputStream().use { buffer ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, buffer)
            buffer.toByteArray()
        }
    }

    /** Decode a camera/gallery/document image: EXIF-upright, display quality. */
    fun fromUri(context: Context, uri: Uri): ByteArray? = runCatching {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        // Sampled decode keeps the transient bitmap bounded (~4096px worst case)
        // while staying above the 2048px display target.
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > DISPLAY_MAX_DIM * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null

        val upright = rotateByExif(context, uri, decoded)
        compress(upright)
    }.getOrNull()

    /** Full-resolution camera JPEGs carry orientation in EXIF, not pixels. */
    private fun rotateByExif(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return bitmap
        }
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    /** Render the first pages of a PDF as sharp display-quality images. */
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
                            val scale = DISPLAY_MAX_DIM.toFloat() / maxOf(page.width, page.height)
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

    /** Smaller re-encoded copy for the AI request; never stored. */
    fun toVisionBase64(displayJpeg: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(displayJpeg, 0, displayJpeg.size)
            ?: return Base64.encodeToString(displayJpeg, Base64.NO_WRAP)
        val vision = compress(bitmap, VISION_MAX_DIM, VISION_QUALITY)
        return Base64.encodeToString(vision, Base64.NO_WRAP)
    }

    fun decode(jpeg: ByteArray): Bitmap? =
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size)

    /** Sampled-down decode for attachment strips; never loads the full bitmap. */
    fun decodeThumbnail(jpeg: ByteArray, maxDim: Int = 320): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > maxDim * 2) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size, options)
    }

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
