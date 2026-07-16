package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * A rectangle expressed as fractions of the image, so it stays correct at any
 * resolution. All four values run 0f..1f, measured from the top left. This is
 * how a crop is stored - what part of the picture the frame keeps.
 */
@Serializable
data class NormRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceIn(0f, 1f)
    val height: Float get() = (bottom - top).coerceIn(0f, 1f)

    // True when the rectangle still covers the whole image -> no crop applied.
    val isFull: Boolean
        get() = left <= 0.0001f && top <= 0.0001f && right >= 0.9999f && bottom >= 0.9999f

    companion object {
        val FULL = NormRect(0f, 0f, 1f, 1f)
    }
}

/**
 * A single point in the same 0f..1f fractional space, used to place added
 * picture layers over the base image.
 */
@Serializable
data class NormPoint(val x: Float = 0.5f, val y: Float = 0.5f)
