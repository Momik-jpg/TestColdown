package com.andrin.examcountdown.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Use available width and the user's text size rather than a device/orientation guess. */
@Composable
internal fun AdaptiveFieldPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    minimumFieldWidth: Dp = 144.dp
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth / LocalDensity.current.fontScale < minimumFieldWidth * 2) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

/** Actual 48 dp layout bounds keep adjacent chips from sharing invisible touch padding. */
@Composable
internal fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    colors: SelectableChipColors = FilterChipDefaults.filterChipColors(
        containerColor = MaterialTheme.colorScheme.surface,
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
) {
    androidx.compose.material3.FilterChip(
        selected = selected, onClick = onClick, label = label,
        modifier = modifier.heightIn(min = 48.dp), enabled = enabled, colors = colors,
        leadingIcon = leadingIcon ?: if (selected) {
            { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) }
        } else null
    )
}

@Composable
internal fun isAppDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

@Composable
internal fun AppScreenHeading(title: String) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        Text(title, fontWeight = FontWeight.Bold,
            style = if (maxWidth / LocalDensity.current.fontScale < 280.dp) {
                MaterialTheme.typography.titleMedium
            } else MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() })
    }
}

@Composable
internal fun HomeNavigationBar(
    visibleTabs: List<HomeTab>, selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fontScale = LocalDensity.current.fontScale
        val compact = maxWidth / fontScale / visibleTabs.size.coerceAtLeast(1) < 72.dp
        val columns = if (maxWidth / fontScale < 176.dp) 2 else 3
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            shadowElevation = 3.dp
        ) {
        NavigationBar(containerColor = Color.Transparent, tonalElevation = 0.dp) {
            if (compact) {
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    visibleTabs.chunked(columns).forEach { tabs ->
                        Row(Modifier.fillMaxWidth()) {
                            tabs.forEach { tab ->
                                val selected = selectedTab == tab
                                Column(
                                    modifier = Modifier.weight(1f)
                                        .heightIn(min = 72.dp)
                                        .selectable(selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                                        .semantics { contentDescription = tab.title }
                                        .padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(
                                        shape = MaterialTheme.shapes.extraLarge,
                                        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Icon(tab.icon, null, Modifier.padding(horizontal = 12.dp, vertical = 4.dp).size(24.dp))
                                    }
                                    Text(tab.shortTitle, style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            } else {
                visibleTabs.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab, onClick = { onTabSelected(tab) },
                        modifier = Modifier.semantics { contentDescription = tab.title },
                        icon = { Icon(tab.icon, null) },
                        label = { Text(tab.shortTitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
        }
    }
}
