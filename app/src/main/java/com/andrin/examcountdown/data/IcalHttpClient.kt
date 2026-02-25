package com.andrin.examcountdown.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal data class IcalHttpResponse(
    val body: String?,
    val httpStatusCode: Int,
    val etag: String?,
    val lastModified: String?,
    val notModified: Boolean
)

internal object IcalHttpClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun download(url: String): String {
        val response = download(
            url = url,
            previousEtag = null,
            previousLastModified = null
        )
        if (response.notModified || response.body.isNullOrBlank()) {
            throw IOException("Leere iCal-Antwort")
        }
        return response.body
    }

    fun download(
        url: String,
        previousEtag: String?,
        previousLastModified: String?
    ): IcalHttpResponse {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "ExamCountdown/1.0")
            setRequestProperty("Accept", "text/calendar, text/plain, */*")
            previousEtag
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { setRequestProperty("If-None-Match", it) }
            previousLastModified
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { setRequestProperty("If-Modified-Since", it) }
        }

        return try {
            val code = connection.responseCode
            val etag = connection.getHeaderField("ETag")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val lastModified = connection.getHeaderField("Last-Modified")
                ?.trim()
                ?.takeIf { it.isNotBlank() }

            if (code == HttpURLConnection.HTTP_NOT_MODIFIED) {
                return IcalHttpResponse(
                    body = null,
                    httpStatusCode = code,
                    etag = etag ?: previousEtag,
                    lastModified = lastModified ?: previousLastModified,
                    notModified = true
                )
            }

            if (code !in 200..299) {
                throw IOException("HTTP-$code")
            }

            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }.ifBlank {
                throw IOException("Leere iCal-Antwort")
            }

            IcalHttpResponse(
                body = body,
                httpStatusCode = code,
                etag = etag,
                lastModified = lastModified,
                notModified = false
            )
        } finally {
            connection.disconnect()
        }
    }
}
