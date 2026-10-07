package com.andrin.examcountdown.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
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

/** Every enabled destination stays directly reachable in one row. */
@Composable
internal fun HomeNavigationBar(
    visibleTabs: List<HomeTab>, selectedTab: HomeTab, onTabSelected: (HomeTab) -> Unit
) {
    if (visibleTabs.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    val current = selectedTab.takeIf { it in visibleTabs } ?: visibleTabs.first()
    val labelStyle = MaterialTheme.typography.labelMedium
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    Surface(color = colors.surface, tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().testTag("home-navigation")) {
        Column(Modifier.windowInsetsPadding(WindowInsets.navigationBars)) {
            HorizontalDivider(color = colors.outlineVariant)
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cellWidthPx = with(density) { (maxWidth / visibleTabs.size - 8.dp).toPx() }
                // Measure actual system-scaled labels instead of shrinking or truncating them.
                val labelsFit = visibleTabs.all { tab ->
                    measurer.measure(tab.shortTitle,
                        style = labelStyle.copy(fontWeight = FontWeight.SemiBold),
                        softWrap = false).size.width <= cellWidthPx
                }
                val itemHeight = if (labelsFit) {
                    with(density) { measurer.measure("Ag", style = labelStyle).size.height.toDp() }
                        .plus(36.dp).coerceAtLeast(64.dp)
                } else 48.dp
                Column {
                    Row(Modifier.fillMaxWidth().selectableGroup()) {
                        visibleTabs.forEach { tab ->
                            val selected = current == tab
                            Column(Modifier.weight(1f).height(itemHeight)
                                .testTag("home-tab-${tab.route}")
                                .selectable(selected, role = Role.Tab, onClick = { onTabSelected(tab) })
                                .semantics { contentDescription = tab.title },
                                horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.fillMaxWidth().height(2.dp)
                                    .background(if (selected) colors.primary else Color.Transparent))
                                Column(Modifier.weight(1f).padding(horizontal = 4.dp, vertical = 4.dp),
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(tab.icon, null, Modifier.size(24.dp),
                                        tint = if (selected) colors.primary else colors.onSurfaceVariant)
                                    if (labelsFit) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(tab.shortTitle, style = labelStyle, maxLines = 1,
                                            color = if (selected) colors.primary else colors.onSurfaceVariant,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                }
                            }
                        }
                    }
                    if (!labelsFit) {
                        Text(current.title, style = labelStyle, fontWeight = FontWeight.SemiBold,
                            color = colors.primary, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                                .testTag("home-current-label"))
                    }
                }
            }
        }
    }
}
