package com.andrin.examcountdown.ui.tabs

import com.andrin.examcountdown.model.Exam
import java.util.Locale

internal const val SUBJECT_FILTER_ALL = "Alle Fächer"

internal enum class ExamWindowFilter(val title: String, val maxDaysAhead: Int?) {
    ALL("Alle", null), NEXT_7("7 Tage", 7), NEXT_30("30 Tage", 30), NEXT_90("90 Tage", 90)
}

internal enum class ExamSortMode(val title: String) {
    NEXT_FIRST("Nächste"), LATEST_FIRST("Späteste"), SUBJECT_AZ("Fach A-Z"), TITLE_AZ("Titel A-Z")
}

internal data class ExamFilters(
    val query: String = "",
    val subject: String = SUBJECT_FILTER_ALL,
    val window: ExamWindowFilter = ExamWindowFilter.ALL,
    val sort: ExamSortMode = ExamSortMode.NEXT_FIRST
)

internal data class ExamSearchDetails(val title: String, val subject: String?)

internal fun filterExams(
    exams: List<Exam>,
    details: Map<String, ExamSearchDetails>,
    filters: ExamFilters,
    now: Long
): List<Exam> {
    val query = filters.query.trim().lowercase(Locale.ROOT)
    val end = filters.window.maxDaysAhead?.let { now + it * 86_400_000L }
    fun title(exam: Exam) = (details[exam.id]?.title ?: exam.title).lowercase(Locale.ROOT)
    fun subject(exam: Exam) = (details[exam.id]?.subject ?: exam.subject).orEmpty()
    val result = exams.filter { exam ->
        val matchesSubject = filters.subject == SUBJECT_FILTER_ALL ||
            subject(exam).equals(filters.subject, ignoreCase = true)
        val matchesQuery = query.isBlank() ||
            "${subject(exam)} ${title(exam)} ${exam.location.orEmpty()}"
                .lowercase(Locale.ROOT).contains(query)
        matchesSubject && matchesQuery && (end == null || exam.startsAtEpochMillis in now..end)
    }
    return when (filters.sort) {
        ExamSortMode.NEXT_FIRST -> result.sortedBy { it.startsAtEpochMillis }
        ExamSortMode.LATEST_FIRST -> result.sortedByDescending { it.startsAtEpochMillis }
        ExamSortMode.SUBJECT_AZ -> result.sortedWith(compareBy(
            { subject(it).lowercase(Locale.ROOT) }, { title(it) }, { it.startsAtEpochMillis }
        ))
        ExamSortMode.TITLE_AZ -> result.sortedWith(compareBy(
            { title(it) }, { subject(it).lowercase(Locale.ROOT) }, { it.startsAtEpochMillis }
        ))
    }
}

/** The spotlight is chronological even when the remaining list is sorted by subject or title. */
internal fun nextUpcomingExam(exams: List<Exam>, now: Long): Exam? =
    exams.filter { it.startsAtEpochMillis >= now }
        .minWithOrNull(compareBy<Exam> { it.startsAtEpochMillis }.thenBy { it.id })
