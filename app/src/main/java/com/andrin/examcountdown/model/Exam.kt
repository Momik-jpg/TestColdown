package com.andrin.examcountdown.model

import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class Exam(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val location: String? = null,
    val startsAtEpochMillis: Long,
    val reminderAtEpochMillis: Long? = null,
    val reminderMinutesBefore: Long? = null,
    val reminderLeadTimesMinutes: List<Long> = emptyList()
)
