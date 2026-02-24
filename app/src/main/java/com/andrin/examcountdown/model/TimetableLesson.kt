package com.andrin.examcountdown.model

import kotlinx.serialization.Serializable

@Serializable
data class TimetableLesson(
    val id: String,
    val title: String,
    val location: String? = null,
    val startsAtEpochMillis: Long,
    val endsAtEpochMillis: Long,
    val isMoved: Boolean = false,
    val isCancelledSlot: Boolean = false,
    val originalStartsAtEpochMillis: Long? = null,
    val originalEndsAtEpochMillis: Long? = null
)
