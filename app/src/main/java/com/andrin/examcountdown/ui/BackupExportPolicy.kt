package com.andrin.examcountdown.ui

internal enum class BackupExportMode {
    ENCRYPTED,
    UNENCRYPTED
}

internal fun shouldAllowUnencryptedExport(userConfirmed: Boolean): Boolean = userConfirmed

internal fun resolveBackupExportMode(
    userRequestedUnencrypted: Boolean,
    userConfirmedUnencryptedWarning: Boolean
): BackupExportMode {
    if (userRequestedUnencrypted && shouldAllowUnencryptedExport(userConfirmedUnencryptedWarning)) {
        return BackupExportMode.UNENCRYPTED
    }
    return BackupExportMode.ENCRYPTED
}
