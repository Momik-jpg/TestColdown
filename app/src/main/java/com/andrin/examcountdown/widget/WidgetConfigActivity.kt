package com.andrin.examcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import com.andrin.examcountdown.ui.SettingToggleRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme

class WidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val widgetId = intent?.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        // Launchers must be able to open this activity; reject unrelated or stale widget IDs.
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID || !isOwnWidget(this, widgetId)) {
            finish()
            return
        }

        setResult(
            RESULT_CANCELED,
            Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        )

        val initial = WidgetPreferences.readConfig(this, widgetId)
        setContent {
            ExamCountdownTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WidgetConfigScreen(
                        initialConfig = initial,
                        isList = AppWidgetManager.getInstance(this).getAppWidgetInfo(widgetId)?.provider?.className == ExamListWidgetProvider::class.java.name,
                        onSave = { config ->
                            WidgetPreferences.saveConfig(this, widgetId, config)
                            WidgetUpdater.updateAll(this)
                            setResult(
                                RESULT_OK,
                                Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                            )
                            finish()
                        },
                        onCancel = { finish() }
                    )
                }
            }
        }
    }
}

internal fun isOwnWidget(context: Context, widgetId: Int): Boolean {
    val provider = AppWidgetManager.getInstance(context).getAppWidgetInfo(widgetId)?.provider ?: return false
    return WidgetKind.entries.any { provider == ComponentName(context, it.provider) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetConfigScreen(
    initialConfig: WidgetConfig,
    isList: Boolean,
    onSave: (WidgetConfig) -> Unit,
    onCancel: () -> Unit
) {
    var mode by rememberSaveable { mutableStateOf(initialConfig.mode) }
    var windowDays by rememberSaveable { mutableIntStateOf(initialConfig.windowDays) }
    var sortMode by rememberSaveable { mutableStateOf(initialConfig.sortMode) }
    var compact by rememberSaveable { mutableStateOf(initialConfig.compact) }
    var showLocation by rememberSaveable { mutableStateOf(initialConfig.showLocation) }
    var showCountdown by rememberSaveable { mutableStateOf(initialConfig.showCountdown) }
    val config = WidgetConfig(mode, windowDays, sortMode, compact, showLocation, showCountdown)

    Scaffold(bottomBar = {
        Surface(tonalElevation = 3.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Abbrechen") }
                Button(onClick = { onSave(config) }, modifier = Modifier.weight(1f)) { Text("Speichern") }
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Text("Widget einstellen", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(if (isList) "Terminliste" else "Nächster Eintrag", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                WidgetConfigCard("Vorschau · Beispiel") {
                    Text(if (mode == WidgetMode.EXAMS) "Mathematik · Funktionen" else "Englisch", style = MaterialTheme.typography.titleMedium)
                    Text("Mo · 08:00" + if (showLocation) " · Raum 204" else "", style = MaterialTheme.typography.bodySmall)
                    if (showCountdown) Text("in 2 Tagen", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                    if (isList && !compact) {
                        Text("${if (mode == WidgetMode.EXAMS) "Deutsch · Literatur" else "Projektabgabe"} · Di 10:15", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                WidgetConfigCard("Inhalt & Zeitraum") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(mode == WidgetMode.EXAMS, { mode = WidgetMode.EXAMS }, label = { Text("Nur Prüfungen") })
                        FilterChip(mode == WidgetMode.AGENDA, { mode = WidgetMode.AGENDA }, label = { Text("Agenda") })
                    }
                    Text("Agenda enthält auch Unterricht und Termine.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf(7, 30, 90, WIDGET_WINDOW_DAYS_ALL) + windowDays).distinct().sorted().forEach { days ->
                            FilterChip(windowDays == days, { windowDays = days }, label = { Text(if (days == WIDGET_WINDOW_DAYS_ALL) "Alle" else "$days Tage") })
                        }
                    }
                }
            }
            if (isList) item {
                WidgetConfigCard("Sortierung") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(sortMode == WidgetSortMode.TIME_ASC, { sortMode = WidgetSortMode.TIME_ASC }, label = { Text("Nach Zeit") })
                        FilterChip(sortMode == WidgetSortMode.TYPE_THEN_TIME, { sortMode = WidgetSortMode.TYPE_THEN_TIME }, label = { Text("Nach Typ") })
                    }
                }
            }
            item {
                WidgetConfigCard("Darstellung") {
                    SettingToggleRow("Kompakte Ansicht", compact, { compact = it })
                    SettingToggleRow("Raum anzeigen", showLocation, { showLocation = it })
                    SettingToggleRow("Countdown anzeigen", showCountdown, { showCountdown = it })
                    Text("Helligkeit folgt dem Handy. Größere Listen zeigen mehr Einträge.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun WidgetConfigCard(title: String, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}
