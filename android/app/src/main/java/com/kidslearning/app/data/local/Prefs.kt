package com.kidslearning.app.data.local

import android.content.Context

/**
 * Tiny settings store for parent-configured values. The backend URL and shared
 * app token are configuration, not child data, so SharedPreferences is enough.
 */
class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("kids_learning_prefs", Context.MODE_PRIVATE)

    var backendUrl: String
        get() = prefs.getString("backend_url", "").orEmpty()
        set(value) = prefs.edit().putString("backend_url", value.trim()).apply()

    var appToken: String
        get() = prefs.getString("app_token", "").orEmpty()
        set(value) = prefs.edit().putString("app_token", value.trim()).apply()

    /**
     * Anthropic API key for on-device generation (personal-use mode). Entered by
     * the parent at runtime and kept in the app's private storage -- it is never
     * compiled into the APK and never leaves the device except to call the API.
     */
    var anthropicKey: String
        get() = prefs.getString("anthropic_key", "").orEmpty()
        set(value) = prefs.edit().putString("anthropic_key", value.trim()).apply()

    /** Engine-specific name of the chosen narrator voice; empty = device default. */
    var voiceName: String
        get() = prefs.getString("voice_name", "").orEmpty()
        set(value) = prefs.edit().putString("voice_name", value).apply()

    /** Narration speed multiplier; kids often benefit from slightly under 1.0. */
    var speechRate: Float
        get() = prefs.getFloat("speech_rate", 1.0f)
        set(value) = prefs.edit().putFloat("speech_rate", value).apply()

    var soundEffects: Boolean
        get() = prefs.getBoolean("sound_effects", true)
        set(value) = prefs.edit().putBoolean("sound_effects", value).apply()
}
