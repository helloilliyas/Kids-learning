package com.kidslearning.app.data.remote

import com.kidslearning.app.domain.model.Lesson
import kotlinx.serialization.json.Json

/**
 * Single configured JSON codec for lessons. The polymorphic sealed [Section]
 * hierarchy uses the "type" discriminator, matching the backend schema's oneOf.
 *
 * ignoreUnknownKeys is on so a lesson produced by a newer backend (extra fields)
 * still deserialises rather than crashing -- forward compatibility that pairs with
 * the stored schema_version for safe migration.
 */
object LessonJson {
    val codec: Json = Json {
        ignoreUnknownKeys = true
        classDiscriminator = "type"
        explicitNulls = false
    }

    fun decode(jsonText: String): Lesson = codec.decodeFromString(Lesson.serializer(), jsonText)

    fun encode(lesson: Lesson): String = codec.encodeToString(Lesson.serializer(), lesson)
}
