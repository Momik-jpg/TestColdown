package com.andrin.examcountdown.ui

import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.ui.tabs.*
import org.junit.Assert.*
import org.junit.Test

class ExamFilterPolicyTest {
    private val now = 1_000_000L
    private val near = Exam(id = "near", title = "Zoologie", subject = "Biologie", startsAtEpochMillis = now + 100)
    private val far = Exam(id = "far", title = "Algebra", subject = "Mathematik", location = "Raum 42",
        startsAtEpochMillis = now + 10 * 86_400_000L)
    private val past = Exam(id = "past", title = "Vergangen", startsAtEpochMillis = now - 1)

    @Test fun spotlightIsEarliestUpcomingForEverySortOrder() {
        ExamSortMode.entries.forEach { sort ->
            val visible = filterExams(listOf(far, past, near), emptyMap(), ExamFilters(sort = sort), now)
            assertEquals(near, nextUpcomingExam(visible, now))
        }
    }

    @Test fun pastExamsAreNeverLabelledAsNext() {
        assertNull(nextUpcomingExam(listOf(past), now))
        assertEquals(near, nextUpcomingExam(listOf(near), near.startsAtEpochMillis))
        assertNull(nextUpcomingExam(listOf(near), near.startsAtEpochMillis + 1))
    }

    @Test fun resetRemovesEveryFilterAndRestoresChronologicalOrder() {
        val restricted = ExamFilters(query = "fehlt", subject = "Biologie", window = ExamWindowFilter.NEXT_7,
            sort = ExamSortMode.LATEST_FIRST)
        assertTrue(filterExams(listOf(near, far), emptyMap(), restricted, now).isEmpty())
        assertEquals(listOf(near, far), filterExams(listOf(far, near), emptyMap(), ExamFilters(), now))
    }

    @Test fun timeWindowIsInclusiveAndRecomputedWhenTimePasses() {
        val edge = near.copy(startsAtEpochMillis = now + 7 * 86_400_000L)
        val filters = ExamFilters(window = ExamWindowFilter.NEXT_7)
        assertEquals(listOf(near, edge), filterExams(listOf(past, edge, near, far), emptyMap(), filters, now))
        assertEquals(listOf(edge), filterExams(listOf(near, edge), emptyMap(), filters, now + 101))
    }

    @Test fun searchUsesPresentedTitleSubjectAndLocation() {
        val details = mapOf(near.id to ExamSearchDetails("Zellteilung", "Naturkunde"))
        assertEquals(listOf(near), filterExams(listOf(near, far), details,
            ExamFilters(query = "  ZELLTEILUNG ", subject = "Naturkunde"), now))
        assertEquals(listOf(far), filterExams(listOf(near, far), details, ExamFilters(query = "raum 42"), now))
    }
}
