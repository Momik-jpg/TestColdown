package com.andrin.examcountdown.util

import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.SchoolEventType
import com.andrin.examcountdown.model.TimetableLesson
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleCollisionDetectorTest {
    private val zone = ZoneId.of("Europe/Zurich")

    @Test
    fun detectsLessonAndAllDayEventCollisionForExam() {
        val examStart = LocalDate.now(zone).plusDays(2)
            .atTime(10, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val lessonStart = LocalDate.now(zone).plusDays(2)
            .atTime(9, 45)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val lessonEnd = LocalDate.now(zone).plusDays(2)
            .atTime(10, 30)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val allDayStart = LocalDate.now(zone).plusDays(2)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
        val allDayEnd = LocalDate.now(zone).plusDays(3)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()

        val exam = Exam(
            id = "exam-1",
            title = "Mathematik Prüfung",
            startsAtEpochMillis = examStart
        )
        val lesson = TimetableLesson(
            id = "lesson-1",
            title = "MAT · L24B · HeiCa",
            startsAtEpochMillis = lessonStart,
            endsAtEpochMillis = lessonEnd
        )
        val event = SchoolEvent(
            id = "event-1",
            title = "Sporttag",
            type = SchoolEventType.SCHOOL,
            startsAtEpochMillis = allDayStart,
            endsAtEpochMillis = allDayEnd,
            isAllDay = true
        )

        val collisions = detectExamCollisions(
            exams = listOf(exam),
            lessons = listOf(lesson),
            events = listOf(event),
            zoneId = zone
        )

        assertEquals(2, collisions.size)
        assertTrue(collisions.any { it.source == CollisionSource.LESSON })
        assertTrue(collisions.any { it.source == CollisionSource.EVENT })
    }

    @Test
    fun ignoresNonOverlappingEntries() {
        val examStart = LocalDate.now(zone).plusDays(2)
            .atTime(14, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val lessonStart = LocalDate.now(zone).plusDays(2)
            .atTime(8, 0)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
        val lessonEnd = LocalDate.now(zone).plusDays(2)
            .atTime(8, 45)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()

        val collisions = detectExamCollisions(
            exams = listOf(
                Exam(
                    id = "exam-2",
                    title = "Englisch",
                    startsAtEpochMillis = examStart
                )
            ),
            lessons = listOf(
                TimetableLesson(
                    id = "lesson-2",
                    title = "ENG · L24B",
                    startsAtEpochMillis = lessonStart,
                    endsAtEpochMillis = lessonEnd
                )
            ),
            events = emptyList(),
            zoneId = zone
        )

        assertTrue(collisions.isEmpty())
    }
}
