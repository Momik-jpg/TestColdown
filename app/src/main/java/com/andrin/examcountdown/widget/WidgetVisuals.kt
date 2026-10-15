package com.andrin.examcountdown.widget

import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** The same RemoteViews as the installed widget; preview actions never open the app. */
@Composable
internal fun WidgetLivePreview(isList: Boolean, config: WidgetConfig, modifier: Modifier = Modifier) {
    val now = remember { System.currentTimeMillis() }
    val items = remember(now, config.mode) {
        if (config.mode == WidgetMode.EXAMS) listOf(
            WidgetTimelineItem("example-math", "Mathematik · Funktionen", now + 172_800_000, now + 172_800_000, WidgetItemKind.EXAM, "Raum 204"),
            WidgetTimelineItem("example-german", "Deutsch · Literatur", now + 259_200_000, now + 259_200_000, WidgetItemKind.EXAM, "Aula")
        ) else listOf(
            WidgetTimelineItem("example-english", "Englisch", now + 3_600_000, now + 6_300_000, WidgetItemKind.LESSON, "Raum 102"),
            WidgetTimelineItem("example-math", "Mathematik · Funktionen", now + 172_800_000, now + 172_800_000, WidgetItemKind.EXAM, "Raum 204"),
            WidgetTimelineItem("example-project", "Projektabgabe", now + 259_200_000, now + 259_200_000, WidgetItemKind.EVENT, "Online")
        )
    }
    val height = if (isList) 264 else 240
    val width = (LocalConfiguration.current.screenWidthDp - 32).coerceAtLeast(220)
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    AndroidView(
        modifier = modifier.fillMaxWidth().height(height.dp).testTag("widget-live-preview")
            .semantics { contentDescription = "Widget-Vorschau mit Beispieldaten. ${widgetHeaderLabel(config)}" },
        factory = { FrameLayout(it) },
        update = { host ->
            val resources = Configuration(host.context.resources.configuration).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            val context = host.context.createConfigurationContext(resources)
            val options = Bundle().apply {
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, height)
                putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, width)
            }
            val sorted = selectWidgetItems(items, config, now, 3, nextOnly = !isList)
            val views = if (isList) WidgetPresentation.list(context, 0, config, sorted, options, now)
                else WidgetPresentation.next(context, 0, config, sorted.firstOrNull(), options, now)
            val view = views.apply(context, host)
            disablePreviewActions(view)
            view.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            host.removeAllViews()
            host.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
    )
}

private fun disablePreviewActions(view: View) {
    view.setOnClickListener(null)
    view.isClickable = false
    view.isFocusable = false
    if (view is ViewGroup) for (index in 0 until view.childCount) disablePreviewActions(view.getChildAt(index))
}

@Composable
internal fun WidgetSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

@Composable
internal fun WidgetChoice(title: String, hint: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier.selectable(selected, role = Role.RadioButton, onClick = onClick), shape = MaterialTheme.shapes.medium,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Icon(icon, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Icon(if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                    null, Modifier.size(20.dp), tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun WidgetAppearanceToggle(title: String, hint: String, icon: ImageVector, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).toggleable(checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)) {
            Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked, onCheckedChange = null)
    }
}
