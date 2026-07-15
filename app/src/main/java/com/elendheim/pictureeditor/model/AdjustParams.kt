package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * The colour adjustments for an edit. Every value is a delta where 0 means
 * "leave it alone", so a fresh, all zero AdjustParams is exactly the original
 * image. That makes saving, resetting and comparing filters trivial.
 *
 * Ranges the UI uses:
 *  - exposure, brightness, contrast, saturation, warmth, tint -> -1f .. 1f
 *  - hue -> -180f .. 180f degrees
 */
@Serializable
data class AdjustParams(
    val exposure: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val warmth: Float = 0f,
    val tint: Float = 0f,
    val hue: Float = 0f
) {
    // True when nothing has been changed -> lets us skip work on the original.
    val isNeutral: Boolean
        get() = exposure == 0f && brightness == 0f && contrast == 0f &&
            saturation == 0f && warmth == 0f && tint == 0f && hue == 0f
}
