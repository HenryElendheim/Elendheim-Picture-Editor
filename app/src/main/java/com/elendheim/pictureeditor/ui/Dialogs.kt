package com.elendheim.pictureeditor.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.export.ExportFormat

/** Pick the format and, for JPEG, the quality before saving to the gallery. */
@Composable
fun ExportDialog(
    defaultFormat: ExportFormat,
    defaultQuality: Int,
    onDismiss: () -> Unit,
    onConfirm: (ExportFormat, Int) -> Unit
) {
    var format by remember { mutableStateOf(defaultFormat) }
    var quality by remember { mutableFloatStateOf(defaultQuality.toFloat()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export to gallery") },
        text = {
            Column {
                Text("Format")
                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.values().forEach { f ->
                        FilterChip(
                            selected = format == f,
                            onClick = { format = f },
                            label = { Text(f.label) }
                        )
                    }
                }
                if (format == ExportFormat.JPEG) {
                    Text(
                        "Quality: ${quality.toInt()}",
                        modifier = Modifier.padding(top = 14.dp)
                    )
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 40f..100f
                    )
                }
                Text(
                    "Saved to Pictures / Elendheim Picture Editor as a new file.",
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(format, quality.toInt()) }) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Name and save the current look as a reusable filter. */
@Composable
fun SaveFilterDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save as filter") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("Filter name") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
