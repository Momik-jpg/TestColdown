package com.andrin.examcountdown.ui.tabs.events

sealed interface TimetableTabEvent {
    data object OpenIcalImport : TimetableTabEvent
    data object ClearChanges : TimetableTabEvent
}
