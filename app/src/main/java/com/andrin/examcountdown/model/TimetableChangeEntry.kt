package com.andrin.examcountdown.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
enum class TimetableChangeType {
    TIME_CHANGED,
    ROOM_CHANGED,
    MOVED,
    ADDED,
    REMOVED
}

@Serializable
data class TimetableChangeEntry(
    val id: String = UUID.randomUUID().toString(),
    val lessonId: String,
    val title: String,
    val startsAtEpochMillis: Long,
    val changeType: TimetableChangeType,
    val oldValue: String? = null,
    val newValue: String? = null,
    val changedAtEpochMillis: Long = System.currentTimeMillis()
)
