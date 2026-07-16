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
import com.elendheim.pictureeditor.engine.PlacedLayer
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

    // --- added picture layers ----------------------------------------------
    val layers = mutableStateListOf<LayerItem>()
    var selectedLayerId by mutableStateOf<String?>(null)
        private set
    val selectedLayer: LayerItem? get() = layers.firstOrNull { it.id == selectedLayerId }

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
                layers.clear()
                selectedLayerId = null
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

    // --- transform helpers --------------------------------------------------
    // Rotating or flipping would put an existing crop in the wrong place, so
    // the crop is cleared back to the full frame when the geometry changes.
    fun rotateBy(deltaDeg: Float) {
        val t = editState.transform
        val next = (t.rotationDeg + deltaDeg + 360f) % 360f
        pushHistory()
        editState = editState.copy(transform = t.copy(rotationDeg = next, crop = null))
    }

    fun toggleFlipH() {
        pushHistory()
        val t = editState.transform
        editState = editState.copy(transform = t.copy(flipH = !t.flipH, crop = null))
    }

    fun toggleFlipV() {
        pushHistory()
        val t = editState.transform
        editState = editState.copy(transform = t.copy(flipV = !t.flipV, crop = null))
    }

    // Picking an aspect frames the middle of the photo straight away; the crop
    // tool can then refine it. Original clears the crop.
    fun setAspect(preset: com.elendheim.pictureeditor.model.AspectPreset) {
        val base = previewBitmap ?: return
        val ratio = preset.ratio
        val t = editState.transform
        if (ratio == null) {
            pushHistory()
            editState = editState.copy(transform = t.copy(aspect = preset, crop = null))
            return
        }
        // Measure the rotated / flipped base so the centred crop is correct.
        val rotated = ImageEngine.rotateFlip(base, t)
        val rect = ImageEngine.centeredCrop(rotated.width, rotated.height, ratio)
        if (rotated !== base) rotated.recycle()
        pushHistory()
        editState = editState.copy(transform = t.copy(aspect = preset, crop = rect))
    }

    // Called by the crop tool with the region the user framed.
    fun setCrop(rect: com.elendheim.pictureeditor.model.NormRect) {
        pushHistory()
        editState = editState.copy(transform = editState.transform.copy(crop = rect))
    }

    // --- added picture layers ----------------------------------------------
    fun addLayer(uri: Uri) {
        busy = true
        viewModelScope.launch {
            val bmp = withContext(Dispatchers.IO) {
                ImageLoader.loadPreview(context, uri, maxDim = 1200)
            }
            if (bmp != null) {
                val item = LayerItem(id = UUID.randomUUID().toString(), uri = uri, bitmap = bmp)
                layers.add(item)
                selectedLayerId = item.id
                message = "Picture added. Drag to move, pinch to resize."
            } else {
                message = "Could not add that picture"
            }
            busy = false
        }
    }

    fun selectLayer(id: String?) {
        selectedLayerId = id
    }

    fun updateLayer(id: String, center: com.elendheim.pictureeditor.model.NormPoint? = null, scale: Float? = null, rotationDeg: Float? = null) {
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = layers[index]
        layers[index] = current.copy(
            center = center ?: current.center,
            scale = scale ?: current.scale,
            rotationDeg = rotationDeg ?: current.rotationDeg
        )
    }

    fun deleteSelectedLayer() {
        val id = selectedLayerId ?: return
        layers.removeAll { it.id == id }
        selectedLayerId = null
    }

    // --- intro --------------------------------------------------------------
    fun markIntroSeen() {
        if (!settings.introSeen) updateSettings(settings.copy(introSeen = true))
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
        // Snapshot the mutable state on the main thread before going to IO.
        val state = editState
        val layerList = layers.toList()
        busy = true
        viewModelScope.launch {
            val saved = withContext(Dispatchers.IO) {
                val full = ImageLoader.loadFull(context, uri) ?: return@withContext null
                // Reload each added picture at full quality for the final render.
                val placed = layerList.mapNotNull { layer ->
                    val lb = ImageLoader.loadFull(context, layer.uri, maxDim = 2048)
                        ?: return@mapNotNull null
                    PlacedLayer(lb, layer.center.x, layer.center.y, layer.scale, layer.rotationDeg)
                }
                val rendered = ImageEngine.render(full, state, placed)
                full.recycle()
                placed.forEach { it.bitmap.recycle() }
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
