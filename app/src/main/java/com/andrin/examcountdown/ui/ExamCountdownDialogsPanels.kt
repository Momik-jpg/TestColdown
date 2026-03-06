package com.andrin.examcountdown.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.data.CollisionRuleSettings
import com.andrin.examcountdown.data.QuietHoursConfig
import com.andrin.examcountdown.ui.theme.AppDimens
import com.andrin.examcountdown.ui.theme.AppOpacity
import java.net.URI

@Composable
internal fun IcalImportDialog(
    primaryUrl: String,
    secondaryUrl: String,
    includeEvents: Boolean,
    isImporting: Boolean,
    onPrimaryUrlChange: (String) -> Unit,
    onSecondaryUrlChange: (String) -> Unit,
    onIncludeEventsChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onImport: () -> Unit
) {
    var showUrl by rememberSaveable { mutableStateOf(primaryUrl.isBlank() && secondaryUrl.isBlank()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("iCal-Kalender verbinden") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = primaryUrl,
                    onValueChange = onPrimaryUrlChange,
                    label = { Text("iCal-URL 1") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showUrl) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showUrl = !showUrl }) {
                            Icon(
                                imageVector = if (showUrl) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showUrl) {
                                    "Link ausblenden"
                                } else {
                                    "Link anzeigen"
                                }
                            )
                        }
                    }
                )
                OutlinedTextField(
                    value = secondaryUrl,
                    onValueChange = onSecondaryUrlChange,
                    label = { Text("iCal-URL 2 (optional)") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showUrl) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    }
                )

                Text(
                    text = if (showUrl) {
                        "Die Links werden lokal verschlüsselt gespeichert."
                    } else {
                        "Links sind aus Sicherheitsgründen ausgeblendet. Tippe auf das Auge zum Anzeigen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "schulNetz: Agenda > Schüler/-innenpläne > Exports > \"Diesen Plan im iCal-Format abonnieren\" > Link kopieren.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Events zusätzlich importieren",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = includeEvents,
                        onCheckedChange = onIncludeEventsChange
                    )
                }

                Text(
                    text = if (includeEvents) {
                        "Es werden Prüfungen, Lektionen und Events importiert."
                    } else {
                        "Standard: nur Prüfungen und Lektionen."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isImporting && primaryUrl.isNotBlank(),
                onClick = onImport
            ) {
                Text(if (isImporting) "Import läuft..." else "Importieren")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isImporting,
                onClick = onDismiss
            ) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
internal fun QuickActionsDialog(
    showSyncStatusStrip: Boolean,
    onDismiss: () -> Unit,
    onSyncNow: () -> Unit,
    onShowSyncStatusStripChange: (Boolean) -> Unit,
    onOpenReminderSettings: () -> Unit,
    onOpenSyncSettings: () -> Unit,
    onOpenIcalImport: () -> Unit,
    onOpenHelp: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenSyncDiagnostics: () -> Unit,
    onOpenExport: () -> Unit,
    onOpenChangelog: () -> Unit,
    onOpenPersonalization: () -> Unit,
    onOpenAppLock: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    hasUnseenChangelog: Boolean
) {
    val scrollState = rememberScrollState()
    val dialogContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = AppOpacity.settingsContainer)
    var showAdvanced by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Einstellungen",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Schnellzugriff auf wichtige Aktionen",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = AppDimens.dialogMaxHeightLarge)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(AppDimens.sectionSpacing)
            ) {
                SettingsSectionCard(
                    title = "Anzeige",
                    containerColor = dialogContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Sync-Leiste anzeigen",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Zeigt zuletzt synchronisiert + Status direkt oben.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = showSyncStatusStrip,
                            onCheckedChange = onShowSyncStatusStripChange
                        )
                    }
                }

                SettingsSectionCard(
                    title = "Kalender & Sync",
                    containerColor = dialogContainer
                ) {
                    Button(
                        onClick = onSyncNow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Refresh,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text("Jetzt synchronisieren")
                    }
                    QuickActionTile(
                        text = "Kalender verbinden",
                        subtitle = null,
                        icon = Icons.Outlined.CloudDownload,
                        onClick = onOpenIcalImport
                    )
                    QuickActionTile(
                        text = "Benachrichtigungen",
                        subtitle = "Vorzeiten und Ruhezeiten",
                        icon = Icons.Outlined.NotificationsActive,
                        onClick = onOpenReminderSettings
                    )
                    QuickActionTile(
                        text = "Automatisch aktualisieren",
                        subtitle = "Intervall für Hintergrund-Sync",
                        icon = Icons.Outlined.Sync,
                        onClick = onOpenSyncSettings
                    )
                }

                SettingsSectionCard(
                    title = "App",
                    containerColor = dialogContainer
                ) {
                    QuickActionTile(
                        text = "App anpassen",
                        subtitle = null,
                        icon = Icons.Outlined.MoreVert,
                        onClick = onOpenPersonalization
                    )
                    QuickActionTile(
                        text = "Hilfe",
                        subtitle = "Kurzanleitung",
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onClick = onOpenHelp
                    )
                }

                TextButton(
                    onClick = { showAdvanced = !showAdvanced },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text(if (showAdvanced) "Weniger Optionen" else "Weitere Optionen")
                }

                if (showAdvanced) {
                    SettingsSectionCard(
                        title = "Datenschutz & Sicherheit",
                        containerColor = dialogContainer
                    ) {
                        QuickActionTile(
                            text = "App-Schutz (PIN)",
                            subtitle = "Optional mit Biometrie",
                            icon = Icons.Outlined.Lock,
                            onClick = onOpenAppLock
                        )
                        QuickActionTile(
                            text = "Datenschutz",
                            subtitle = "Lokale Daten und Screenshot-Schutz",
                            icon = Icons.Outlined.Lock,
                            onClick = onOpenPrivacy
                        )
                        QuickActionTile(
                            text = "Sync-Diagnose",
                            subtitle = "Status und Fehlersuche",
                            icon = Icons.Outlined.Schedule,
                            onClick = onOpenSyncDiagnostics
                        )
                    }

                    SettingsSectionCard(
                        title = "Daten",
                        containerColor = dialogContainer
                    ) {
                        QuickActionTile(
                            text = "CSV/PDF Export",
                            subtitle = "Prüfungen und Agenda exportieren",
                            icon = Icons.Outlined.CloudDownload,
                            onClick = onOpenExport
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = onExportBackup,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Backup Export")
                            }
                            FilledTonalButton(
                                onClick = onImportBackup,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Backup Import")
                            }
                        }
                    }

                    SettingsSectionCard(
                        title = "Version",
                        containerColor = dialogContainer
                    ) {
                        QuickActionTile(
                            text = "Was ist neu",
                            subtitle = "Update-Verlauf anzeigen",
                            icon = Icons.Outlined.CalendarToday,
                            showAlertBadge = hasUnseenChangelog,
                            onClick = onOpenChangelog
                        )
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
internal fun OnboardingDialog(
    primaryUrl: String,
    secondaryUrl: String,
    includeEvents: Boolean,
    statusMessage: String,
    isBusy: Boolean,
    canFinish: Boolean,
    onPrimaryUrlChange: (String) -> Unit,
    onSecondaryUrlChange: (String) -> Unit,
    onIncludeEventsChange: (Boolean) -> Unit,
    onTest: () -> Unit,
    onFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var showUrl by rememberSaveable { mutableStateOf(primaryUrl.isBlank() && secondaryUrl.isBlank()) }
    val hasAnyUrl = primaryUrl.isNotBlank() || secondaryUrl.isNotBlank()
    val statusColor = when {
        statusMessage.isBlank() -> MaterialTheme.colorScheme.onSurfaceVariant
        canFinish -> MaterialTheme.colorScheme.primary
        statusMessage.contains("fehlgeschlagen", ignoreCase = true) -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Start in 3 Schritten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("1 Link", "2 Test", "3 Fertig").forEachIndexed { index, title ->
                        FilterChip(
                            selected = step == index,
                            onClick = { step = index },
                            label = { Text(title) }
                        )
                    }
                }

                if (step == 0) {
                    Text(
                        text = "Schritt 1: Füge deinen iCal-Link ein. schulNetz: Agenda > Schüler/-innenpläne > Exports > \"Diesen Plan im iCal-Format abonnieren\" > Link kopieren.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = primaryUrl,
                        onValueChange = onPrimaryUrlChange,
                        label = { Text("iCal-URL 1") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showUrl) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        trailingIcon = {
                            IconButton(onClick = { showUrl = !showUrl }) {
                                Icon(
                                    imageVector = if (showUrl) {
                                        Icons.Outlined.VisibilityOff
                                    } else {
                                        Icons.Outlined.Visibility
                                    },
                                    contentDescription = if (showUrl) {
                                        "Link ausblenden"
                                    } else {
                                        "Link anzeigen"
                                    }
                                )
                            }
                        }
                    )
                    OutlinedTextField(
                        value = secondaryUrl,
                        onValueChange = onSecondaryUrlChange,
                        label = { Text("iCal-URL 2 (optional)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showUrl) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        }
                    )

                    Text(
                        text = if (showUrl) {
                            "Die Links werden lokal verschlüsselt gespeichert."
                        } else {
                            "Links sind aus Sicherheitsgründen ausgeblendet."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Events zusätzlich importieren",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Switch(
                            checked = includeEvents,
                            onCheckedChange = onIncludeEventsChange
                        )
                    }
                }

                if (step == 1) {
                    Text(
                        text = "Schritt 2: Verbindung testen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (includeEvents) {
                                    "Import: Prüfungen, Lektionen und Events"
                                } else {
                                    "Import: Prüfungen und Lektionen"
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = when {
                                    !hasAnyUrl -> "Noch kein Link eingegeben"
                                    secondaryUrl.isBlank() -> "Link 1: ${maskUrlForDisplay(primaryUrl)}"
                                    else -> "Link 1: ${maskUrlForDisplay(primaryUrl)}\nLink 2: ${maskUrlForDisplay(secondaryUrl)}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (step == 2) {
                    Text(
                        text = "Schritt 3: Fertig und starten.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (canFinish) "Alles bereit. Du kannst die App jetzt normal nutzen." else "Bitte zuerst Verbindung testen.",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "Später kannst du jederzeit oben mit ↻ aktualisieren.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (statusMessage.isNotBlank()) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !isBusy && step > 0,
                    onClick = { step -= 1 }
                ) {
                    Text("Zurück")
                }
                when (step) {
                    0 -> {
                        TextButton(
                            enabled = !isBusy && hasAnyUrl,
                            onClick = { step = 1 }
                        ) {
                            Text("Weiter")
                        }
                    }

                    1 -> {
                        TextButton(
                            enabled = !isBusy && hasAnyUrl,
                            onClick = onTest
                        ) {
                            Text(if (isBusy) "Prüfe..." else "Testen")
                        }
                        TextButton(
                            enabled = !isBusy && canFinish,
                            onClick = { step = 2 }
                        ) {
                            Text("Weiter")
                        }
                    }

                    else -> {
                        TextButton(
                            enabled = !isBusy && canFinish,
                            onClick = onFinish
                        ) {
                            Text(if (isBusy) "Sync..." else "Fertig")
                        }
                    }
                }
            }
        },
        dismissButton = {
            TextButton(enabled = !isBusy, onClick = onDismiss) {
                Text("Später")
            }
        }
    )
}

@Composable
internal fun HelpDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hilfe & Troubleshooting") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HelpSectionTitle("Schnellstart (2 Minuten)")
                HelpStepLine("1.", "SchulNetz öffnen -> Agenda -> Schüler/-innenpläne.")
                HelpStepLine("2.", "Exports öffnen -> \"Diesen Plan im iCal-Format abonnieren\".")
                HelpStepLine("3.", "iCal-Link kopieren (nicht öffnen).")
                HelpStepLine("4.", "In der App Link einfügen -> Testen -> Fertig.")
                HelpStepLine("5.", "Oben auf Aktualisieren tippen.")

                HelpSectionTitle("Was die Tabs machen")
                HelpBulletLine("Prüfungen: Countdown, Suche, Filter.")
                HelpBulletLine("Stundenplan: Lektionen mit Verschiebungen und Raumänderungen.")
                HelpBulletLine("Events: Gesamtagenda nach Zeit.")
                HelpBulletLine("Notenrechner: Durchschnitt, Zielnote, Punkte-Rechner.")

                HelpSectionTitle("Täglich")
                HelpStepLine("1.", "App öffnen.")
                HelpStepLine("2.", "Aktualisieren.")
                HelpStepLine("3.", "Nächste Prüfungen und Lektionen prüfen.")

                HelpSectionTitle("Wenn etwas nicht klappt")
                HelpBulletLine("Sync-Fehler: Link + Internet prüfen; bei HTTP 410 neuen iCal-Link erstellen.")
                HelpBulletLine("Keine Events: In iCal-Einstellungen den Event-Import aktivieren.")
                HelpBulletLine("Backup: Einstellungen -> Backup Export/Import.")
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
private fun HelpSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun HelpStepLine(step: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = step,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun HelpBulletLine(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "•",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun BackupExportDialog(
    title: String,
    message: String,
    password: String,
    allowUnencrypted: Boolean,
    confirmLabel: String,
    onPasswordChange: (String) -> Unit,
    onAllowUnencryptedChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }
    val passwordRequired = !allowUnencrypted
    val canConfirm = allowUnencrypted || password.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = {
                        Text(
                            if (passwordRequired) {
                                "Passwort (erforderlich)"
                            } else {
                                "Passwort (optional)"
                            }
                        )
                    },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showPassword) {
                                    "Passwort ausblenden"
                                } else {
                                    "Passwort anzeigen"
                                }
                            )
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unverschlüsselt exportieren",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    Switch(
                        checked = allowUnencrypted,
                        onCheckedChange = onAllowUnencryptedChange
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = canConfirm
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
internal fun BackupPasswordDialog(
    title: String,
    message: String,
    password: String,
    confirmLabel: String,
    onPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var showPassword by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Passwort (optional)") },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Icon(
                                imageVector = if (showPassword) {
                                    Icons.Outlined.VisibilityOff
                                } else {
                                    Icons.Outlined.Visibility
                                },
                                contentDescription = if (showPassword) {
                                    "Passwort ausblenden"
                                } else {
                                    "Passwort anzeigen"
                                }
                            )
                        }
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
internal fun PrivacyDialog(
    screenshotProtectionEnabled: Boolean,
    onScreenshotProtectionChange: (Boolean) -> Unit,
    onDeleteAllData: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showDeleteConfirmDialog by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Datenschutz") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Kurz erklärt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Daten bleiben lokal auf deinem Gerät.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "iCal-Links sind verschlüsselt gespeichert.",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Für Sync wird nur dein iCal-Link abgerufen.",
                    style = MaterialTheme.typography.bodySmall
                )

                Text(
                    text = "Berechtigungen",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Benachrichtigungen werden erst dann angefragt, wenn du Erinnerungen wirklich nutzen willst.",
                    style = MaterialTheme.typography.bodySmall
                )
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Screenshots blockieren",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Aktiviert FLAG_SECURE gegen Mitschnitt in Apps/Recent-Screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = screenshotProtectionEnabled,
                            onCheckedChange = onScreenshotProtectionChange
                        )
                    }
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://github.com/Momik-jpg/TestColdown")
                                )
                            )
                        }
                    }
                ) {
                    Text("Datenschutz-Infos öffnen")
                }

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { showDeleteConfirmDialog = true }
                ) {
                    Text("Alle lokalen Daten löschen")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        }
    )

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Lokale Daten löschen?") },
            text = {
                Text(
                    text = "Das entfernt lokale Termine, Einstellungen und iCal-Links auf diesem Gerät. Dieser Schritt kann nicht rückgängig gemacht werden.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAllData()
                        onDismiss()
                    }
                ) {
                    Text("Löschen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
internal fun PersonalizationDialog(
    showTimetableTab: Boolean,
    showAgendaTab: Boolean,
    showExamCollisionBadges: Boolean,
    collisionRules: CollisionRuleSettings,
    accessibilityModeEnabled: Boolean,
    simpleModeEnabled: Boolean,
    showSetupGuideCard: Boolean,
    onDismiss: () -> Unit,
    onShowTimetableTabChange: (Boolean) -> Unit,
    onShowAgendaTabChange: (Boolean) -> Unit,
    onShowExamCollisionBadgesChange: (Boolean) -> Unit,
    onCollisionRulesChange: (CollisionRuleSettings) -> Unit,
    onAccessibilityModeChange: (Boolean) -> Unit,
    onSimpleModeChange: (Boolean) -> Unit,
    onShowSetupGuideCardChange: (Boolean) -> Unit
) {
    var showAdvancedCollisionRules by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Personalisieren") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SettingToggleRow(
                            label = "Stundenplan-Tab anzeigen",
                            checked = showTimetableTab,
                            onCheckedChange = onShowTimetableTabChange
                        )
                        SettingToggleRow(
                            label = "Agenda-Tab anzeigen",
                            checked = showAgendaTab,
                            onCheckedChange = onShowAgendaTabChange
                        )
                        SettingToggleRow(
                            label = "Setup-Hilfe anzeigen",
                            checked = showSetupGuideCard,
                            onCheckedChange = onShowSetupGuideCardChange
                        )
                        SettingToggleRow(
                            label = "Barrierefreiheit-Modus",
                            checked = accessibilityModeEnabled,
                            onCheckedChange = onAccessibilityModeChange
                        )
                        SettingToggleRow(
                            label = "Einfach-Modus (weniger Optionen)",
                            checked = simpleModeEnabled,
                            onCheckedChange = onSimpleModeChange
                        )
                        SettingToggleRow(
                            label = "Kollisions-Badges anzeigen",
                            checked = showExamCollisionBadges,
                            onCheckedChange = onShowExamCollisionBadgesChange
                        )
                    }
                }

                if (showExamCollisionBadges) {
                    TextButton(
                        onClick = { showAdvancedCollisionRules = !showAdvancedCollisionRules },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(
                            if (showAdvancedCollisionRules) {
                                "Kollisionsregeln ausblenden"
                            } else {
                                "Kollisionsregeln anzeigen"
                            }
                        )
                    }
                }

                if (showExamCollisionBadges && showAdvancedCollisionRules) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Kollisionen - Erweitert",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            SettingToggleRow(
                                label = "Kollisionen mit Lektionen",
                                checked = collisionRules.includeLessonCollisions,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(includeLessonCollisions = it)
                                    )
                                }
                            )
                            SettingToggleRow(
                                label = "Kollisionen mit Events",
                                checked = collisionRules.includeEventCollisions,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(includeEventCollisions = it)
                                    )
                                }
                            )
                            SettingToggleRow(
                                label = "Nur anderes Fach",
                                checked = collisionRules.onlyDifferentSubject,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(onlyDifferentSubject = it)
                                    )
                                }
                            )
                            SettingToggleRow(
                                label = "Nur echte Zeitüberschneidung",
                                checked = collisionRules.requireExactTimeOverlap,
                                onCheckedChange = {
                                    onCollisionRulesChange(
                                        collisionRules.copy(requireExactTimeOverlap = it)
                                    )
                                }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fertig")
            }
        }
    )
}


