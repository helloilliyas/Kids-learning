package com.kidslearning.app.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.kidslearning.app.data.local.SourceImages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Fetches a real lead photo for a term from Wikipedia's REST summary API. Used to
 * show actual planets, animals, and landmarks in generated lessons when nothing
 * was uploaded. No API key, free, and public-domain / freely-licensed imagery.
 *
 * The parent always sees fetched pictures in the preview before approving, so an
 * occasional off-target image is caught by that human gate. Results are returned
 * as compressed JPEG bytes, stored on-device with the lesson for offline replay.
 */
object WikipediaImages {

    private const val SUMMARY_API =
        "https://en.wikipedia.org/api/rest_v1/page/summary/"

    /** Returns compressed JPEG bytes for [term], or null if nothing suitable. */
    suspend fun fetch(term: String): ByteArray? = withContext(Dispatchers.IO) {
        runCatching {
            val title = URLEncoder.encode(term.trim().replace(' ', '_'), "UTF-8")
            val summary = get(SUMMARY_API + title) ?: return@runCatching null
            val root = LessonJson.codec.parseToJsonElement(summary).jsonObject
            // Prefer the higher-resolution originalimage, fall back to thumbnail.
            val imageUrl = (root["originalimage"] ?: root["thumbnail"])
                ?.jsonObject?.get("source")?.jsonPrimitive?.content
                ?: return@runCatching null
            val bytes = getBytes(imageUrl) ?: return@runCatching null
            val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: return@runCatching null
            SourceImages.compress(bitmap)
        }.getOrNull()
    }

    private fun get(url: String): String? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.setRequestProperty("User-Agent", "KidsLearning/1.0 (personal use)")
            connection.setRequestProperty("Accept", "application/json")
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else null
        } finally {
            connection.disconnect()
        }
    }

    private fun getBytes(url: String): ByteArray? {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.setRequestProperty("User-Agent", "KidsLearning/1.0 (personal use)")
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            if (connection.responseCode in 200..299) {
                connection.inputStream.use { it.readBytes() }
            } else null
        } finally {
            connection.disconnect()
        }
    }
}
