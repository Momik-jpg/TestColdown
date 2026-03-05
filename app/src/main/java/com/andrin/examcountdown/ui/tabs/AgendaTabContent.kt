package com.andrin.examcountdown.ui.tabs

import androidx.compose.runtime.Composable
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.ui.EventsTimelineContent
import com.andrin.examcountdown.ui.tabs.state.AgendaTabUiState

@Composable
fun AgendaTabContent(
    state: AgendaTabUiState,
    onOpenIcalImport: () -> Unit,
    onEnableEventsImportAndSync: () -> Unit,
    onAddCustomEvents: (List<SchoolEvent>) -> Unit,
    onDeleteCustomEvent: (String) -> Unit,
    onUpdateCustomEvent: (SchoolEvent) -> Unit
) {
    EventsTimelineContent(
        exams = state.exams,
        lessons = state.lessons,
        events = state.events,
        hasIcalUrl = state.hasIcalUrl,
        importEventsEnabled = state.importEventsEnabled,
        onOpenIcalImport = onOpenIcalImport,
        onEnableEventsImportAndSync = onEnableEventsImportAndSync,
        onAddCustomEvents = onAddCustomEvents,
        onDeleteCustomEvent = onDeleteCustomEvent,
        onUpdateCustomEvent = onUpdateCustomEvent
    )
}
