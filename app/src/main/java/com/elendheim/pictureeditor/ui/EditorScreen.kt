package com.elendheim.pictureeditor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lens
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elendheim.pictureeditor.model.BuiltInFilters
import com.elendheim.pictureeditor.ui.components.AdjustPanel
import com.elendheim.pictureeditor.ui.components.FiltersPanel
import com.elendheim.pictureeditor.ui.components.ImagePreview
import com.elendheim.pictureeditor.ui.components.TransformPanel
import com.elendheim.pictureeditor.ui.components.VignettePanel

// The bottom tools, one per group.
private enum class Tool(val label: String, val icon: ImageVector) {
    ADJUST("Adjust", Icons.Filled.Tune),
    TRANSFORM("Transform", Icons.Filled.Crop),
    FILTERS("Examples", Icons.Filled.PhotoFilter),
    VIGNETTE("Vignette", Icons.Filled.Lens)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    vm: EditorViewModel,
    onOpenSettings: () -> Unit,
    onOpenCrop: () -> Unit
) {
    var tool by remember { mutableStateOf(Tool.ADJUST) }
    var showExport by remember { mutableStateOf(false) }
    var showSaveFilter by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.openImage(uri) }

    val addPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.addLayer(uri) }

    fun pickPhoto() = picker.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )
    fun addPhoto() = addPicker.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbar.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { BrandTitle() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                ),
                actions = {
                    if (vm.hasImage) {
                        IconButton(onClick = { vm.undo() }, enabled = vm.canUndo) {
                            Icon(Icons.Filled.Undo, contentDescription = "Undo")
                        }
                        IconButton(onClick = { vm.reset() }) {
                            Icon(Icons.Filled.RestartAlt, contentDescription = "Reset all edits")
                        }
                        IconButton(onClick = { addPhoto() }) {
                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = "Add another picture")
                        }
                        IconButton(onClick = { showExport = true }) {
                            Icon(Icons.Filled.Download, contentDescription = "Export to gallery")
                        }
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                Box(
                    Modifier.weight(1f).fillMaxWidth().padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bmp = vm.previewBitmap
                    if (bmp != null) {
                        ImagePreview(
                            source = bmp,
                            state = vm.editState,
                            layers = vm.layers,
                            selectedLayerId = vm.selectedLayerId,
                            onSelectLayer = { vm.selectLayer(it) },
                            onLayerGesture = { id, dx, dy, zoom, rot ->
                                vm.moveLayer(id, dx, dy, zoom, rot)
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyState(onPick = ::pickPhoto)
                    }
                }

                if (vm.hasImage) {
                    // The raised control panel: rounded top, lifted clear of the
                    // navigation bar so nothing sits under a system gesture area.
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                            )
                            .navigationBarsPadding()
                            .padding(top = 10.dp, bottom = 6.dp)
                    ) {
                        if (vm.selectedLayer != null) {
                            SelectedLayerBar(onRemove = { vm.deleteSelectedLayer() })
                        }
                        ToolBar(current = tool, onSelect = { tool = it })
                        // When an added picture is selected, Adjust and Examples
                        // change that picture instead of the base.
                        val editingLayer = vm.selectedLayer
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 170.dp, max = 300.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            when (tool) {
                                Tool.ADJUST -> AdjustPanel(
                                    adjust = editingLayer?.adjust ?: vm.editState.adjust,
                                    onChange = {
                                        if (editingLayer != null) vm.setLayerAdjust(editingLayer.id, it)
                                        else vm.setAdjust(it)
                                    },
                                    fine = vm.settings.fineSliders
                                )
                                Tool.TRANSFORM -> TransformPanel(
                                    transform = vm.editState.transform,
                                    onRotateLeft = { vm.rotateBy(-90f) },
                                    onRotateRight = { vm.rotateBy(90f) },
                                    onFlipH = { vm.toggleFlipH() },
                                    onFlipV = { vm.toggleFlipV() },
                                    onAspect = { vm.setAspect(it) },
                                    onOpenCrop = onOpenCrop
                                )
                                Tool.FILTERS -> FiltersPanel(
                                    builtIns = BuiltInFilters.all,
                                    custom = vm.customFilters,
                                    onApply = {
                                        if (editingLayer != null) vm.applyFilterToLayer(editingLayer.id, it)
                                        else vm.applyFilter(it)
                                    },
                                    onDelete = { vm.deleteFilter(it.id) },
                                    onSaveCurrent = { showSaveFilter = true }
                                )
                                Tool.VIGNETTE -> VignettePanel(
                                    vignette = vm.editState.vignette,
                                    onChange = { vm.setVignette(it) },
                                    fine = vm.settings.fineSliders
                                )
                            }
                        }
                    }
                }
            }

            if (vm.busy) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showExport) {
        ExportDialog(
            defaultFormat = vm.settings.defaultFormat,
            defaultQuality = vm.settings.defaultQuality,
            onDismiss = { showExport = false },
            onConfirm = { format, quality ->
                showExport = false
                vm.exportImage(format, quality)
            }
        )
    }

    if (showSaveFilter) {
        SaveFilterDialog(
            onDismiss = { showSaveFilter = false },
            onConfirm = { name ->
                showSaveFilter = false
                vm.saveCurrentAsFilter(name)
            }
        )
    }
}

// The top left wordmark: Elendheim big, picture editor small underneath.
@Composable
private fun BrandTitle() {
    Column {
        Text(
            text = "Elendheim",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = "picture editor",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            maxLines = 1
        )
    }
}

// The horizontal strip of tools. Scrolls, so labels never get squeezed.
@Composable
private fun ToolBar(current: Tool, onSelect: (Tool) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Tool.values().forEach { t ->
            ToolPill(tool = t, selected = current == t, onClick = { onSelect(t) })
        }
    }
}

@Composable
private fun ToolPill(tool: Tool, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp)
    ) {
        Icon(tool.icon, contentDescription = tool.label, tint = fg)
        Text(
            tool.label,
            color = fg,
            maxLines = 1,
            fontSize = 12.sp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

@Composable
private fun SelectedLayerBar(onRemove: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Editing added picture - Adjust and Examples change this one",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1
        )
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove added picture")
        }
    }
}

@Composable
private fun EmptyState(onPick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            Icons.Filled.AddPhotoAlternate,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            "Open a photo to start editing",
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 12.dp)
        )
        Text(
            "Your original is never changed. Edits are saved as new photos.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        FilledTonalButton(onClick = onPick) { Text("Open a photo") }
    }
}
