package com.elendheim.pictureeditor.model

import kotlinx.serialization.Serializable

/**
 * A saved look. It is just a bundle of adjustment values plus an optional
 * vignette, so applying one is instant and re-applying it to any future photo
 * is one tap. Save your own from the current edit and reuse it forever.
 */
@Serializable
data class FilterPreset(
    val id: String,
    val name: String,
    val adjust: AdjustParams,
    val vignette: Vignette? = null,
    val builtIn: Boolean = false,
    val createdAt: Long = 0L
)

/**
 * One file that holds every custom filter. This is what gets written to and
 * read from the user's files so looks can travel between devices or be shared
 * as a pack. schemaVersion lets old packs keep loading in future versions.
 */
@Serializable
data class FilterPack(
    val schemaVersion: Int = 1,
    val exportedAt: Long = 0L,
    val filters: List<FilterPreset> = emptyList()
)
