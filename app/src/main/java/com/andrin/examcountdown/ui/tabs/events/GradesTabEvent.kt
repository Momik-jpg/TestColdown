package com.andrin.examcountdown.ui.tabs.events

sealed interface GradesTabEvent {
    data object NoOp : GradesTabEvent
}
