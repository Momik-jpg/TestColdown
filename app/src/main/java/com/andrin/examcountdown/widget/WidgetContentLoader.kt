package com.andrin.examcountdown.widget

import android.content.Context
import com.andrin.examcountdown.data.ExamRepository
import com.andrin.examcountdown.model.Exam
import kotlinx.coroutines.runBlocking

object WidgetContentLoader {
    fun loadUpcoming(context: Context, limit: Int): List<Exam> {
        return runBlocking {
            ExamRepository(context.applicationContext)
                .readSnapshot()
                .filter { it.startsAtEpochMillis >= System.currentTimeMillis() }
                .sortedBy { it.startsAtEpochMillis }
                .take(limit)
        }
    }
}