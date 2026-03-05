package com.andrin.examcountdown.domain.usecase

import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.SchoolEventType
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.util.CollisionRules
import com.andrin.examcountdown.util.CollisionSource
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectScheduleCollisionsUseCaseTest {
    private val zone = ZoneId.of("Europe/Zurich")

    @Test
    fun groupsDetectedCollisionsByExamId() {
        val examStart = LocalDate.of(2026, 3, 3).atTime(10, 0).atZone(zone).toInstant().toEpochMilli()
        val exam = Exam(id = "exam-1", title = "Staatskunde", startsAtEpochMillis = examStart)
        val lesson = TimetableLesson(
            id = "lesson-1",
            title = "MAT · I24B",
            startsAtEpochMillis = LocalDate.of(2026, 3, 3).atTime(9, 45).atZone(zone).toInstant().toEpochMilli(),
            endsAtEpochMillis = LocalDate.of(2026, 3, 3).atTime(10, 30).atZone(zone).toInstant().toEpochMilli()
        )
        val event = SchoolEvent(
            id = "event-1",
            title = "Sporttag",
            type = SchoolEventType.SCHOOL,
            startsAtEpochMillis = LocalDate.of(2026, 3, 3).atStartOfDay(zone).toInstant().toEpochMilli(),
            endsAtEpochMillis = LocalDate.of(2026, 3, 4).atStartOfDay(zone).toInstant().toEpochMilli(),
            isAllDay = true
        )

        val result = DetectScheduleCollisionsUseCase().invoke(
            DetectScheduleCollisionsUseCase.Params(
                exams = listOf(exam),
                lessons = listOf(lesson),
                events = listOf(event),
                rules = CollisionRules(),
                zoneId = zone
            )
        )

        assertEquals(2, result.collisions.size)
        assertEquals(2, result.byExam["exam-1"]?.size)
        assertTrue(result.collisions.any { it.source == CollisionSource.LESSON })
        assertTrue(result.collisions.any { it.source == CollisionSource.EVENT })
    }

    @Test
    fun onlyDifferentSubjectRuleSuppressesSameSubjectLessonCollision() {
        val examStart = LocalDate.of(2026, 3, 4).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val exam = Exam(id = "exam-2", title = "Mathematik", subject = "MAT", startsAtEpochMillis = examStart)
        val lesson = TimetableLesson(
            id = "lesson-2",
            title = "MAT · I24B · HeiCa",
            startsAtEpochMillis = LocalDate.of(2026, 3, 4).atTime(11, 45).atZone(zone).toInstant().toEpochMilli(),
            endsAtEpochMillis = LocalDate.of(2026, 3, 4).atTime(12, 30).atZone(zone).toInstant().toEpochMilli()
        )

        val defaultRules = DetectScheduleCollisionsUseCase().invoke(
            DetectScheduleCollisionsUseCase.Params(
                exams = listOf(exam),
                lessons = listOf(lesson),
                events = emptyList(),
                rules = CollisionRules(onlyDifferentSubject = true),
                zoneId = zone
            )
        )
        val strictRules = DetectScheduleCollisionsUseCase().invoke(
            DetectScheduleCollisionsUseCase.Params(
                exams = listOf(exam),
                lessons = listOf(lesson),
                events = emptyList(),
                rules = CollisionRules(onlyDifferentSubject = false),
                zoneId = zone
            )
        )

        assertTrue(defaultRules.collisions.isEmpty())
        assertEquals(1, strictRules.collisions.size)
    }

    @Test
    fun exactTimeOverlapDisablesAllDayEventCollisions() {
        val examStart = LocalDate.of(2026, 3, 5).atTime(13, 0).atZone(zone).toInstant().toEpochMilli()
        val exam = Exam(id = "exam-3", title = "Deutsch", startsAtEpochMillis = examStart)
        val holiday = SchoolEvent(
            id = "event-3",
            title = "Ferien",
            type = SchoolEventType.HOLIDAY,
            startsAtEpochMillis = LocalDate.of(2026, 3, 5).atStartOfDay(zone).toInstant().toEpochMilli(),
            endsAtEpochMillis = LocalDate.of(2026, 3, 6).atStartOfDay(zone).toInstant().toEpochMilli(),
            isAllDay = true
        )

        val result = DetectScheduleCollisionsUseCase().invoke(
            DetectScheduleCollisionsUseCase.Params(
                exams = listOf(exam),
                lessons = emptyList(),
                events = listOf(holiday),
                rules = CollisionRules(requireExactTimeOverlap = true),
                zoneId = zone
            )
        )

        assertTrue(result.collisions.isEmpty())
        assertTrue(result.byExam.isEmpty())
    }
}
