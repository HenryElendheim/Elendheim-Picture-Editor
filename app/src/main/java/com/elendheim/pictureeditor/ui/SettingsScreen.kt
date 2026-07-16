package com.elendheim.pictureeditor.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elendheim.pictureeditor.BuildConfig
import com.elendheim.pictureeditor.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: EditorViewModel,
    onBack: () -> Unit
) {
    val settings = vm.settings

    // Save a filter pack to a file the user picks.
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportFilters(uri) }

    // Load a filter pack from a file the user picks.
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importFilters(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back to editor")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            SectionLabel("Accessibility", Modifier.padding(top = 8.dp))
            ToggleRow(
                title = "High contrast",
                subtitle = "Brighter text and a stronger accent",
                checked = settings.highContrast
            ) { vm.updateSettings(settings.copy(highContrast = it)) }
            ToggleRow(
                title = "Reduce motion",
                subtitle = "Skip fades and animated reveals",
                checked = settings.reduceMotion
            ) { vm.updateSettings(settings.copy(reduceMotion = it)) }
            ToggleRow(
                title = "Larger tap targets",
                subtitle = "Roomier controls for easier tapping",
                checked = settings.largeTapTargets
            ) { vm.updateSettings(settings.copy(largeTapTargets = it)) }
            ToggleRow(
                title = "Fine slider mode",
                subtitle = "Add detents for more precise adjustments",
                checked = settings.fineSliders
            ) { vm.updateSettings(settings.copy(fineSliders = it)) }
            ToggleRow(
                title = "Haptics",
                subtitle = "Small vibration feedback where supported",
                checked = settings.haptics
            ) { vm.updateSettings(settings.copy(haptics = it)) }
            ToggleRow(
                title = "Snap added pictures to grid",
                subtitle = "Added pictures line up to a thirds grid; turn off for free placement",
                checked = settings.snapToGrid
            ) { vm.updateSettings(settings.copy(snapToGrid = it)) }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            SectionLabel("Your filters")
            Text(
                "Move your saved looks to and from your files as a filter pack.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("elendheim_filters.json") },
                    modifier = Modifier.weight(1f)
                ) { Text("Export filters") }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.weight(1f)
                ) { Text("Import filters") }
            }

            HorizontalDivider(Modifier.padding(vertical = 12.dp))

            SectionLabel("About")
            Text(
                "Elendheim Picture Editor v${BuildConfig.VERSION_NAME}",
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                "A private, non-destructive photo editor. Adjust, crop and filter " +
                    "your photos, then save them to your gallery as new files. Your " +
                    "originals are never changed, and the app has no internet access.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
    }
}

/** One settings row: a title, a short note, and a switch, all one tap target. */
@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
