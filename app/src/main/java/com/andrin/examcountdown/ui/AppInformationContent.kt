package com.andrin.examcountdown.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal enum class AppInformationPage(val title: String, val asset: String) {
    PRIVACY("Datenschutzhinweise", "privacy.md"),
    ACCESSIBILITY("Barrierefreie Bedienung", "accessibility.md"),
    LICENSES("Lizenzen & Bildnachweise", "licenses.md")
}

@Composable
internal fun AppInformationDialog(page: AppInformationPage, onDismiss: () -> Unit, onPersonalize: () -> Unit) {
    val context = LocalContext.current
    var document by remember(page) { mutableStateOf("Hinweise werden geladen …") }
    LaunchedEffect(page) {
        document = withContext(Dispatchers.IO) {
            runCatching { context.assets.open("legal/${page.asset}").bufferedReader().use { it.readText() } }
                .getOrElse { "Hinweise konnten nicht geladen werden. Bitte öffne die Dokumentation im Projekt TestColdown auf GitHub." }
        }
    }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        AppInformationContent(page, document, onDismiss, onPersonalize)
    }
}

/** Paragraphs are individually selectable and lazily laid out, including long dependency notices. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppInformationContent(page: AppInformationPage, document: String, onDismiss: () -> Unit, onPersonalize: () -> Unit) {
    val paragraphs = remember(document) { document.split(Regex("\\n\\s*\\n")).filter { it.isNotBlank() } }
    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        TopAppBar(title = { AppScreenHeading(page.title) }, navigationIcon = {
            IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Hinweise schließen") }
        })
    }) { insets ->
        LazyColumn(Modifier.fillMaxSize().padding(insets),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            if (page == AppInformationPage.ACCESSIBILITY) item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Passe Schrift, Kontrast und Startansicht an deine Bedürfnisse an.", style = MaterialTheme.typography.bodyLarge)
                    Button(onClick = onPersonalize, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                        Text("Ansicht & Bedienung anpassen")
                    }
                }
            }
            itemsIndexed(paragraphs) { _, paragraph ->
                val isHeading = paragraph.startsWith("#")
                SelectionContainer {
                    Text(if (isHeading) paragraph.trimStart('#', ' ') else paragraph,
                        modifier = Modifier.fillMaxWidth().then(if (isHeading) Modifier.semantics { heading() } else Modifier),
                        style = if (isHeading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}
