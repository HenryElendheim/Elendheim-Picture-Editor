package com.elendheim.pictureeditor.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.model.AdjustParams
import com.elendheim.pictureeditor.model.AspectPreset
import com.elendheim.pictureeditor.model.FilterPreset
import com.elendheim.pictureeditor.model.Transform
import com.elendheim.pictureeditor.model.Vignette

/** The colour sliders. Every one maps to a real adjustment in the engine. */
@Composable
fun AdjustPanel(
    adjust: AdjustParams,
    onChange: (AdjustParams) -> Unit,
    fine: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        LabeledSlider("Exposure", adjust.exposure, -1f..1f, { onChange(adjust.copy(exposure = it)) }, fine = fine)
        LabeledSlider("Brightness", adjust.brightness, -1f..1f, { onChange(adjust.copy(brightness = it)) }, fine = fine)
        LabeledSlider("Contrast", adjust.contrast, -1f..1f, { onChange(adjust.copy(contrast = it)) }, fine = fine)
        LabeledSlider("Saturation", adjust.saturation, -1f..1f, { onChange(adjust.copy(saturation = it)) }, fine = fine)
        LabeledSlider("Warmth", adjust.warmth, -1f..1f, { onChange(adjust.copy(warmth = it)) }, fine = fine)
        LabeledSlider("Tint", adjust.tint, -1f..1f, { onChange(adjust.copy(tint = it)) }, fine = fine)
        LabeledSlider(
            "Hue", adjust.hue, -180f..180f, { onChange(adjust.copy(hue = it)) },
            fine = fine, display = { "${it.toInt()}°" }
        )
    }
}

/** Rotate, flip and pick an aspect ratio. Crop is a centred cut to the ratio. */
@Composable
fun TransformPanel(
    transform: Transform,
    onChange: (Transform) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onChange(transform.copy(rotationDeg = (transform.rotationDeg - 90f + 360f) % 360f)) },
                modifier = Modifier.weight(1f)
            ) { Text("Rotate left") }
            OutlinedButton(
                onClick = { onChange(transform.copy(rotationDeg = (transform.rotationDeg + 90f) % 360f)) },
                modifier = Modifier.weight(1f)
            ) { Text("Rotate right") }
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = transform.flipH,
                onClick = { onChange(transform.copy(flipH = !transform.flipH)) },
                label = { Text("Flip H") },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = transform.flipV,
                onClick = { onChange(transform.copy(flipV = !transform.flipV)) },
                label = { Text("Flip V") },
                modifier = Modifier.weight(1f)
            )
        }
        SectionLabel("Aspect ratio", Modifier.padding(top = 10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AspectPreset.values().forEach { preset ->
                FilterChip(
                    selected = transform.aspect == preset,
                    onClick = { onChange(transform.copy(aspect = preset)) },
                    label = { Text(preset.label) },
                    modifier = Modifier.semantics { contentDescription = "${preset.label}. ${preset.use}" }
                )
            }
        }
    }
}

/** The vignette controls. */
@Composable
fun VignettePanel(
    vignette: Vignette,
    onChange: (Vignette) -> Unit,
    fine: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        LabeledSlider("Amount", vignette.amount, -1f..1f, { onChange(vignette.copy(amount = it)) }, fine = fine)
        LabeledSlider(
            "Size", vignette.size, 0f..1f, { onChange(vignette.copy(size = it)) },
            fine = fine, resetValue = 0.5f, display = { "${(it * 100).toInt()}" }
        )
        LabeledSlider(
            "Feather", vignette.feather, 0f..1f, { onChange(vignette.copy(feather = it)) },
            fine = fine, resetValue = 0.5f, display = { "${(it * 100).toInt()}" }
        )
    }
}

/** Premade looks and your own saved filters, plus a save button. */
@Composable
fun FiltersPanel(
    builtIns: List<FilterPreset>,
    custom: List<FilterPreset>,
    onApply: (FilterPreset) -> Unit,
    onDelete: (FilterPreset) -> Unit,
    onSaveCurrent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        FilledTonalButton(
            onClick = onSaveCurrent,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Save current look as a filter") }

        SectionLabel("Premade", Modifier.padding(top = 10.dp))
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            builtIns.forEach { preset ->
                FilterChip(
                    selected = false,
                    onClick = { onApply(preset) },
                    label = { Text(preset.name) },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
        }

        if (custom.isNotEmpty()) {
            SectionLabel("Your filters", Modifier.padding(top = 10.dp))
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                custom.forEach { preset ->
                    FilterChip(
                        selected = false,
                        onClick = { onApply(preset) },
                        label = { Text(preset.name) }
                    )
                }
            }
            // A plain row of delete buttons keeps the action obvious and reachable.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                custom.forEach { preset ->
                    OutlinedButton(onClick = { onDelete(preset) }) {
                        Text("Delete ${preset.name}")
                    }
                }
            }
        }
    }
}
