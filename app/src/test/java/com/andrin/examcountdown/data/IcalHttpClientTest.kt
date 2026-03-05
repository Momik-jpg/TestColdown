package com.andrin.examcountdown.data

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IcalHttpClientTest {

    @Test
    fun download_whenServerReturns304_usesPreviousMetadataAndNoBody() {
        IcalHttpClient.connectionFactory = {
            FakeHttpConnection(
                url = URL(it),
                responseCodeValue = HttpURLConnection.HTTP_NOT_MODIFIED
            )
        }

        try {
            val previousEtag = "\"etag-prev\""
            val previousLastModified = "Wed, 01 Jan 2025 12:00:00 GMT"

            val response = IcalHttpClient.download(
                url = "https://example.com/calendar.ics",
                previousEtag = previousEtag,
                previousLastModified = previousLastModified
            )

            assertTrue(response.notModified)
            assertEquals(HttpURLConnection.HTTP_NOT_MODIFIED, response.httpStatusCode)
            assertNull(response.body)
            assertEquals(previousEtag, response.etag)
            assertEquals(previousLastModified, response.lastModified)
        } finally {
            IcalHttpClient.resetConnectionFactoryForTest()
        }
    }

    @Test
    fun download_when304ContainsFreshHeaders_prefersFreshMetadata() {
        IcalHttpClient.connectionFactory = {
            FakeHttpConnection(
                url = URL(it),
                responseCodeValue = HttpURLConnection.HTTP_NOT_MODIFIED,
                headers = mapOf(
                    "ETag" to "\"etag-new\"",
                    "Last-Modified" to "Thu, 02 Jan 2025 08:30:00 GMT"
                )
            )
        }

        try {
            val response = IcalHttpClient.download(
                url = "https://example.com/calendar.ics",
                previousEtag = "\"etag-prev\"",
                previousLastModified = "Wed, 01 Jan 2025 12:00:00 GMT"
            )

            assertTrue(response.notModified)
            assertEquals("\"etag-new\"", response.etag)
            assertEquals("Thu, 02 Jan 2025 08:30:00 GMT", response.lastModified)
            assertNull(response.body)
        } finally {
            IcalHttpClient.resetConnectionFactoryForTest()
        }
    }

    private class FakeHttpConnection(
        url: URL,
        private val responseCodeValue: Int,
        private val headers: Map<String, String?> = emptyMap(),
        private val body: String? = null
    ) : HttpURLConnection(url) {
        private val requestHeaders = mutableMapOf<String, String>()

        override fun connect() = Unit

        override fun disconnect() = Unit

        override fun usingProxy(): Boolean = false

        override fun setRequestProperty(key: String?, value: String?) {
            if (key != null && value != null) {
                requestHeaders[key] = value
            }
        }

        override fun getRequestProperty(key: String?): String? {
            return key?.let { requestHeaders[it] }
        }

        override fun getResponseCode(): Int = responseCodeValue

        override fun getHeaderField(name: String?): String? {
            return name?.let { headers[it] }
        }

        override fun getInputStream(): ByteArrayInputStream {
            val content = body ?: throw IOException("No body configured for fake connection.")
            return ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        }
    }
}
