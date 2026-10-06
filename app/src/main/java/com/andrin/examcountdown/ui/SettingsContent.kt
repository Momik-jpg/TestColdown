package com.andrin.examcountdown.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.AccessibilityNew
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import java.util.Locale

internal enum class SettingsSection(val title: String, val hint: String, val icon: ImageVector) {
    CALENDAR("Kalender", "Import, Aktualisierung & Erinnerungen", Icons.Outlined.CalendarToday),
    DISPLAY("Darstellung", "Tabs, Lesbarkeit & Status", Icons.Outlined.Tune),
    SECURITY("Sicherheit", "PIN, Biometrie & Datenschutz", Icons.Outlined.Lock),
    DATA("Daten", "Export & Sicherungen", Icons.Outlined.FolderOpen),
    HELP("Hilfe & Version", "Bedienung & Änderungen", Icons.AutoMirrored.Outlined.HelpOutline)
}

internal enum class SettingsAction(val section: SettingsSection, val title: String, val hint: String, val icon: ImageVector) {
    SYNC_NOW(SettingsSection.CALENDAR, "Jetzt aktualisieren", "Schulkalender synchronisieren", Icons.Outlined.Refresh),
    CALENDAR(SettingsSection.CALENDAR, "Kalender verbinden", "iCal-Links hinzufügen oder bearbeiten", Icons.Outlined.CalendarToday),
    REMINDERS(SettingsSection.CALENDAR, "Erinnerungen", "Zeiten, Ruhezeiten & Testnachricht", Icons.Outlined.NotificationsActive),
    SYNC_OPTIONS(SettingsSection.CALENDAR, "Automatische Aktualisierung", "Intervall festlegen", Icons.Outlined.Sync),
    DIAGNOSTICS(SettingsSection.CALENDAR, "Sync-Diagnose", "Fehler & letzte Aktualisierung", Icons.Outlined.Settings),
    PERSONALIZE(SettingsSection.DISPLAY, "Ansicht & Bedienung", "Tabs, Lesbarkeit & Prüfungskonflikte", Icons.Outlined.Tune),
    ACCESSIBILITY(SettingsSection.DISPLAY, "Barrierefreie Bedienung", "TalkBack, große Schrift & hoher Kontrast", Icons.Outlined.AccessibilityNew),
    WIDGETS(SettingsSection.DISPLAY, "Widgets", "Startbildschirm, Countdown & Terminliste", Icons.Outlined.Widgets),
    SYNC_STATUS(SettingsSection.DISPLAY, "Sync-Status anzeigen", "Aktualisierung oben im Blick", Icons.Outlined.Sync),
    APP_LOCK(SettingsSection.SECURITY, "App-Schutz", "PIN & optionale Biometrie", Icons.Outlined.Lock),
    PRIVACY(SettingsSection.SECURITY, "Datenschutz", "Speicherung & Datenverarbeitung", Icons.Outlined.Lock),
    EXPORT(SettingsSection.DATA, "CSV / PDF exportieren", "Prüfungen als Datei teilen", Icons.Outlined.FolderOpen),
    BACKUP_EXPORT(SettingsSection.DATA, "Sicherung exportieren", "Backup von Daten und Einstellungen", Icons.Outlined.CloudUpload),
    BACKUP_IMPORT(SettingsSection.DATA, "Sicherung importieren", "Vorhandenes Backup wiederherstellen", Icons.Outlined.CloudDownload),
    HELP(SettingsSection.HELP, "Bedienung & Hilfe", "Schritt für Schritt starten", Icons.AutoMirrored.Outlined.HelpOutline),
    LICENSES(SettingsSection.HELP, "Lizenzen & Bildnachweise", "Open Source und KI-Illustrationen · offline lesbar", Icons.Outlined.Description),
    CHANGELOG(SettingsSection.HELP, "Was ist neu", "Änderungen dieser Version", Icons.Outlined.CalendarToday)
}

