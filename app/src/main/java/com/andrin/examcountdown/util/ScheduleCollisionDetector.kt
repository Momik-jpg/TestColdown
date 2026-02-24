package com.andrin.examcountdown.util

import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import java.time.Instant
import java.time.ZoneId

enum class CollisionSource {
    LESSON,
    EVENT
}

data class ExamCollision(
    val examId: String,
    val examTitle: String,
    val examStartsAtEpochMillis: Long,
    val source: CollisionSource,
    val sourceId: String,
    val sourceTitle: String,
    val sourceStartsAtEpochMillis: Long,
    val sourceEndsAtEpochMillis: Long,
    val sourceIsAllDay: Boolean
)

fun detectExamCollisions(
    exams: List<Exam>,
    lessons: List<TimetableLesson>,
    events: List<SchoolEvent>,
    zoneId: ZoneId = ZoneId.of("Europe/Zurich")
): List<ExamCollision> {
    if (exams.isEmpty()) return emptyList()

    val collisions = mutableListOf<ExamCollision>()

    exams.forEach { exam ->
        val examAt = exam.startsAtEpochMillis
        if (examAt <= 0L) return@forEach

        lessons.forEach { lesson ->
            val hasValidRange = lesson.endsAtEpochMillis > lesson.startsAtEpochMillis
            val overlaps = hasValidRange && examAt in lesson.startsAtEpochMillis until lesson.endsAtEpochMillis
            val shouldIgnore = isLikelySameSubjectLesson(exam, lesson) || looksLikeExamSlotLabel(lesson.title)

            if (overlaps && !shouldIgnore) {
                collisions += ExamCollision(
                    examId = exam.id,
                    examTitle = exam.title,
                    examStartsAtEpochMillis = examAt,
                    source = CollisionSource.LESSON,
                    sourceId = lesson.id,
                    sourceTitle = lesson.title,
                    sourceStartsAtEpochMillis = lesson.startsAtEpochMillis,
                    sourceEndsAtEpochMillis = lesson.endsAtEpochMillis,
                    sourceIsAllDay = false
                )
            }
        }

        events.forEach { event ->
            val conflicts = if (event.isAllDay) {
                isAllDayConflict(
                    examAtMillis = examAt,
                    eventStartMillis = event.startsAtEpochMillis,
                    eventEndMillis = event.endsAtEpochMillis,
                    zoneId = zoneId
                )
            } else {
                examAt in event.startsAtEpochMillis until event.endsAtEpochMillis
            }

            if (conflicts) {
                collisions += ExamCollision(
                    examId = exam.id,
                    examTitle = exam.title,
                    examStartsAtEpochMillis = examAt,
                    source = CollisionSource.EVENT,
                    sourceId = event.id,
                    sourceTitle = event.title,
                    sourceStartsAtEpochMillis = event.startsAtEpochMillis,
                    sourceEndsAtEpochMillis = event.endsAtEpochMillis,
                    sourceIsAllDay = event.isAllDay
                )
            }
        }
    }

    return collisions
        .distinctBy { collision ->
            "${collision.examId}|${collision.source}|${collision.sourceId}|${collision.sourceStartsAtEpochMillis}|${collision.sourceEndsAtEpochMillis}"
        }
        .sortedWith(
            compareBy<ExamCollision>(
                { it.examStartsAtEpochMillis },
                { it.examTitle.lowercase() },
                { it.source.ordinal },
                { it.sourceStartsAtEpochMillis }
            )
        )
}

private fun isLikelySameSubjectLesson(exam: Exam, lesson: TimetableLesson): Boolean {
    val examSubject = normalizeToken(exam.subject.orEmpty())
    if (examSubject.isBlank()) return false

    val lessonTitleNormalized = normalizeToken(lesson.title)
    if (lessonTitleNormalized.contains(examSubject)) return true

    val firstToken = lesson.title
        .substringBefore('·')
        .substringBefore('_')
        .substringBefore(' ')
    return normalizeToken(firstToken) == examSubject
}

private fun looksLikeExamSlotLabel(text: String): Boolean {
    val normalized = text.lowercase()
    return normalized.contains("prüfung") ||
        normalized.contains("pruefung") ||
        normalized.contains("exam") ||
        normalized.contains("test")
}

private fun normalizeToken(text: String): String {
    return text.trim()
        .lowercase()
        .replace(Regex("[^a-z0-9äöü]"), "")
}

fun collisionsByExam(collisions: List<ExamCollision>): Map<String, List<ExamCollision>> {
    return collisions
        .groupBy { it.examId }
        .mapValues { (_, entries) ->
            entries.sortedWith(
                compareBy<ExamCollision>(
                    { it.source.ordinal },
                    { it.sourceStartsAtEpochMillis },
                    { it.sourceTitle.lowercase() }
                )
            )
        }
}

private fun isAllDayConflict(
    examAtMillis: Long,
    eventStartMillis: Long,
    eventEndMillis: Long,
    zoneId: ZoneId
): Boolean {
    if (eventEndMillis <= eventStartMillis) return false
    val examDate = Instant.ofEpochMilli(examAtMillis).atZone(zoneId).toLocalDate()
    val eventStartDate = Instant.ofEpochMilli(eventStartMillis).atZone(zoneId).toLocalDate()
    val eventEndDateExclusive = Instant.ofEpochMilli(eventEndMillis).atZone(zoneId).toLocalDate()
    return examDate >= eventStartDate && examDate < eventEndDateExclusive
}
