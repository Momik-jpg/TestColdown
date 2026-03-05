package com.andrin.examcountdown.ui.tabs.state

import com.andrin.examcountdown.data.CollisionRuleSettings
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableLesson

data class ExamsTabUiState(
    val exams: List<Exam> = emptyList(),
    val lessons: List<TimetableLesson> = emptyList(),
    val events: List<SchoolEvent> = emptyList(),
    val showCollisionBadges: Boolean = false,
    val collisionRules: CollisionRuleSettings = CollisionRuleSettings(),
    val hasIcalUrl: Boolean = false,
    val hasSyncedOnce: Boolean = false,
    val lastSyncError: String? = null,
    val simpleModeEnabled: Boolean = true,
    val showSetupGuideCard: Boolean = true
)

data class TimetableTabUiState(
    val lessons: List<TimetableLesson> = emptyList(),
    val changes: List<TimetableChangeEntry> = emptyList(),
    val hasIcalUrl: Boolean = false
)

data class AgendaTabUiState(
    val exams: List<Exam> = emptyList(),
    val lessons: List<TimetableLesson> = emptyList(),
    val events: List<SchoolEvent> = emptyList(),
    val hasIcalUrl: Boolean = false,
    val importEventsEnabled: Boolean = false
)

data class GradesTabUiState(
    val preferencesLoaded: Boolean = false
)
