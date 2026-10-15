package com.andrin.examcountdown.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
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
    val isDark = isAppDarkTheme()
    val resolvedContainer = if (isDark) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    } else {
        containerColor
    }
    Surface(
        color = resolvedContainer,
        shape = MaterialTheme.shapes.large,
        tonalElevation = if (isDark) 0.dp else 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.16f else 0.42f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
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
    val isDark = isAppDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.12f else 0.24f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isDark) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.78f)
                },
                shape = MaterialTheme.shapes.small
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(18.dp)
                )
            }
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
                color = if (isDark) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.primary
                }
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
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).toggleable(
            value = checked, role = Role.Switch, onValueChange = onCheckedChange
        ).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            // Let long labels wrap rather than hiding the setting's meaning.
        )
        Switch(
            checked = checked,
            onCheckedChange = null
        )
    }
}

@Composable
internal fun SyncStatusStrip(syncStatus: SyncStatus, onRepairIcalLink: (() -> Unit)? = null) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val error = syncStatus.lastSyncError?.takeIf { it.isNotBlank() }
    val stale = syncStatus.lastSyncAtMillis?.let { SchoolTime.nowMillis() - it > 24L * 60 * 60 * 1000 } == true
    val title = when {
        error != null -> "Kalenderaktualisierung fehlgeschlagen"
        stale -> "Kalenderdaten veraltet"
        syncStatus.lastSyncAtMillis != null -> "Kalender aktualisiert"
        else -> "Noch keine Synchronisierung"
    }
    val timestamp = syncStatus.lastSyncAtMillis?.let { "Zuletzt: ${formatSyncDateTime(it)}" }
    val detail = error ?: if (stale) {
        "Die letzte erfolgreiche Aktualisierung ist älter als 24 Stunden. Tippe auf Aktualisieren."
    } else syncStatus.lastSyncSummary?.takeIf { it.isNotBlank() }
    val hasDetails = detail != null
    val container = if (error != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface
    val foreground = if (error != null) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium, color = container,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
                    .then(if (hasDetails) Modifier.clickable(role = Role.Button,
                        onClickLabel = if (expanded) "Synchronisationsdetails schließen" else "Synchronisationsdetails anzeigen",
                        onClick = { expanded = !expanded }) else Modifier)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        if (hasDetails) stateDescription = if (expanded) "Geöffnet" else "Geschlossen"
                    }.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.labelLarge, color = foreground)
                    timestamp?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = foreground) }
                }
                if (hasDetails) Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    null, Modifier.size(24.dp), tint = foreground)
            }
            if (expanded && detail != null) Text(detail, style = MaterialTheme.typography.bodySmall,
                color = foreground, modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp))
            if (onRepairIcalLink != null && isIcalLinkRepairRecommended(error)) {
                TextButton(onClick = onRepairIcalLink, modifier = Modifier.align(Alignment.End).heightIn(min = 48.dp)) {
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
