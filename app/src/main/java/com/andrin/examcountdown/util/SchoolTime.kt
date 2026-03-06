package com.andrin.examcountdown.util

import java.time.ZoneId

internal object SchoolTime {
    @Volatile
    internal var nowProvider: () -> Long = { System.currentTimeMillis() }

    val schoolZone: ZoneId = ZoneId.of("Europe/Zurich")

    fun nowMillis(): Long = nowProvider()

    internal fun resetNowProviderForTest() {
        nowProvider = { System.currentTimeMillis() }
    }
}
