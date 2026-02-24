package com.andrin.examcountdown.data

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IcalSyncErrorFormatterTest {
    @Test
    fun formatsHttp404() {
        val message = toSyncErrorMessage(IOException("HTTP-404"))
        assertEquals("iCal-Link nicht gefunden (HTTP 404).", message)
    }

    @Test
    fun retriesOnTransientNetworkErrors() {
        assertTrue(shouldRetrySync(UnknownHostException("offline")))
        assertTrue(shouldRetrySync(SocketTimeoutException("timeout")))
        assertTrue(shouldRetrySync(ConnectException("refused")))
        assertFalse(shouldRetrySync(IllegalArgumentException("bad")))
    }
}
