package com.andrin.examcountdown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FilterControls(
    resultLabel: String,
    activeCount: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    onReset: () -> Unit
) {
    val actions: @Composable () -> Unit = {
        if (activeCount > 0) {
            TextButton(onClick = onReset, modifier = Modifier.heightIn(min = 48.dp)) { Text("Zurücksetzen", maxLines = 1) }
        }
        TextButton(onClick = onToggle, modifier = Modifier.heightIn(min = 48.dp).semantics {
            stateDescription = if (expanded) "Geöffnet" else "Geschlossen"
        }) {
            Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(if (expanded) "Schließen" else if (activeCount > 0) "Filter ($activeCount)" else "Filter",
                modifier = Modifier.padding(start = 4.dp), maxLines = 1)
        }
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / LocalDensity.current.fontScale < 320.dp) {
            Column {
                Text(resultLabel, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) { actions() }
            }
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(resultLabel, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                actions()
            }
        }
    }
}

@Composable
internal fun ActiveFilterChip(label: String, onRemove: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onRemove,
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
        modifier = Modifier.heightIn(min = 48.dp).semantics { contentDescription = "Filter $label entfernen" },
        colors = InputChipDefaults.inputChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedTrailingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}
