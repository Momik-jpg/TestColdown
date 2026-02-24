package com.andrin.examcountdown.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
enum class SchoolEventType {
    SCHOOL,
    HOLIDAY,
    DEADLINE,
    INFO,
    OTHER
}

@Serializable
data class SchoolEvent(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val type: SchoolEventType = SchoolEventType.OTHER,
    val location: String? = null,
    val description: String? = null,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val isAllDay: Boolean = false,
    val source: String? = null
)
