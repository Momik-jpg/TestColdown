package com.andrin.examcountdown.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgendaControlsPolicyTest {

    @Test
    fun hasActiveAgendaFilters_isFalse_forDefaultState() {
        val result = hasActiveAgendaFilters(
            searchQuery = "",
            sourceFilterIsAll = true
        )

        assertFalse(result)
    }

    @Test
    fun hasActiveAgendaFilters_isTrue_forAnyNonDefaultInput() {
        assertTrue(
            hasActiveAgendaFilters(
                searchQuery = "mathe",
                sourceFilterIsAll = true
            )
        )
        assertTrue(
            hasActiveAgendaFilters(
                searchQuery = "",
                sourceFilterIsAll = false
            )
        )
        assertFalse(hasActiveAgendaFilters(searchQuery = "  ", sourceFilterIsAll = true))
    }

    @Test
    fun shouldShowEnableEventImportAction_onlyWhenEventsOnlyAndDisabled() {
        assertTrue(
            shouldShowEnableEventImportAction(
                sourceFilterIsEventsOnly = true,
                importEventsEnabled = false
            )
        )
        assertFalse(
            shouldShowEnableEventImportAction(
                sourceFilterIsEventsOnly = false,
                importEventsEnabled = false
            )
        )
        assertFalse(
            shouldShowEnableEventImportAction(
                sourceFilterIsEventsOnly = true,
                importEventsEnabled = true
            )
        )
    }
}
