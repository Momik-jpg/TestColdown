package com.andrin.examcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.andrin.examcountdown.ui.theme.ExamCountdownTheme

internal enum class WidgetKind(val title: String, val hint: String, val provider: Class<*>) {
    NEXT("Nächster Eintrag", "Prüfung oder Termin mit Countdown", NextExamWidgetProvider::class.java),
    LIST("Terminliste", "Prüfungen, Unterricht und Termine im Überblick", ExamListWidgetProvider::class.java)
}

internal data class InstalledWidget(val id: Int, val kind: WidgetKind, val config: WidgetConfig)

class WidgetSettingsActivity : ComponentActivity() {
    private var widgets by mutableStateOf(emptyList<InstalledWidget>())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val manager = AppWidgetManager.getInstance(this)
        setContent {
            ExamCountdownTheme {
                Surface(Modifier.fillMaxSize()) {
                    WidgetSettingsScreen(widgets, manager.isRequestPinAppWidgetSupported,
                        onAdd = { kind ->
                            if (!manager.requestPinAppWidget(ComponentName(this, kind.provider), null, null)) {
                                Toast.makeText(this, "Über den Startbildschirm hinzufügen: lange drücken → Widgets.", Toast.LENGTH_LONG).show()
                            }
                        },
                        onConfigure = { id -> startActivity(Intent(this, WidgetConfigActivity::class.java)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)) },
                        onBack = { finish() })
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val manager = AppWidgetManager.getInstance(this)
        widgets = WidgetKind.entries.flatMap { kind ->
            manager.getAppWidgetIds(ComponentName(this, kind.provider)).map { id ->
                InstalledWidget(id, kind, WidgetPreferences.readConfig(this, id))
            }
        }
    }
}

@Composable
internal fun WidgetSettingsScreen(widgets: List<InstalledWidget>, canPin: Boolean, onAdd: (WidgetKind) -> Unit, onConfigure: (Int) -> Unit, onBack: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize().testTag("widget-settings-list"), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Widgets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                OutlinedButton(onClick = onBack) { Text("Zurück") }
            }
            Text("Dein Tag. Direkt auf dem Startbildschirm.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Zum Vergrößern das Widget lange drücken und die Ränder ziehen. Hohe Widgets zeigen weitere Einträge.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp))
        }
        WidgetKind.entries.forEach { kind -> item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Box(Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                            Icon(if (kind == WidgetKind.NEXT) Icons.Outlined.Timer else Icons.Outlined.CalendarToday,
                                null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(kind.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(kind.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                WidgetLivePreview(kind == WidgetKind.LIST, WidgetConfig(mode = if (kind == WidgetKind.LIST) WidgetMode.AGENDA else WidgetMode.EXAMS))
                if (canPin) Button(onClick = { onAdd(kind) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                    Text("${kind.title} hinzufügen", modifier = Modifier.padding(start = 8.dp))
                }
            }
        } }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Tune, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Text(if (canPin) "Nach dem Hinzufügen über das Zahnrad einstellen." else
                        "Zum Hinzufügen auf dem Startbildschirm lange drücken → Widgets → Prüfungs-Countdown.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("Deine Widgets · ${widgets.size}", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 20.dp))
            if (widgets.isEmpty()) Text("Noch kein Widget hinzugefügt", color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
        }
        widgets.forEachIndexed { index, widget -> item(key = widget.id) {
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${widget.kind.title} · ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(widgetHeaderLabel(widget.config), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { onConfigure(widget.id) }, modifier = Modifier.testTag("edit-widget-${widget.id}")) { Text("Einstellen") }
                }
            }
        } }
    }
}