@Composable
internal fun ReminderSettingsDialog(
    initialConfig: QuietHoursConfig,
    syncIntervalMinutes: Long,
    onDismiss: () -> Unit,
    onSave: (QuietHoursConfig) -> Unit,
    onSendTestNotification: () -> Unit,
    onOpenSyncSettings: () -> Unit
) {
    var enabled by remember(initialConfig) { mutableStateOf(initialConfig.enabled) }
    var startMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.startMinutesOfDay) }
    var endMinutes by remember(initialConfig) { mutableIntStateOf(initialConfig.endMinutesOfDay) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Benachrichtigungen") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Stille Zeiten aktiv",
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it }
                    )
                }

                if (enabled) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openTimePicker(
                                context = context,
                                initialMinutesOfDay = startMinutes,
                                onPicked = { picked -> startMinutes = picked }
                            )
                        }
                    ) {
                        Text("Stille Zeit ab: ${formatMinutesOfDay(startMinutes)}")
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            openTimePicker(
                                context = context,
                                initialMinutesOfDay = endMinutes,
                                onPicked = { picked -> endMinutes = picked }
                            )
                        }
                    ) {
                        Text("Stille Zeit bis: ${formatMinutesOfDay(endMinutes)}")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                startMinutes = 22 * 60
                                endMinutes = 7 * 60
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("22:00-07:00")
                        }
                        OutlinedButton(
                            onClick = {
                                startMinutes = 23 * 60
                                endMinutes = 6 * 60 + 30
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("23:00-06:30")
                        }
                    }
                    Text(
                        text = "Erinnerungen in stiller Zeit werden auf das Ende der stillen Zeit verschoben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Auto-Sync aktuell: alle $syncIntervalMinutes Minuten",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onSendTestNotification,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Test senden")
                    }
                    OutlinedButton(
                        onClick = onOpenSyncSettings,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Sync einstellen")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        QuietHoursConfig(
                            enabled = enabled,
                            startMinutesOfDay = startMinutes,
                            endMinutesOfDay = endMinutes
                        )
                    )
                }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
internal fun SyncSettingsDialog(
    initialIntervalMinutes: Long,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    var intervalRaw by rememberSaveable(initialIntervalMinutes) {
        mutableStateOf(initialIntervalMinutes.toString())
    }
    val parsed = intervalRaw.trim().toLongOrNull()
    val normalized = parsed?.coerceIn(15L, 12L * 60L)
    val isValid = normalized != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Auto-Synchronisierung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = intervalRaw,
                    onValueChange = { intervalRaw = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Intervall in Minuten") },
                    placeholder = { Text("z. B. 60") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "Gültig: 15 bis 720 Minuten. Empfohlen: 60 oder 180 Minuten.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30L, 60L, 180L, 360L).forEach { quick ->
                        OutlinedButton(
                            onClick = { intervalRaw = quick.toString() },
                            modifier = Modifier.width(84.dp),
                            contentPadding = PaddingValues(horizontal = 0.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "$quick",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                if (!isValid) {
                    Text(
                        text = "Bitte eine Zahl zwischen 15 und 720 eingeben.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = isValid,
                onClick = { onSave(normalized ?: 60L) }
            ) {
                Text("Speichern")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

