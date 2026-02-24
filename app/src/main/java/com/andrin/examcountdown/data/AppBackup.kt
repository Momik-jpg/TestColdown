package com.andrin.examcountdown.data

import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableChangeEntry
import com.andrin.examcountdown.model.TimetableLesson
import kotlinx.serialization.Serializable

@Serializable
data class QuietHoursConfig(
    val enabled: Boolean = false,
    val startMinutesOfDay: Int = 22 * 60,
    val endMinutesOfDay: Int = 7 * 60
)

@Serializable
data class AppBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val exportedAtEpochMillis: Long = System.currentTimeMillis(),
    val exams: List<Exam> = emptyList(),
    val lessons: List<TimetableLesson> = emptyList(),
    val events: List<SchoolEvent> = emptyList(),
    val timetableChanges: List<TimetableChangeEntry> = emptyList(),
    val iCalUrl: String? = null,
    val importEventsEnabled: Boolean = false,
    val showTimetableTab: Boolean = true,
    val showAgendaTab: Boolean = true,
    val showExamCollisionBadges: Boolean = false,
    val onboardingDone: Boolean = false,
    val onboardingPromptSeen: Boolean = false,
    val quietHours: QuietHoursConfig = QuietHoursConfig(),
    val syncIntervalMinutes: Long = 6L * 60L,
    val showSyncStatusStrip: Boolean = true
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 6
    }
}
