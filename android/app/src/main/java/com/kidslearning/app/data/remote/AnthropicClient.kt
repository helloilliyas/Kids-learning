package com.kidslearning.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Direct Anthropic Messages API client for personal-use mode: the app talks to
 * the API itself with the parent's own key (entered at runtime, stored in private
 * app storage). A forced tool call guarantees the response is a JSON object
 * matching the given schema, and source images ride as vision blocks -- the same
 * structured-output design as the backend provider, ported to the device.
 */
class AnthropicClient(private val apiKey: String) {

    companion object {
        // Cost routing: cheap model reads/plans, strong model writes the lesson.
        const val MODEL_SMALL = "claude-haiku-4-5-20251001"
        const val MODEL_STRONG = "claude-sonnet-5"
        private const val TOOL_NAME = "emit_structured_output"
        private const val API_URL = "https://api.anthropic.com/v1/messages"
    }

    /** Returns the tool-call input object, guaranteed to match [schema]. */
    suspend fun generateStructured(
        model: String,
        system: String,
        prompt: String,
        schema: JsonObject,
        images: List<String> = emptyList(),
        maxTokens: Int = 8192,
    ): JsonObject = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            put("model", model)
            put("max_tokens", maxTokens)
            put("system", system)
            putJsonArray("tools") {
                addJsonObject {
                    put("name", TOOL_NAME)
                    put("description", "Return the result as structured data matching the schema.")
                    put("input_schema", schema)
                }
            }
            putJsonObject("tool_choice") {
                put("type", "tool")
                put("name", TOOL_NAME)
            }
            putJsonArray("messages") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("content") {
                        images.forEach { image ->
                            addJsonObject {
                                put("type", "image")
                                putJsonObject("source") {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", image)
                                }
                            }
                        }
                        addJsonObject {
                            put("type", "text")
                            put("text", prompt)
                        }
                    }
                }
            }
        }

        val connection = URL(API_URL).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("x-api-key", apiKey)
            connection.setRequestProperty("anthropic-version", "2023-06-01")
            connection.doOutput = true
            connection.connectTimeout = 20_000
            connection.readTimeout = 300_000

            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()

            if (code == 401) throw IOException("The API key was rejected — check it in settings.")
            if (code == 429) throw IOException("Rate limited — wait a moment and try again.")
            if (code !in 200..299) throw IOException("API error HTTP $code: ${text.take(300)}")

            val root = LessonJson.codec.parseToJsonElement(text).jsonObject
            if (root["stop_reason"]?.jsonPrimitive?.content == "max_tokens") {
                throw IOException(
                    "The lesson was too long and got cut off — try fewer questions or fewer pages."
                )
            }
            val content = root["content"]?.jsonArray
                ?: throw IOException("Unexpected API response shape.")
            val toolUse = content.firstOrNull {
                it.jsonObject["type"]?.jsonPrimitive?.content == "tool_use"
            } ?: throw IOException("The model did not return structured output.")
            toolUse.jsonObject["input"]?.jsonObject
                ?: throw IOException("The structured output was empty.")
        } finally {
            connection.disconnect()
        }
    }
}
