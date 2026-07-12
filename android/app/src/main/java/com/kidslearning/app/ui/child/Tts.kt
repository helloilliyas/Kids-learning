package com.kidslearning.app.ui.child

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import java.util.Locale

/**
 * Wrapper over Android's on-device TextToSpeech: free, offline, and sufficient
 * for read-aloud and story narration -- the v2 plan removed backend audio
 * generation in favour of this.
 *
 * Modern devices ship many voices per language (Google's natural voices among
 * them); [availableVoices] lists the offline ones for the lesson language so the
 * parent can pick a narrator in settings, and [applyVoice]/[applyRate] switch the
 * engine live. The chosen voice/rate are passed in at construction and re-applied
 * once the engine finishes initialising.
 */
class Tts(
    context: Context,
    private val language: String,
    private val preferredVoice: String = "",
    private val preferredRate: Float = 1.0f,
) {
    private var ready = false
    private var onReadyListeners = mutableListOf<() -> Unit>()

    private val engine = TextToSpeech(context) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            runCatching {
                setLanguageTag(language)
                if (preferredVoice.isNotBlank()) applyVoice(preferredVoice)
                applyRate(preferredRate)
            }
            onReadyListeners.forEach { it() }
            onReadyListeners.clear()
        }
    }

    private fun setLanguageTag(tag: String) {
        val locale = Locale.forLanguageTag(tag)
        engine.language = if (locale.language.isNotBlank()) locale else Locale.ENGLISH
    }

    /** Runs the block now if the engine is ready, or as soon as it becomes ready. */
    fun whenReady(block: () -> Unit) {
        if (ready) block() else onReadyListeners.add(block)
    }

    /**
     * Offline voices matching the lesson language, best quality first. Network
     * voices are excluded: narration must never stall on connectivity.
     */
    fun availableVoices(): List<Voice> {
        if (!ready) return emptyList()
        val lang = Locale.forLanguageTag(language).language.ifBlank { "en" }
        return runCatching {
            engine.voices
                .orEmpty()
                .filter { it.locale.language == lang && !it.isNetworkConnectionRequired }
                .sortedWith(compareByDescending<Voice> { it.quality }.thenBy { it.name })
        }.getOrDefault(emptyList())
    }

    fun applyVoice(name: String) {
        if (!ready) return
        runCatching {
            engine.voices?.firstOrNull { it.name == name }?.let { engine.voice = it }
        }
    }

    fun applyRate(rate: Float) {
        runCatching { engine.setSpeechRate(rate.coerceIn(0.5f, 1.5f)) }
    }

    fun speak(text: String) {
        if (ready) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "lesson_tts")
        }
    }

    fun stop() {
        runCatching { engine.stop() }
    }

    fun shutdown() {
        engine.stop()
        engine.shutdown()
    }
}
