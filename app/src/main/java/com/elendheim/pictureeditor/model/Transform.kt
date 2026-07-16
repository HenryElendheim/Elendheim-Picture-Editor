package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * Geometry side of an edit: rotation in 90 degree steps, horizontal / vertical
 * flips, and an aspect ratio crop. Crop is a centred cut to the chosen ratio,
 * which is exactly what the social presets need.
 */
@Serializable
data class Transform(
    val rotationDeg: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val aspect: AspectPreset = AspectPreset.ORIGINAL,
    // The kept region after rotation and flips. null means the whole image.
    val crop: NormRect? = null
) {
    val isNeutral: Boolean
        get() = rotationDeg == 0f && !flipH && !flipV &&
            aspect == AspectPreset.ORIGINAL && (crop == null || crop.isFull)
}

/**
 * Aspect ratio presets. ratio is width divided by height; ORIGINAL keeps the
 * photo's own shape so it has no fixed number.
 */
@Serializable
enum class AspectPreset(val label: String, val ratio: Float?, val use: String) {
    ORIGINAL("Original", null, "Keep the photo's own shape"),
    SQUARE("Square 1:1", 1f / 1f, "Instagram post"),
    PORTRAIT("Portrait 4:5", 4f / 5f, "Instagram tall post"),
    STORY("Vertical 9:16", 9f / 16f, "Stories, Reels, TikTok, Shorts"),
    WIDE("Wide 16:9", 16f / 9f, "YouTube frame or thumbnail"),
    CLASSIC("Classic 3:2", 3f / 2f, "Standard camera ratio"),
    STANDARD("Standard 4:3", 4f / 3f, "Classic photo ratio");
}
