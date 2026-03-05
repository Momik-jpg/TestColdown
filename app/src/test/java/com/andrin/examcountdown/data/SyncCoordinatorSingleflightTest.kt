package com.andrin.examcountdown.data

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class SyncCoordinatorSingleflightTest {

    @Test
    fun withSingleflightLockForTest_serializesConcurrentTriggersWithoutOverlap() = runBlocking {
        val activeRuns = AtomicInteger(0)
        val maxConcurrentRuns = AtomicInteger(0)
        val totalRuns = AtomicInteger(0)

        coroutineScope {
            listOf(
                async {
                    SyncCoordinator.withSingleflightLockForTest {
                        totalRuns.incrementAndGet()
                        val concurrent = activeRuns.incrementAndGet()
                        maxConcurrentRuns.updateAndGet { current -> maxOf(current, concurrent) }
                        delay(120)
                        activeRuns.decrementAndGet()
                    }
                },
                async {
                    SyncCoordinator.withSingleflightLockForTest {
                        totalRuns.incrementAndGet()
                        val concurrent = activeRuns.incrementAndGet()
                        maxConcurrentRuns.updateAndGet { current -> maxOf(current, concurrent) }
                        delay(120)
                        activeRuns.decrementAndGet()
                    }
                }
            ).awaitAll()
        }

        assertEquals(2, totalRuns.get())
        assertEquals(1, maxConcurrentRuns.get())
    }
}
