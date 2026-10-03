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
import androidx.compose.material3.Card
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
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Widgets", style = MaterialTheme.typography.headlineSmall)
                OutlinedButton(onClick = onBack) { Text("Zurück") }
            }
            Text("Dein Plan auf dem Startbildschirm", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        WidgetKind.entries.forEach { kind -> item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(kind.title, style = MaterialTheme.typography.titleMedium)
                    Text(kind.hint, style = MaterialTheme.typography.bodyMedium)
                    if (canPin) Button(onClick = { onAdd(kind) }) { Text("${kind.title} hinzufügen") }
                }
            }
        } }
        item {
            Text("Nach dem Hinzufügen: Zahnrad im Widget antippen. Inhalt, Zeitraum und Darstellung lassen sich je Widget einstellen.", style = MaterialTheme.typography.bodyMedium)
            if (!canPin) Text("Zum Hinzufügen auf dem Startbildschirm lange drücken → Widgets → Prüfungs-Countdown.", style = MaterialTheme.typography.bodyMedium)
            Text("Deine Widgets · ${widgets.size}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
            if (widgets.isEmpty()) Text("Noch kein Widget hinzugefügt", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        widgets.forEach { widget -> item(key = widget.id) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${widget.kind.title} · #${widget.id}", style = MaterialTheme.typography.titleSmall)
                    Text(widgetHeaderLabel(widget.config), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { onConfigure(widget.id) }) { Text("Widget #${widget.id} einstellen") }
                }
            }
        } }
    }
}
