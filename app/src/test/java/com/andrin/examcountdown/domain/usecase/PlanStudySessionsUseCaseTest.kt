package com.andrin.examcountdown.domain.usecase

import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanStudySessionsUseCaseTest {
    private val zone = ZoneId.of("Europe/Zurich")

    @Test
    fun createsSessionsWithWeekdayFilterAndTargetCount() {
        val now = ZonedDateTime.of(2026, 2, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val exam = ZonedDateTime.of(2026, 2, 25, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val useCase = PlanStudySessionsUseCase(nowProvider = { now })

        val allowedWeekdays = setOf(
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.SUNDAY
        )

        val sessions = useCase(
            PlanStudySessionsUseCase.Params(
                subject = "MAT",
                examTitle = "Pruefung 1",
                examLocation = "B411",
                examStartsAtMillis = exam,
                startWeeksBefore = 3,
                durationMinutes = 60,
                targetSessions = 10,
                weekdays = allowedWeekdays,
                startMinutesOfDay = 17 * 60,
                schoolZone = zone
            )
        )

        assertEquals(10, sessions.size)
        assertTrue(sessions.all { it.startsAtEpochMillis > now })
        assertTrue(sessions.all { it.startsAtEpochMillis < exam })
        assertTrue(
            sessions.all {
                ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.startsAtEpochMillis), zone).dayOfWeek in allowedWeekdays
            }
        )
    }

    @Test
    fun respectsThreeWeeksBeforeOffsetAsPlanningStart() {
        val now = ZonedDateTime.of(2026, 2, 1, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val examDate = ZonedDateTime.of(2026, 3, 23, 8, 0, 0, 0, zone)
        val exam = examDate.toInstant().toEpochMilli()
        val useCase = PlanStudySessionsUseCase(nowProvider = { now })

        val sessions = useCase(
            PlanStudySessionsUseCase.Params(
                subject = "DEU",
                examTitle = "Essay",
                examLocation = null,
                examStartsAtMillis = exam,
                startWeeksBefore = 3,
                durationMinutes = 45,
                targetSessions = 3,
                weekdays = setOf(DayOfWeek.MONDAY),
                startMinutesOfDay = 16 * 60,
                schoolZone = zone
            )
        )

        val firstSessionDate = ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(sessions.first().startsAtEpochMillis),
            zone
        ).toLocalDate()

        assertEquals(examDate.toLocalDate().minusWeeks(3), firstSessionDate)
        assertEquals(3, sessions.size)
    }

    @Test
    fun returnsOnlyFeasibleSessionsWhenWindowIsTooShort() {
        val now = ZonedDateTime.of(2026, 2, 20, 0, 0, 0, 0, zone).toInstant().toEpochMilli()
        val exam = ZonedDateTime.of(2026, 2, 22, 10, 0, 0, 0, zone).toInstant().toEpochMilli()
        val useCase = PlanStudySessionsUseCase(nowProvider = { now })

        val sessions = useCase(
            PlanStudySessionsUseCase.Params(
                subject = "BIO",
                examTitle = "Test",
                examLocation = null,
                examStartsAtMillis = exam,
                startWeeksBefore = 1,
                durationMinutes = 60,
                targetSessions = 10,
                weekdays = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY),
                startMinutesOfDay = 18 * 60,
                schoolZone = zone
            )
        )

        assertTrue(sessions.size < 10)
        assertTrue(sessions.isNotEmpty())
    }
}
