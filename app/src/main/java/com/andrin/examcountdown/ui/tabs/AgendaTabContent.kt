package com.andrin.examcountdown.ui.tabs

import androidx.compose.runtime.Composable
import com.andrin.examcountdown.ui.EventsTimelineContent
import com.andrin.examcountdown.ui.tabs.events.AgendaTabEvent
import com.andrin.examcountdown.ui.tabs.state.AgendaTabUiState

@Composable
fun AgendaTabContent(
    state: AgendaTabUiState,
    onEvent: (AgendaTabEvent) -> Unit
) {
    EventsTimelineContent(
        exams = state.exams,
        lessons = state.lessons,
        events = state.events,
        hasIcalUrl = state.hasIcalUrl,
        importEventsEnabled = state.importEventsEnabled,
        onOpenIcalImport = { onEvent(AgendaTabEvent.OpenIcalImport) },
        onEnableEventsImportAndSync = { onEvent(AgendaTabEvent.EnableEventsImportAndSync) },
        onAddCustomEvents = { createdEvents ->
            onEvent(AgendaTabEvent.AddCustomEvents(createdEvents))
        },
        onDeleteCustomEvent = { eventId ->
            onEvent(AgendaTabEvent.DeleteCustomEvent(eventId))
        },
        onUpdateCustomEvent = { event ->
            onEvent(AgendaTabEvent.UpdateCustomEvent(event))
        }
    )
}
