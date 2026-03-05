package com.andrin.examcountdown.domain.usecase

import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.SchoolEventType
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/**
 * Builds manual study sessions before an exam using weekday, duration and planning window rules.
 * The use case is pure except for the injected current-time provider.
 */
class PlanStudySessionsUseCase(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    data class Params(
        val subject: String?,
        val examTitle: String,
        val examLocation: String?,
        val examStartsAtMillis: Long,
        val startWeeksBefore: Int,
        val durationMinutes: Int,
        val targetSessions: Int,
        val weekdays: Set<DayOfWeek>,
        val startMinutesOfDay: Int,
        val schoolZone: ZoneId
    )

    operator fun invoke(params: Params): List<SchoolEvent> {
        if (
            params.startWeeksBefore <= 0 ||
            params.durationMinutes <= 0 ||
            params.targetSessions <= 0 ||
            params.weekdays.isEmpty()
        ) {
            return emptyList()
        }

        val nowMillis = nowProvider()
        val examStart = Instant.ofEpochMilli(params.examStartsAtMillis).atZone(params.schoolZone)
        val examDate = examStart.toLocalDate()
        val firstDateByRule = examDate.minusWeeks(params.startWeeksBefore.toLong())
        val todayDate = Instant.ofEpochMilli(nowMillis).atZone(params.schoolZone).toLocalDate()
        val startDate = if (firstDateByRule.isBefore(todayDate)) todayDate else firstDateByRule
        val lastDate = examDate.minusDays(1)
        if (lastDate.isBefore(startDate)) return emptyList()

        val hour = (params.startMinutesOfDay / 60).coerceIn(0, 23)
        val minute = (params.startMinutesOfDay % 60).coerceIn(0, 59)
        val baseTitle = buildString {
            append("Lernen")
            params.subject.orEmpty().trim().takeIf { it.isNotBlank() }?.let {
                append(" $it")
            }
            append(": ${params.examTitle.trim()}")
        }
        val safeLocation = params.examLocation.orEmpty().trim().takeIf { it.isNotBlank() }
        val seed = nowProvider()
        val maxCount = params.targetSessions.coerceIn(1, 400)

        val sessions = mutableListOf<SchoolEvent>()
        var currentDate = startDate
        var index = 0
        while (!currentDate.isAfter(lastDate) && index < 500 && sessions.size < maxCount) {
            if (currentDate.dayOfWeek in params.weekdays) {
                val sessionStart = currentDate
                    .atTime(hour, minute)
                    .atZone(params.schoolZone)
                val startsAtMillis = sessionStart.toInstant().toEpochMilli()
                if (startsAtMillis > nowMillis && startsAtMillis < params.examStartsAtMillis) {
                    sessions += SchoolEvent(
                        id = "manual-study:$seed:$index",
                        title = baseTitle,
                        type = SchoolEventType.INFO,
                        location = safeLocation,
                        description = "Lern-Session für ${params.examTitle.trim()}",
                        startsAtEpochMillis = startsAtMillis,
                        endsAtEpochMillis = sessionStart.plusMinutes(params.durationMinutes.toLong()).toInstant().toEpochMilli(),
                        isAllDay = false,
                        source = "manual"
                    )
                }
            }
            currentDate = currentDate.plusDays(1)
            index += 1
        }
        return sessions
    }
}
