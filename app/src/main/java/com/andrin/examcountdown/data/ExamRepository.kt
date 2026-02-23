package com.andrin.examcountdown.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.andrin.examcountdown.model.Exam
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "exam_store")

class ExamRepository(private val appContext: Context) {
    private val examsKey = stringPreferencesKey("exams_json")
    private val json = Json { ignoreUnknownKeys = true }

    val examsFlow: Flow<List<Exam>> = appContext.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            decode(preferences[examsKey])
                .sortedBy { it.startsAtEpochMillis }
        }

    suspend fun addExam(exam: Exam) {
        updateExams { current ->
            (current + exam)
                .distinctBy { it.id }
                .sortedBy { it.startsAtEpochMillis }
        }
    }

    suspend fun deleteExam(examId: String) {
        updateExams { current ->
            current.filterNot { it.id == examId }
        }
    }

    suspend fun readSnapshot(): List<Exam> = examsFlow.first()

    private suspend fun updateExams(transform: (List<Exam>) -> List<Exam>) {
        appContext.dataStore.edit { preferences ->
            val updated = transform(decode(preferences[examsKey]))
            preferences[examsKey] = json.encodeToString(updated)
        }
    }

    private fun decode(raw: String?): List<Exam> {
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<Exam>>(raw) }
            .getOrDefault(emptyList())
    }
}