internal fun matchingSettings(query: String): List<SettingsAction> {
    val normalized = query.trim().lowercase(Locale.ROOT)
    return SettingsAction.entries.filter { option ->
        normalized.isEmpty() || "${option.title} ${option.hint} ${option.section.title}"
            .lowercase(Locale.ROOT).contains(normalized)
    }
}

/** Full-screen settings use the same saved preferences and action callbacks as the former menu. */
@Composable
internal fun SettingsContent(
    showSyncStatusStrip: Boolean,
    hasUnseenChangelog: Boolean,
    onShowSyncStatusStripChange: (Boolean) -> Unit,
    onAction: (SettingsAction) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedSection by rememberSaveable { mutableStateOf<SettingsSection?>(null) }
    val options = matchingSettings(query)
    val searching = query.isNotBlank()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (!searching && expandedSection == null) item("settings-world") {
            StudyWorldHeader("Deine Lernwelt", "Kalender, Darstellung und Schutz an einem Ort", illustrated = true)
        }
        item("settings-search") {
            AppTextField(
                value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
                label = { Text("Einstellungen suchen") }, placeholder = { Text("Kalender, Ansicht oder Backup") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Einstellungssuche löschen")
                    }
                },
                singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )
        }
        if (!searching && expandedSection == null) {
            item("settings-shortcuts") {
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Tune, null, Modifier.size(26.dp), tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Dein Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface)
                                Text("Alles für deinen Schulalltag", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        BoxWithConstraints(Modifier.fillMaxWidth()) {
                            if (maxWidth / LocalDensity.current.fontScale < 320.dp) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SetupButton(false, Modifier.fillMaxWidth(), onAction)
                                    SetupButton(true, Modifier.fillMaxWidth(), onAction)
                                }
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SetupButton(false, Modifier.weight(1f), onAction)
                                    SetupButton(true, Modifier.weight(1f), onAction)
                                }
                            }
                        }
                    }
                }
            }
        }
        SettingsSection.entries.forEach { section ->
            val sectionOptions = options.filter { it.section == section }
            if (sectionOptions.isNotEmpty()) item("settings-${section.name}") {
                val expanded = searching || expandedSection == section
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, if (expanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable(enabled = !searching, role = Role.Button) {
                                    expandedSection = if (expandedSection == section) null else section
                                }
                                .semantics { stateDescription = if (expanded) "Geöffnet" else "Geschlossen" }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)) {
                                Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                                    Icon(section.icon, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(section.title, modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(section.hint, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!searching) Icon(if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        if (expanded) Column(
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            sectionOptions.forEach { option ->
                                if (option == SettingsAction.SYNC_STATUS) {
                                    SettingToggleRow(option.title, showSyncStatusStrip, onShowSyncStatusStripChange)
                                } else {
                                    SettingsActionRow(option, option == SettingsAction.CHANGELOG && hasUnseenChangelog) { onAction(option) }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (options.isEmpty()) item("settings-no-results") {
            Text("Keine passende Einstellung. Suche nach Kalender, PIN oder Sicherung.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp))
        }
    }
}

@Composable
private fun SetupButton(update: Boolean, modifier: Modifier, onAction: (SettingsAction) -> Unit) {
    val content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit = {
        Icon(if (update) Icons.Outlined.Refresh else Icons.Outlined.CalendarToday, null, Modifier.size(18.dp))
        Text(if (update) "Aktualisieren" else "Verbinden", Modifier.padding(start = 6.dp))
    }
    val click = { onAction(if (update) SettingsAction.SYNC_NOW else SettingsAction.CALENDAR) }
    val buttonModifier = modifier.heightIn(min = 48.dp)
    if (update) Button(click, buttonModifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), content = content)
    else OutlinedButton(click, buttonModifier, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp), content = content)
}

@Composable
private fun SettingsActionRow(option: SettingsAction, showAlert: Boolean, onClick: () -> Unit) {
    Column {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
        Row(Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = onClick).padding(horizontal = 6.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(option.icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(option.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(option.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (showAlert) Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Text("Neu", modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp), style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
