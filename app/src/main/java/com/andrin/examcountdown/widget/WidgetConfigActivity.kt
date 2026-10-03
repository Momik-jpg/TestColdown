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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.ViewCompact
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import com.andrin.examcountdown.ui.AppFilterChip as FilterChip
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
    var previewExpanded by rememberSaveable { mutableStateOf(true) }
    val config = WidgetConfig(mode, windowDays, sortMode, compact, showLocation, showCountdown)

    Scaffold(bottomBar = {
        Surface(shadowElevation = 6.dp) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                com.andrin.examcountdown.ui.AdaptiveFieldPair(
                    first = { OutlinedButton(onClick = onCancel, modifier = it.heightIn(min = 48.dp)) { Text("Abbrechen", maxLines = 1) } },
                    second = { Button(onClick = { onSave(config) }, modifier = it.heightIn(min = 48.dp)) { Text("Speichern", maxLines = 1) } }
                )
            }
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).testTag("widget-config-list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                com.andrin.examcountdown.ui.AppScreenHeading("Widget einstellen")
                Text(if (isList) "Deine Terminliste" else "Dein nächster Eintrag", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    com.andrin.examcountdown.ui.AdaptiveFieldPair(
                        first = { Text("Vorschau · Beispiel", modifier = it, style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant) },
                        second = { androidx.compose.material3.TextButton(onClick = { previewExpanded = !previewExpanded },
                            modifier = it.heightIn(min = 48.dp)) {
                            Text(if (previewExpanded) "Ausblenden" else "Anzeigen", maxLines = 1)
                        } }
                    )
                    if (previewExpanded) WidgetLivePreview(isList, config)
                }
            }
            item {
                WidgetSection("Inhalt", Icons.Outlined.CalendarToday) {
                    Column(Modifier.selectableGroup()) {
                        com.andrin.examcountdown.ui.AdaptiveFieldPair(
                            first = { WidgetChoice("Nur Prüfungen", "Fokus auf Prüfungen", Icons.Outlined.School,
                                mode == WidgetMode.EXAMS, { mode = WidgetMode.EXAMS }, it) },
                            second = { WidgetChoice("Agenda", "Dein gesamter Plan", Icons.Outlined.CalendarToday,
                                mode == WidgetMode.AGENDA, { mode = WidgetMode.AGENDA }, it) }
                        )
                    }
                }
            }
            item {
                WidgetSection("Zeitraum", Icons.Outlined.Schedule) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (listOf(7, 30, 90, WIDGET_WINDOW_DAYS_ALL) + windowDays).distinct().sorted().forEach { days ->
                            FilterChip(windowDays == days, { windowDays = days }, label = { Text(if (days == WIDGET_WINDOW_DAYS_ALL) "Alle" else "$days Tage") })
                        }
                    }
                }
            }
            if (isList) item {
                WidgetSection("Sortierung", Icons.AutoMirrored.Outlined.Sort) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(sortMode == WidgetSortMode.TIME_ASC, { sortMode = WidgetSortMode.TIME_ASC }, label = { Text("Nach Zeit") })
                        FilterChip(sortMode == WidgetSortMode.TYPE_THEN_TIME, { sortMode = WidgetSortMode.TYPE_THEN_TIME }, label = { Text("Nach Typ") })
                    }
                }
            }
            item {
                WidgetSection("Darstellung", Icons.Outlined.Tune) {
                    WidgetAppearanceToggle("Kompakte Ansicht", "Weniger Höhe", Icons.Outlined.ViewCompact,
                        compact, { compact = it })
                    WidgetAppearanceToggle("Raum anzeigen", "Ort im Blick", Icons.Outlined.LocationOn,
                        showLocation, { showLocation = it })
                    WidgetAppearanceToggle("Countdown anzeigen", "Zeit bis zum Start", Icons.Outlined.Timer,
                        showCountdown, { showCountdown = it })
                }
            }
        }
    }
}
