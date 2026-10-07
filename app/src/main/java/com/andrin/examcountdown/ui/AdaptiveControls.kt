package com.andrin.examcountdown.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
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

/** A single compact dock; destinations open vertically only when requested. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeNavigationBar(
    visibleTabs: List<HomeTab>, selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit
) {
    if (visibleTabs.isEmpty()) return
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val current = selectedTab.takeIf { it in visibleTabs } ?: visibleTabs.first()
    Surface(color = colors.surface, tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().testTag("home-navigation")) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = colors.outlineVariant)
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.weight(1f).heightIn(min = 48.dp)
                    .selectable(true, role = Role.Tab, onClick = { menuOpen = true })
                    .semantics { contentDescription = current.title },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(current.icon, null, Modifier.size(22.dp), tint = colors.tertiary)
                    Text(current.shortTitle, style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold, color = colors.onSurface)
                }
                TextButton(onClick = { menuOpen = true },
                    modifier = Modifier.heightIn(min = 48.dp)
                        .semantics { contentDescription = "Bereiche öffnen" },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)) {
                    Icon(Icons.Outlined.Menu, null, Modifier.size(20.dp))
                    Text("Menü", Modifier.padding(start = 8.dp))
                }
            }
        }
    }
    if (menuOpen) {
        ModalBottomSheet(onDismissRequest = { menuOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.surface, contentColor = colors.onSurface,
            shape = MaterialTheme.shapes.extraLarge, dragHandle = null,
            modifier = Modifier.testTag("home-menu")) {
            Column(Modifier.fillMaxWidth()
                .heightIn(max = LocalConfiguration.current.screenHeightDp.dp * 0.7f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Bereiche", style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    IconButton(onClick = { menuOpen = false }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Menü schliessen")
                    }
                }
                Column(Modifier.fillMaxWidth().selectableGroup()) {
                    visibleTabs.forEach { tab ->
                        val selected = current == tab
                        Surface(color = if (selected) colors.primaryContainer else colors.surface,
                            shape = MaterialTheme.shapes.small) {
                            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
                                .testTag("home-menu-${tab.route}")
                                .selectable(selected, role = Role.Tab, onClick = {
                                    menuOpen = false
                                    onTabSelected(tab)
                                }).semantics { contentDescription = tab.title }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                Icon(tab.icon, null, Modifier.size(24.dp),
                                    tint = if (selected) colors.tertiary else colors.onSurfaceVariant)
                                Text(tab.title, style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f))
                                if (selected) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
