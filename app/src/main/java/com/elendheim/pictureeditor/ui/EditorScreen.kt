package com.elendheim.pictureeditor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.model.BuiltInFilters
import com.elendheim.pictureeditor.ui.components.AdjustPanel
import com.elendheim.pictureeditor.ui.components.FiltersPanel
import com.elendheim.pictureeditor.ui.components.ImagePreview
import com.elendheim.pictureeditor.ui.components.TransformPanel
import com.elendheim.pictureeditor.ui.components.VignettePanel

// The bottom tabs, one per group of tools.
private enum class Tool(val label: String) {
    ADJUST("Adjust"), TRANSFORM("Transform"), FILTERS("Filters"), VIGNETTE("Vignette")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    vm: EditorViewModel,
    onOpenSettings: () -> Unit
) {
    var tool by remember { mutableStateOf(Tool.ADJUST) }
    var showExport by remember { mutableStateOf(false) }
    var showSaveFilter by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    // The system photo picker. No storage permission needed for this.
    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> if (uri != null) vm.openImage(uri) }

    fun pickPhoto() = picker.launch(
        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
    )

    // Surface one line messages then clear them.
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
                title = { Text("Elendheim Picture Editor") },
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
                        ImagePreview(source = bmp, state = vm.editState, modifier = Modifier.fillMaxSize())
                    } else {
                        EmptyState(onPick = ::pickPhoto)
                    }
                }

                if (vm.hasImage) {
                    ToolTabs(current = tool, onSelect = { tool = it })
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp, max = 300.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        when (tool) {
                            Tool.ADJUST -> AdjustPanel(
                                adjust = vm.editState.adjust,
                                onChange = { vm.setAdjust(it) },
                                fine = vm.settings.fineSliders
                            )
                            Tool.TRANSFORM -> TransformPanel(
                                transform = vm.editState.transform,
                                onChange = { vm.setTransform(it) }
                            )
                            Tool.FILTERS -> FiltersPanel(
                                builtIns = BuiltInFilters.all,
                                custom = vm.customFilters,
                                onApply = { vm.applyFilter(it) },
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

@Composable
private fun ToolTabs(current: Tool, onSelect: (Tool) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Tool.values().forEach { t ->
            FilterChip(
                selected = current == t,
                onClick = { onSelect(t) },
                label = { Text(t.label) },
                modifier = Modifier.weight(1f)
            )
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
