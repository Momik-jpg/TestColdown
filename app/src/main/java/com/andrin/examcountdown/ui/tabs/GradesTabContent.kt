package com.andrin.examcountdown.ui.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.andrin.examcountdown.ui.GradeCalculatorScreen
import com.andrin.examcountdown.ui.tabs.state.GradesTabUiState

@Composable
@Suppress("UNUSED_PARAMETER")
fun GradesTabContent(
    state: GradesTabUiState,
    modifier: Modifier = Modifier
) {
    GradeCalculatorScreen(modifier = modifier)
}
