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

    fun setVignette(v: Vignette) {
        pushHistory()
        editState = editState.copy(vignette = v)
    }

    // --- transform helpers --------------------------------------------------
    // Each geometry change is a new step on the ordered list, so a crop added
    // earlier is never lost when you rotate or flip afterwards.
    private fun addOp(op: com.elendheim.pictureeditor.model.GeoOp) {
        pushHistory()
        val t = editState.transform
        editState = editState.copy(transform = t.copy(ops = t.ops + op))
    }

    fun rotateRight() = addOp(com.elendheim.pictureeditor.model.GeoOp.Rotate(cw = true))
    fun rotateLeft() = addOp(com.elendheim.pictureeditor.model.GeoOp.Rotate(cw = false))
    fun toggleFlipH() = addOp(com.elendheim.pictureeditor.model.GeoOp.Flip(horizontal = true))
    fun toggleFlipV() = addOp(com.elendheim.pictureeditor.model.GeoOp.Flip(horizontal = false))

    // Picking an aspect frames the middle of the current photo. Re-picking an
    // aspect replaces the previous aspect crop rather than stacking on it.
    fun setAspect(preset: com.elendheim.pictureeditor.model.AspectPreset) {
        val base = previewBitmap ?: return
        val t = editState.transform
        val ratio = preset.ratio
        // Drop any trailing crop that came from a previous aspect tap.
        val baseOps = t.ops.dropLastWhile { it is com.elendheim.pictureeditor.model.GeoOp.Crop && it.fromAspect }
        if (ratio == null) {
            pushHistory()
            editState = editState.copy(transform = t.copy(ops = baseOps, aspect = preset))
            return
        }
        // Measure the image with those base steps applied so the centred crop
        // lands correctly.
        val baseImg = ImageEngine.geometry(base, t.copy(ops = baseOps))
        val rect = ImageEngine.centeredCrop(baseImg.width, baseImg.height, ratio)
        baseImg.recycle()
        pushHistory()
        val newOps = baseOps + com.elendheim.pictureeditor.model.GeoOp.Crop(rect, fromAspect = true)
        editState = editState.copy(transform = t.copy(ops = newOps, aspect = preset))
    }

    // Called by the crop tool with the region framed on the current image. It
    // is appended, so it applies on top of whatever is already there.
    fun setCrop(rect: com.elendheim.pictureeditor.model.NormRect) {
        addOp(com.elendheim.pictureeditor.model.GeoOp.Crop(rect, fromAspect = false))
    }

    // Restore the original framing: clear every geometry step.
    fun restoreOriginal() {
        if (editState.transform.isNeutral) return
        pushHistory()
        editState = editState.copy(
            transform = com.elendheim.pictureeditor.model.Transform()
        )
        message = "Original framing restored"
    }

    // --- added picture layers ----------------------------------------------
    // Up to eight added pictures on one photo keeps things responsive.
    val maxLayers = 8

    fun addLayer(uri: Uri) {
        if (layers.size >= maxLayers) {
            message = "That is the most pictures you can add ($maxLayers)"
            return
        }
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

    // Move / resize / rotate the layer by a gesture delta. Reading the current
    // value here (not in the gesture closure) is what makes dragging actually
    // stick instead of snapping back every frame. Centres snap to the thirds
    // and middle grid lines when they get close.
    fun moveLayer(id: String, dxNorm: Float, dyNorm: Float, zoom: Float, rotDelta: Float) {
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0) return
        val current = layers[index]
        // Snapping is an accessibility aid; it can be turned off in settings.
        val nx = snapToGrid((current.center.x + dxNorm).coerceIn(0f, 1f))
        val ny = snapToGrid((current.center.y + dyNorm).coerceIn(0f, 1f))
        layers[index] = current.copy(
            center = com.elendheim.pictureeditor.model.NormPoint(nx, ny),
            scale = (current.scale * zoom).coerceIn(0.05f, 3f),
            rotationDeg = current.rotationDeg + rotDelta
        )
    }

    // Snap to 1/3, 1/2 and 2/3 when within a small threshold, if snapping is on.
    private fun snapToGrid(v: Float): Float {
        if (!settings.snapToGrid) return v
        val lines = floatArrayOf(1f / 3f, 0.5f, 2f / 3f)
        for (line in lines) if (kotlin.math.abs(line - v) < 0.02f) return line
        return v
    }

    // Change the opacity of a specific added picture (0 clear, 1 solid).
    fun setLayerOpacity(id: String, opacity: Float) {
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0) return
        layers[index] = layers[index].copy(opacity = opacity.coerceIn(0f, 1f))
    }

    // Change the colour adjustments of a specific added picture.
    fun setLayerAdjust(id: String, adjust: AdjustParams) {
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0) return
        layers[index] = layers[index].copy(adjust = adjust)
    }

    // Apply a saved look to a specific added picture.
    fun applyFilterToLayer(id: String, preset: FilterPreset) {
        setLayerAdjust(id, preset.adjust)
        message = "Applied ${preset.name} to the added picture"
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
                    PlacedLayer(lb, layer.center.x, layer.center.y, layer.scale, layer.rotationDeg, layer.adjust, layer.opacity)
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
