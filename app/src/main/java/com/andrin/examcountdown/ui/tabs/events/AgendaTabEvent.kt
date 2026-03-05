package com.andrin.examcountdown.ui.tabs.events

import com.andrin.examcountdown.model.SchoolEvent

sealed interface AgendaTabEvent {
    data object OpenIcalImport : AgendaTabEvent
    data object EnableEventsImportAndSync : AgendaTabEvent
    data class AddCustomEvents(val events: List<SchoolEvent>) : AgendaTabEvent
    data class DeleteCustomEvent(val eventId: String) : AgendaTabEvent
    data class UpdateCustomEvent(val event: SchoolEvent) : AgendaTabEvent
}
