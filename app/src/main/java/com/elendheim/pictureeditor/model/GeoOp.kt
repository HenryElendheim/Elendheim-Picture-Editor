package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * A single geometry step. Edits are an ordered list of these, replayed in order
 * on a copy of the photo. Because a crop is just one step in the list, a flip or
 * rotation added afterwards is a later step that re-orients the already-cropped
 * picture - so the crop stays instead of being lost.
 */
@Serializable
sealed class GeoOp {
    // Quarter turn. cw = clockwise (rotate right), otherwise anticlockwise.
    @Serializable
    data class Rotate(val cw: Boolean) : GeoOp()

    // Mirror. horizontal = left/right, otherwise top/bottom.
    @Serializable
    data class Flip(val horizontal: Boolean) : GeoOp()

    // Keep only this fraction of the current image. fromAspect marks crops that
    // came from tapping an aspect chip, so re-picking an aspect replaces them.
    @Serializable
    data class Crop(val rect: NormRect, val fromAspect: Boolean = false) : GeoOp()
}
