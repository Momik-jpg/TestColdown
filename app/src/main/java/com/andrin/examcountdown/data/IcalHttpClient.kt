package com.andrin.examcountdown.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

internal object IcalHttpClient {
    private const val CONNECT_TIMEOUT_MS = 15_000
    private const val READ_TIMEOUT_MS = 20_000

    fun download(url: String): String {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("User-Agent", "ExamCountdown/1.0")
            setRequestProperty("Accept", "text/calendar, text/plain, */*")
        }

        return try {
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IOException("HTTP-$code")
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }.ifBlank {
                throw IOException("Leere iCal-Antwort")
            }
        } finally {
            connection.disconnect()
        }
    }
}
