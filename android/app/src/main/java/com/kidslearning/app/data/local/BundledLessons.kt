package com.kidslearning.app.data.local

import android.content.Context
import com.kidslearning.app.data.remote.LessonJson
import com.kidslearning.app.domain.model.Lesson

/**
 * Loads the example lessons bundled as assets. The assets directory is wired
 * straight to lesson-schema/examples/ at build time, so the app always plays the
 * exact fixtures the backend test suite validated -- one source of truth.
 *
 * Generated lessons will live in Room; bundled ones exist so the whole child-mode
 * flow works offline with zero backend, which is also how it is demoed and tested.
 */
object BundledLessons {

    fun load(context: Context): List<Lesson> =
        context.assets.list("")
            .orEmpty()
            .filter { it.endsWith(".json") }
            .mapNotNull { name ->
                runCatching {
                    context.assets.open(name).bufferedReader().use { it.readText() }
                        .let(LessonJson::decode)
                }.getOrNull() // a non-lesson JSON asset is simply skipped
            }
            .sortedBy { it.age }
}
