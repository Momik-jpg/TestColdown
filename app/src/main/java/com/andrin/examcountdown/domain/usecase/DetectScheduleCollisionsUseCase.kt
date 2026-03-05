package com.andrin.examcountdown.domain.usecase

import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.model.SchoolEvent
import com.andrin.examcountdown.model.TimetableLesson
import com.andrin.examcountdown.util.CollisionRules
import com.andrin.examcountdown.util.ExamCollision
import com.andrin.examcountdown.util.collisionsByExam
import com.andrin.examcountdown.util.detectExamCollisions
import java.time.ZoneId

/**
 * Computes schedule collisions for exams against timetable lessons/events with configurable rules.
 * Output includes both the flat collision list and a map grouped by exam id.
 */
class DetectScheduleCollisionsUseCase {
    data class Params(
        val exams: List<Exam>,
        val lessons: List<TimetableLesson>,
        val events: List<SchoolEvent>,
        val rules: CollisionRules,
        val zoneId: ZoneId = ZoneId.of("Europe/Zurich")
    )

    data class Result(
        val collisions: List<ExamCollision>,
        val byExam: Map<String, List<ExamCollision>>
    )

    operator fun invoke(params: Params): Result {
        val collisions = detectExamCollisions(
            exams = params.exams,
            lessons = params.lessons,
            events = params.events,
            zoneId = params.zoneId,
            rules = params.rules
        )
        return Result(
            collisions = collisions,
            byExam = collisionsByExam(collisions)
        )
    }
}
