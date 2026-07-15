package com.elendheim.pictureeditor.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A slider with its name and current value always visible. Two accessibility
 * touches are built in, not bolted on: the value is announced to screen readers
 * as it changes, and tapping the number resets the slider to neutral for people
 * who find precise dragging hard. Fine mode adds detents for finer control.
 */
@Composable
fun LabeledSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    fine: Boolean = false,
    resetValue: Float = 0f,
    display: (Float) -> String = { formatSigned(it) }
) {
    Column(modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelLarge
            )
            // Tap the value to snap back to neutral -> an easy reset target.
            Text(
                text = display(value),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .clickable { onValueChange(resetValue) }
                    .clearAndSetSemantics {
                        contentDescription = "Reset $label"
                    }
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = if (fine) 40 else 0,
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    stateDescription = "$label, ${display(value)}"
                }
        )
    }
}

/** Small heading used above a group of controls. */
@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        style = MaterialTheme.typography.labelMedium,
        modifier = modifier.padding(vertical = 6.dp)
    )
}

// Show a tidy signed number like +20 or -8 from a -1..1 slider value.
fun formatSigned(value: Float): String {
    val scaled = (value * 100f).toInt()
    return if (scaled > 0) "+$scaled" else scaled.toString()
}
