package com.andrin.examcountdown.ui.tabs.events

import com.andrin.examcountdown.model.Exam

sealed interface ExamsTabEvent {
    data object OpenIcalImport : ExamsTabEvent
    data object RefreshNow : ExamsTabEvent
    data object OpenHelp : ExamsTabEvent
    data object OpenSyncDiagnostics : ExamsTabEvent
    data object HideSetupGuide : ExamsTabEvent
    data object AddExam : ExamsTabEvent
    data class PlanStudy(val exam: Exam) : ExamsTabEvent
    data class DeleteExam(val exam: Exam) : ExamsTabEvent
}
