package com.andrin.examcountdown.worker

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncSchedulingSemanticsTest {

    @Test
    fun periodicSync_usesUpdatePolicy_forIntervalAndConstraintChanges() {
        assertEquals(ExistingPeriodicWorkPolicy.UPDATE, IcalSyncScheduler.PERIODIC_POLICY)
        assertTrue(IcalSyncScheduler.periodicWorkName().isNotBlank())
    }

    @Test
    fun immediateSync_usesKeepPolicy_toDedupeBurstTriggers() {
        assertEquals(ExistingWorkPolicy.KEEP, IcalSyncScheduler.IMMEDIATE_POLICY)
        assertTrue(IcalSyncScheduler.immediateWorkName().isNotBlank())
    }

    @Test
    fun widgetLegacyPeriodicRefresh_isDisabled() {
        assertFalse(WidgetRefreshScheduler.LEGACY_PERIODIC_REFRESH_ENABLED)
        assertTrue(WidgetRefreshScheduler.workName().isNotBlank())
        assertEquals(
            WidgetRefreshExecutionMode.TRIGGER_SYNC_ONLY,
            WidgetRefreshScheduler.WORKER_EXECUTION_MODE
        )
    }
}
