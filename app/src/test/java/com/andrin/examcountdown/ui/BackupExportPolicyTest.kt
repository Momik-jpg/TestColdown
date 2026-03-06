package com.andrin.examcountdown.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExportPolicyTest {

    @Test
    fun resolveBackupExportMode_defaultsToEncrypted_whenUserDidNotRequestOptOut() {
        val mode = resolveBackupExportMode(
            userRequestedUnencrypted = false,
            userConfirmedUnencryptedWarning = false
        )

        assertEquals(BackupExportMode.ENCRYPTED, mode)
    }

    @Test
    fun resolveBackupExportMode_requiresExplicitWarningConfirm_forUnencryptedExport() {
        val mode = resolveBackupExportMode(
            userRequestedUnencrypted = true,
            userConfirmedUnencryptedWarning = false
        )

        assertEquals(BackupExportMode.ENCRYPTED, mode)
    }

    @Test
    fun resolveBackupExportMode_allowsUnencrypted_afterExplicitConfirm() {
        val mode = resolveBackupExportMode(
            userRequestedUnencrypted = true,
            userConfirmedUnencryptedWarning = true
        )

        assertEquals(BackupExportMode.UNENCRYPTED, mode)
    }

    @Test
    fun shouldAllowUnencryptedExport_reflectsExplicitConfirmation() {
        assertFalse(shouldAllowUnencryptedExport(false))
        assertTrue(shouldAllowUnencryptedExport(true))
    }
}
