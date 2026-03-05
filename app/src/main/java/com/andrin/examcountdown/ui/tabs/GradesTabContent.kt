package com.andrin.examcountdown.ui.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andrin.examcountdown.ui.GradeCalculatorScreen
import com.andrin.examcountdown.ui.tabs.events.GradesTabEvent
import com.andrin.examcountdown.ui.tabs.state.GradesTabUiState

@Composable
fun GradesTabContent(
    state: GradesTabUiState,
    onEvent: (GradesTabEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    @Suppress("UNUSED_VARIABLE")
    val ignored = onEvent
    @Suppress("UNUSED_VARIABLE")
    val ignoredState = state
    GradeCalculatorScreen(modifier = modifier)
}
