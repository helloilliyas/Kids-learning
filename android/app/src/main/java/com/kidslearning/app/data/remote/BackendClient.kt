package com.kidslearning.app.data.remote

import com.kidslearning.app.domain.model.Lesson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal HTTP client for the FastAPI backend -- plain HttpURLConnection, no extra
 * dependencies. The backend does the AI work and holds all secrets; the app only
 * sends lesson parameters and receives validated lesson JSON. The shared app token
 * (never an AI key) rides in the Authorization header when configured.
 */
class BackendClient(baseUrl: String, private val token: String?) {

    private val base = baseUrl.trimEnd('/')

    @Serializable
    data class GenerateRequest(
        @SerialName("source_text") val sourceText: String = "",
        val topic: String,
        val age: Int,
        val subject: String,
        val objective: String = "",
        val difficulty: String = "beginner",
        val language: String = "en",
        @SerialName("num_questions") val numQuestions: Int = 6,
    )

    @Serializable
    data class GenerateResponse(
        val ok: Boolean,
        val lesson: Lesson? = null,
        @SerialName("structural_errors") val structuralErrors: List<String> = emptyList(),
        @SerialName("semantic_errors") val semanticErrors: List<String> = emptyList(),
    )

    suspend fun generateLesson(request: GenerateRequest): GenerateResponse =
        withContext(Dispatchers.IO) {
            val connection = URL("$base/api/lessons/generate").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                token?.takeIf { it.isNotBlank() }?.let {
                    connection.setRequestProperty("Authorization", "Bearer $it")
                }
                connection.doOutput = true
                connection.connectTimeout = 20_000
                connection.readTimeout = 180_000 // generation can take a while

                val body = LessonJson.codec.encodeToString(GenerateRequest.serializer(), request)
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

                val code = connection.responseCode
                val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader()?.use { it.readText() }.orEmpty()
                if (code !in 200..299) {
                    throw IOException("Backend returned HTTP $code: ${text.take(300)}")
                }
                LessonJson.codec.decodeFromString(GenerateResponse.serializer(), text)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun health(): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL("$base/api/health").openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 8_000
                connection.readTimeout = 8_000
                connection.responseCode == 200
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(false)
    }
}
