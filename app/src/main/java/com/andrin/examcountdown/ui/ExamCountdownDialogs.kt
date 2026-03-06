package com.andrin.examcountdown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.data.SyncDiagnostics
import com.andrin.examcountdown.data.SyncStatus
import com.andrin.examcountdown.util.SchoolTime
import com.andrin.examcountdown.util.formatSyncDateTime

@Composable
internal fun SettingsSectionCard(
    title: String,
    containerColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            content()
        }
    }
}

@Composable
internal fun QuickActionTile(
    text: String,
    subtitle: String? = null,
    icon: ImageVector,
    showAlertBadge: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                subtitle?.takeIf { it.isNotBlank() }?.let { helperText ->
                    Text(
                        text = helperText,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (showAlertBadge) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = "!",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = "›",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun SettingToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
internal fun SyncStatusStrip(
    syncStatus: SyncStatus,
    onRepairIcalLink: (() -> Unit)? = null
) {
    val error = syncStatus.lastSyncError
    val now = SchoolTime.nowMillis()
    val staleThresholdMillis = 24L * 60L * 60L * 1000L
    val isStale = syncStatus.lastSyncAtMillis?.let { last ->
        now - last > staleThresholdMillis
    } == true
    val headline = when {
        !error.isNullOrBlank() -> error
        syncStatus.lastSyncAtMillis != null -> {
            val time = formatSyncDateTime(syncStatus.lastSyncAtMillis)
            if (isStale) {
                "Zuletzt synchronisiert: $time (veraltet)"
            } else {
                "Zuletzt synchronisiert: $time"
            }
        }
        else -> "Noch keine Synchronisierung"
    }
    val details = when {
        !error.isNullOrBlank() -> null
        isStale -> "Letzter erfolgreicher Sync ist älter als 24h. Bitte oben auf Aktualisieren tippen."
        else -> syncStatus.lastSyncSummary?.takeIf { it.isNotBlank() }
    }

    val containerColor = when {
        !error.isNullOrBlank() -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f)
        isStale -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.65f)
    }

    val textColor = when {
        !error.isNullOrBlank() -> MaterialTheme.colorScheme.onErrorContainer
        isStale -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val showRepairAction = onRepairIcalLink != null && isIcalLinkRepairRecommended(error)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = containerColor
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
            Text(
                text = headline,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            details?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor.copy(alpha = 0.9f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (showRepairAction) {
                TextButton(
                    onClick = { onRepairIcalLink?.invoke() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Link reparieren")
                }
            }
        }
    }
}

@Composable
internal fun SyncDiagnosticsDialog(
    diagnostics: SyncDiagnostics,
    syncStatus: SyncStatus,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sync-Diagnose") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Letzter erfolgreicher Sync: ${
                        syncStatus.lastSyncAtMillis?.let(::formatSyncDateTime) ?: "noch nie"
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Letzter Versuch: ${
                        diagnostics.lastAttemptAtMillis?.let(::formatSyncDateTime) ?: "unbekannt"
                    }",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Dauer: ${diagnostics.lastDurationMillis?.let(::formatDurationMillis) ?: "unbekannt"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "HTTP-Status: ${diagnostics.lastHttpStatusCode?.toString() ?: "unbekannt"}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Delta-Sync: ${if (diagnostics.lastDeltaNotModified) "Keine Änderungen (304)" else "Daten aktualisiert"}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Importiert",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Prüfungen: ${diagnostics.importedExams}", style = MaterialTheme.typography.bodySmall)
                        Text("Lektionen: ${diagnostics.importedLessons}", style = MaterialTheme.typography.bodySmall)
                        Text("Events: ${diagnostics.importedEvents}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Erkannte Änderungen",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text("Gesamt: ${diagnostics.changedLessons}", style = MaterialTheme.typography.bodySmall)
                        Text("Verschoben: ${diagnostics.movedLessons}", style = MaterialTheme.typography.bodySmall)
                        Text("Raum geändert: ${diagnostics.roomChangedLessons}", style = MaterialTheme.typography.bodySmall)
                    }
                }

                val error = diagnostics.lastErrorReason
                    ?.takeIf { it.isNotBlank() }
                    ?: syncStatus.lastSyncError
                if (!error.isNullOrBlank()) {
                    Text(
                        text = "Fehlerursache: $error",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
internal fun ChangelogDialog(
    versionName: String,
    entries: List<String>,
    onShowFullLog: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neu in Version $versionName") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                entries.forEach { entry ->
                    Text(
                        text = "• $entry",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Verstanden")
            }
        },
        dismissButton = {
            TextButton(onClick = onShowFullLog) {
                Text("Mehr anzeigen")
            }
        }
    )
}

@Composable
internal fun FullChangelogDialog(
    versions: List<ChangelogVersion>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update-Log") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                versions.forEachIndexed { index, version ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Version ${version.versionName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        version.highlights.forEach { entry ->
                            Text(
                                text = "• $entry",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    if (index != versions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}

@Composable
internal fun ExportDialog(
    onDismiss: () -> Unit,
    onExportExamsCsv: () -> Unit,
    onExportTimetableCsv: () -> Unit,
    onExportExamsPdf: () -> Unit,
    onExportTimetablePdf: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("CSV/PDF Export") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportExamsCsv
                ) {
                    Text("Prüfungen als CSV")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTimetableCsv
                ) {
                    Text("Stundenplan als CSV")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportExamsPdf
                ) {
                    Text("Prüfungen als PDF")
                }
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onExportTimetablePdf
                ) {
                    Text("Stundenplan als PDF")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )
}
