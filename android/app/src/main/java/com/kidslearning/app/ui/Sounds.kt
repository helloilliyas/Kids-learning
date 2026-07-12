package com.kidslearning.app.ui

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.staticCompositionLocalOf
import com.kidslearning.app.R

/**
 * Small bundled sound effects (synthesized, soft, child-friendly) played through
 * one shared SoundPool. Renderers reach it via [LocalSounds]; the parent can turn
 * effects off, which [enabled] gates centrally.
 */
class Sounds(context: Context) {

    enum class Effect { POP, WHOOSH, CHIME, OOPS, TADA }

    var enabled: Boolean = true

    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val ids = mapOf(
        Effect.POP to pool.load(context, R.raw.sfx_pop, 1),
        Effect.WHOOSH to pool.load(context, R.raw.sfx_whoosh, 1),
        Effect.CHIME to pool.load(context, R.raw.sfx_chime, 1),
        Effect.OOPS to pool.load(context, R.raw.sfx_oops, 1),
        Effect.TADA to pool.load(context, R.raw.sfx_tada, 1),
    )

    fun play(effect: Effect) {
        if (!enabled) return
        ids[effect]?.let { pool.play(it, 1f, 1f, 1, 0, 1f) }
    }

    fun release() = pool.release()
}

/** No-op default so previews and tests never crash on missing audio. */
val LocalSounds = staticCompositionLocalOf<(Sounds.Effect) -> Unit> { {} }
