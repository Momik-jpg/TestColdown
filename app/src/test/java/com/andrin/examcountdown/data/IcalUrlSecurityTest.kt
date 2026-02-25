package com.andrin.examcountdown.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IcalUrlSecurityTest {
    @Test
    fun `accepts schulnetz https url`() {
        val url = "https://www.schulnetz-ag.ch/aksa/cindex.php?longurl=abc123"
        val normalized = normalizeAndValidateIcalUrl(url)
        assertEquals(url, normalized)
    }

    @Test
    fun `accepts generic public https url`() {
        val url = "https://calendar.kanti-example.ch/feed.ics"
        val normalized = normalizeAndValidateIcalUrl(url)
        assertEquals(url, normalized)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects cleartext http`() {
        normalizeAndValidateIcalUrl("http://www.schulnetz-ag.ch/aksa/cindex.php?longurl=abc123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects private ipv4 host`() {
        normalizeAndValidateIcalUrl("https://192.168.1.20/feed.ics")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects non standard port`() {
        normalizeAndValidateIcalUrl("https://www.schulnetz-ag.ch:8443/aksa/cindex.php?longurl=abc123")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects localhost`() {
        normalizeAndValidateIcalUrl("https://localhost/feed.ics")
    }

    @Test
    fun `import helper returns null for invalid url`() {
        assertNull(normalizeImportedIcalUrlOrNull("https://127.0.0.1/feed.ics"))
    }
}
