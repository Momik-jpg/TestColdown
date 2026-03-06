package com.andrin.examcountdown.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgendaControlsPolicyTest {

    @Test
    fun hasActiveAgendaFilters_isFalse_forDefaultState() {
        val result = hasActiveAgendaFilters(
            searchQuery = "",
            sourceFilterIsAll = true,
            layoutModeIsMonth = true
        )

        assertFalse(result)
    }

    @Test
    fun hasActiveAgendaFilters_isTrue_forAnyNonDefaultInput() {
        assertTrue(
            hasActiveAgendaFilters(
                searchQuery = "mathe",
                sourceFilterIsAll = true,
                layoutModeIsMonth = true
            )
        )
        assertTrue(
            hasActiveAgendaFilters(
                searchQuery = "",
                sourceFilterIsAll = false,
                layoutModeIsMonth = true
            )
        )
        assertTrue(
            hasActiveAgendaFilters(
                searchQuery = "",
                sourceFilterIsAll = true,
                layoutModeIsMonth = false
            )
        )
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
