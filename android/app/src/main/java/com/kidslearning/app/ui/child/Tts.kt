package com.kidslearning.app.ui.child

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Thin wrapper over Android's on-device TextToSpeech: free, offline, and
 * sufficient for read-aloud support -- the v2 plan removed backend audio
 * generation in favour of this.
 */
class Tts(context: Context, language: String) {
    private var ready = false
    private val engine = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching { setLanguageTag(language) }
        }
    }

    private fun setLanguageTag(tag: String) {
        val locale = Locale.forLanguageTag(tag)
        engine.language = if (locale.language.isNotBlank()) locale else Locale.ENGLISH
    }

    fun speak(text: String) {
        if (ready) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson_tts")
        }
    }

    fun shutdown() {
        engine.stop()
        engine.shutdown()
    }
}
