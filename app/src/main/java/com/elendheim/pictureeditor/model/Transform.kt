package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * The geometry side of an edit: an ordered list of rotate / flip / crop steps
 * plus the aspect ratio currently chosen for the crop frame. Keeping the steps
 * in order is what lets a crop survive a later flip or rotation.
 */
@Serializable
data class Transform(
    val ops: List<GeoOp> = emptyList(),
    val aspect: AspectPreset = AspectPreset.ORIGINAL
) {
    val isNeutral: Boolean get() = ops.isEmpty()

    val hasCrop: Boolean get() = ops.any { it is GeoOp.Crop }

    // Simple on/off hints for the flip chips. Parity of the flip steps.
    val flipHActive: Boolean get() = ops.count { it is GeoOp.Flip && it.horizontal } % 2 == 1
    val flipVActive: Boolean get() = ops.count { it is GeoOp.Flip && !it.horizontal } % 2 == 1
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
