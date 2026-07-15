package com.elendheim.pictureeditor.ui

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.elendheim.pictureeditor.data.AppSettings
import com.elendheim.pictureeditor.data.FilterStore
import com.elendheim.pictureeditor.data.SettingsStore
import com.elendheim.pictureeditor.engine.ImageEngine
import com.elendheim.pictureeditor.export.ExportFormat
import com.elendheim.pictureeditor.export.GalleryExporter
import com.elendheim.pictureeditor.model.AdjustParams
import com.elendheim.pictureeditor.model.EditState
import com.elendheim.pictureeditor.model.FilterPreset
import com.elendheim.pictureeditor.model.Transform
import com.elendheim.pictureeditor.model.Vignette
import com.elendheim.pictureeditor.util.ImageLoader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Holds all editor state and does the work off the UI thread. The UI just reads
 * these values and calls these functions; it never touches pixels itself.
 */
class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val filterStore = FilterStore(app)
    private val settingsStore = SettingsStore(app)

    // The application context, used for loading and saving off the UI thread.
    private val context get() = getApplication<Application>()

    // --- image + edit state -------------------------------------------------
    var sourceUri by mutableStateOf<Uri?>(null)
        private set
    var previewBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var editState by mutableStateOf(EditState())
        private set
    var busy by mutableStateOf(false)
        private set

    // A short lived message for the snackbar (export done, filter saved...).
    var message by mutableStateOf<String?>(null)

    // Undo history -> snapshots of the recipe, newest last.
    private val history = mutableStateListOf<EditState>()
    val canUndo: Boolean get() = history.isNotEmpty()
    val hasImage: Boolean get() = previewBitmap != null

    // --- filters + settings -------------------------------------------------
    val customFilters = mutableStateListOf<FilterPreset>()
    var settings by mutableStateOf(AppSettings())
        private set

    init {
        settings = settingsStore.load()
        customFilters.addAll(filterStore.load())
    }

    // --- loading an image ---------------------------------------------------
    fun openImage(uri: Uri) {
        busy = true
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                ImageLoader.loadPreview(context, uri)
            }
            if (bmp != null) {
                sourceUri = uri
                previewBitmap = bmp
                editState = EditState()
                history.clear()
            } else {
                message = "Could not open that image"
            }
            busy = false
        }
    }

    // --- editing ------------------------------------------------------------
    private fun pushHistory() {
        history.add(editState)
        if (history.size > 30) history.removeAt(0)
    }

    fun setAdjust(a: AdjustParams) {
        pushHistory()
        editState = editState.copy(adjust = a)
    }

    fun setTransform(t: Transform) {
        pushHistory()
        editState = editState.copy(transform = t)
    }

    fun setVignette(v: Vignette) {
        pushHistory()
        editState = editState.copy(vignette = v)
    }

    fun applyFilter(preset: FilterPreset) {
        pushHistory()
        editState = editState.withFilter(preset)
        message = "Applied ${preset.name}"
    }

    fun undo() {
        if (history.isNotEmpty()) {
            editState = history.removeAt(history.size - 1)
        }
    }

    fun reset() {
        if (!editState.isNeutral) pushHistory()
        editState = EditState()
    }

    // --- custom filters -----------------------------------------------------
    fun saveCurrentAsFilter(name: String) {
        val trimmed = name.trim().ifEmpty { "My Filter" }
        val preset = FilterPreset(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            adjust = editState.adjust,
            vignette = editState.vignette.takeIf { !it.isNeutral },
            builtIn = false,
            createdAt = System.currentTimeMillis()
        )
        customFilters.add(0, preset)
        filterStore.save(customFilters.toList())
        message = "Saved filter \"$trimmed\""
    }

    fun deleteFilter(id: String) {
        customFilters.removeAll { it.id == id }
        filterStore.save(customFilters.toList())
    }

    fun exportFilters(uri: Uri) {
        val ok = filterStore.exportPack(uri, customFilters.toList(), System.currentTimeMillis())
        message = if (ok) "Filters exported" else "Export failed"
    }

    fun importFilters(uri: Uri) {
        val incoming = filterStore.importPack(uri)
        if (incoming.isEmpty()) {
            message = "No filters found in that file"
            return
        }
        val merged = filterStore.merge(customFilters.toList(), incoming)
        customFilters.clear()
        customFilters.addAll(merged)
        filterStore.save(merged)
        message = "Imported ${incoming.size} filter(s)"
    }

    // --- settings -----------------------------------------------------------
    fun updateSettings(newSettings: AppSettings) {
        settings = newSettings
        settingsStore.save(newSettings)
    }

    // --- export -------------------------------------------------------------
    fun exportImage(format: ExportFormat, quality: Int) {
        val uri = sourceUri ?: return
        busy = true
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                val full = ImageLoader.loadFull(context, uri) ?: return@withContext null
                val rendered = ImageEngine.render(full, editState)
                full.recycle()
                val stem = "Elendheim_" + System.currentTimeMillis()
                val out = GalleryExporter.save(context, rendered, format, quality, stem)
                rendered.recycle()
                out
            }
            message = if (saved != null) "Saved to your gallery" else "Export failed"
            busy = false
        }
    }
}
