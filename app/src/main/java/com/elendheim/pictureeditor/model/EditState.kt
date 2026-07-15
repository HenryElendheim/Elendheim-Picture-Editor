package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * The whole recipe for one edit, kept as pure data. Nothing here touches the
 * original pixels; the engine replays this on a copy for the preview and again
 * at full size for export. Undo / redo is just keeping a history of these.
 */
@Serializable
data class EditState(
    val adjust: AdjustParams = AdjustParams(),
    val transform: Transform = Transform(),
    val vignette: Vignette = Vignette()
) {
    val isNeutral: Boolean
        get() = adjust.isNeutral && transform.isNeutral && vignette.isNeutral

    // Build the state a saved filter describes, keeping the current geometry.
    fun withFilter(preset: FilterPreset): EditState = copy(
        adjust = preset.adjust,
        vignette = preset.vignette ?: Vignette()
    )
}
