package com.elendheim.pictureeditor.data

import android.content.Context
import android.net.Uri
import com.elendheim.pictureeditor.model.FilterPack
import com.elendheim.pictureeditor.model.FilterPreset
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Keeps the user's custom filters on the device and handles the filter pack
 * import / export. Everything is a plain JSON file, so it is easy to read,
 * share and future proof. No database, no cloud, no account.
 */
class FilterStore(context: Context) {

    private val appContext = context.applicationContext
    private val file = File(appContext.filesDir, "custom_filters.json")

    // Lenient + ignore unknown keys -> old and future packs still load.
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /** Read the saved custom filters, or an empty list on a fresh install. */
    fun load(): List<FilterPreset> {
        if (!file.exists()) return emptyList()
        return runCatching {
            json.decodeFromString<FilterPack>(file.readText()).filters
        }.getOrDefault(emptyList())
    }

    /** Write the full custom filter list back to disk. */
    fun save(filters: List<FilterPreset>) {
        val pack = FilterPack(exportedAt = 0L, filters = filters)
        file.writeText(json.encodeToString(FilterPack.serializer(), pack))
    }

    /**
     * Write every custom filter to a file the user chose in their storage.
     * exportedAt is passed in so the caller controls the timestamp.
     */
    fun exportPack(uri: Uri, filters: List<FilterPreset>, exportedAt: Long): Boolean =
        runCatching {
            val pack = FilterPack(exportedAt = exportedAt, filters = filters)
            appContext.contentResolver.openOutputStream(uri, "w")?.use { out ->
                out.write(json.encodeToString(FilterPack.serializer(), pack).toByteArray())
            }
            true
        }.getOrDefault(false)

    /**
     * Read a filter pack the user picked. Returns the filters it holds, or an
     * empty list if the file could not be parsed.
     */
    fun importPack(uri: Uri): List<FilterPreset> = runCatching {
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            val text = input.readBytes().decodeToString()
            json.decodeFromString<FilterPack>(text).filters
        } ?: emptyList()
    }.getOrDefault(emptyList())

    /**
     * Merge imported filters into the existing set. Matching ids are replaced,
     * new ids are added, so importing the same pack twice stays clean.
     */
    fun merge(existing: List<FilterPreset>, incoming: List<FilterPreset>): List<FilterPreset> {
        val byId = existing.associateBy { it.id }.toMutableMap()
        for (f in incoming) byId[f.id] = f
        return byId.values.sortedByDescending { it.createdAt }
    }
}
