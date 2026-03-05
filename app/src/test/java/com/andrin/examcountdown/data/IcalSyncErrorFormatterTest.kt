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
        assertTrue(shouldRetrySync(IOException("HTTP-500")))
        assertFalse(shouldRetrySync(IOException("HTTP-401")))
        assertFalse(shouldRetrySync(IllegalArgumentException("bad")))
    }

    @Test
    fun retriesOnThrottlingAndTimeoutHttpCodes() {
        assertTrue(shouldRetrySync(IOException("HTTP-408")))
        assertTrue(shouldRetrySync(IOException("HTTP-429")))
        assertFalse(shouldRetrySync(IOException("HTTP-403")))
    }

    @Test
    fun retriesOnGenericIoWithoutStatus() {
        assertTrue(shouldRetrySync(IOException("connection reset by peer")))
    }
}
