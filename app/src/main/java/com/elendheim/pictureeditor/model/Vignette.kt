package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * A vignette darkens (or lightens) the edges of the frame.
 *  - amount  -> strength; positive darkens, negative lightens, 0 is off.
 *  - size    -> 0f..1f, how far in from the edge the effect reaches.
 *  - feather -> 0f..1f, how soft the transition is.
 */
@Serializable
data class Vignette(
    val amount: Float = 0f,
    val size: Float = 0.5f,
    val feather: Float = 0.5f
) {
    val isNeutral: Boolean get() = amount == 0f
}
