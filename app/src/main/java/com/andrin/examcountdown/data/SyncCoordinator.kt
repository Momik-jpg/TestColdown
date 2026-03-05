package com.andrin.examcountdown.data

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

sealed interface SyncExecutionResult {
    data class Success(val result: IcalSyncResult) : SyncExecutionResult
    data class Failed(
        val message: String,
        val retryable: Boolean
    ) : SyncExecutionResult
    data object NoUrls : SyncExecutionResult
}

/**
 * Central sync gate that serializes all sync entry points (worker/manual/widget)
 * to avoid overlapping writes and duplicate side-effects.
 */
object SyncCoordinator {
    private val syncMutex = Mutex()

    internal suspend fun <T> withSingleflightLockForTest(block: suspend () -> T): T {
        return syncMutex.withLock { block() }
    }

    suspend fun syncFromRepository(
        context: Context,
        emitChangeNotification: Boolean
    ): SyncExecutionResult {
        val appContext = context.applicationContext
        val repository = ExamRepository(appContext)
        val urls = repository.readIcalUrls()
        val includeEvents = repository.readImportEventsEnabled()
        return syncInternal(
            context = appContext,
            repository = repository,
            urls = urls,
            includeEvents = includeEvents,
            emitChangeNotification = emitChangeNotification
        )
    }

    suspend fun syncExplicit(
        context: Context,
        urls: List<String>,
        includeEvents: Boolean,
        emitChangeNotification: Boolean
    ): SyncExecutionResult {
        val appContext = context.applicationContext
        val repository = ExamRepository(appContext)
        return syncInternal(
            context = appContext,
            repository = repository,
            urls = urls,
            includeEvents = includeEvents,
            emitChangeNotification = emitChangeNotification
        )
    }

    private suspend fun syncInternal(
        context: Context,
        repository: ExamRepository,
        urls: List<String>,
        includeEvents: Boolean,
        emitChangeNotification: Boolean
    ): SyncExecutionResult = syncMutex.withLock {
        val normalizedUrls = runCatching {
            urls.map { normalizeAndValidateIcalUrl(it) }
                .distinct()
                .take(ExamRepository.MAX_ICAL_URLS)
        }.getOrElse { throwable ->
            val message = toSyncErrorMessage(throwable)
            repository.markSyncError("Sync fehlgeschlagen: $message")
            return SyncExecutionResult.Failed(
                message = message,
                retryable = shouldRetrySync(throwable)
            )
        }

        if (normalizedUrls.isEmpty()) {
            return SyncExecutionResult.NoUrls
        }

        return runCatching {
            IcalSyncEngine(context).syncFromUrls(
                urls = normalizedUrls,
                emitChangeNotification = emitChangeNotification,
                importEvents = includeEvents
            )
        }.fold(
            onSuccess = { result ->
                SyncExecutionResult.Success(result)
            },
            onFailure = { throwable ->
                val message = toSyncErrorMessage(throwable)
                repository.markSyncError("Sync fehlgeschlagen: $message")
                SyncExecutionResult.Failed(
                    message = message,
                    retryable = shouldRetrySync(throwable)
                )
            }
        )
    }
}
