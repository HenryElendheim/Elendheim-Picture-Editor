package com.elendheim.pictureeditor.data

import android.content.Context
import com.elendheim.pictureeditor.export.ExportFormat

/**
 * The accessibility and editing defaults, kept in one place. These are plain
 * on / off preferences that the UI reads to adapt itself.
 */
data class AppSettings(
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val largeTapTargets: Boolean = false,
    val fineSliders: Boolean = false,
    val haptics: Boolean = true,
    val defaultFormat: ExportFormat = ExportFormat.JPEG,
    val defaultQuality: Int = 95,
    // True once the intro splash has played -> it never shows again.
    val introSeen: Boolean = false
)

/** Saves the settings with the simplest possible storage: SharedPreferences. */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("elendheim_settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        highContrast = prefs.getBoolean(KEY_CONTRAST, false),
        reduceMotion = prefs.getBoolean(KEY_MOTION, false),
        largeTapTargets = prefs.getBoolean(KEY_TAPS, false),
        fineSliders = prefs.getBoolean(KEY_FINE, false),
        haptics = prefs.getBoolean(KEY_HAPTICS, true),
        defaultFormat = if (prefs.getString(KEY_FORMAT, "JPEG") == "PNG") {
            ExportFormat.PNG
        } else {
            ExportFormat.JPEG
        },
        defaultQuality = prefs.getInt(KEY_QUALITY, 95),
        introSeen = prefs.getBoolean(KEY_INTRO, false)
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_CONTRAST, s.highContrast)
            .putBoolean(KEY_MOTION, s.reduceMotion)
            .putBoolean(KEY_TAPS, s.largeTapTargets)
            .putBoolean(KEY_FINE, s.fineSliders)
            .putBoolean(KEY_HAPTICS, s.haptics)
            .putString(KEY_FORMAT, s.defaultFormat.name)
            .putInt(KEY_QUALITY, s.defaultQuality)
            .putBoolean(KEY_INTRO, s.introSeen)
            .apply()
    }

    private companion object {
        const val KEY_CONTRAST = "high_contrast"
        const val KEY_MOTION = "reduce_motion"
        const val KEY_TAPS = "large_taps"
        const val KEY_FINE = "fine_sliders"
        const val KEY_HAPTICS = "haptics"
        const val KEY_FORMAT = "default_format"
        const val KEY_QUALITY = "default_quality"
        const val KEY_INTRO = "intro_seen"
    }
}
