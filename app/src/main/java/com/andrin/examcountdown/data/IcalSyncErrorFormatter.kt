package com.andrin.examcountdown.data

import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

internal fun toSyncErrorMessage(throwable: Throwable): String {
    return when (throwable) {
        is IllegalArgumentException -> throwable.message ?: "Ungültige Eingabe."
        is UnknownHostException -> "Kein Internet oder Host nicht erreichbar."
        is SocketTimeoutException -> "Zeitüberschreitung beim Laden des iCal."
        is ConnectException -> "Verbindung zum iCal-Server fehlgeschlagen."
        is FileNotFoundException -> "iCal-Link ungültig oder nicht mehr verfügbar."
        is SSLException -> "Sichere Verbindung (SSL/TLS) fehlgeschlagen."
        is IOException -> {
            val raw = throwable.message.orEmpty()
            val statusCode = extractHttpStatusCode(raw)
            when {
                raw.contains("HTTP-401") -> "Zugriff verweigert (HTTP 401). Prüfe den iCal-Link."
                raw.contains("HTTP-403") -> "Zugriff verweigert (HTTP 403)."
                raw.contains("HTTP-404") -> "iCal-Link nicht gefunden (HTTP 404)."
                raw.contains("HTTP-410") -> "iCal-Link ist abgelaufen (HTTP 410)."
                raw.contains("HTTP-") && statusCode != null -> "iCal-Serverfehler (HTTP $statusCode)."
                raw.contains("HTTP-") -> "iCal-Serverfehler."
                raw.isBlank() -> "Netzwerkfehler beim iCal-Sync."
                else -> sanitizeNetworkErrorDetails(raw)
            }
        }

        else -> throwable.message?.takeIf { it.isNotBlank() } ?: "Unbekannter Fehler"
    }
}

internal fun shouldRetrySync(throwable: Throwable): Boolean {
    return when (throwable) {
        is UnknownHostException,
        is SocketTimeoutException,
        is ConnectException -> true
        is SSLException -> false
        is IOException -> {
            if (throwable is FileNotFoundException) return false
            when (extractHttpStatusCode(throwable.message)) {
                null -> true
                408, 429 -> true
                in 500..599 -> true
                else -> false
            }
        }
        else -> false
    }
}

private fun extractHttpStatusCode(message: String?): Int? {
    val raw = message.orEmpty()
    val match = Regex("HTTP-(\\d{3})").find(raw) ?: return null
    return match.groupValues.getOrNull(1)?.toIntOrNull()
}

private fun sanitizeNetworkErrorDetails(raw: String): String {
    val withoutUrls = raw.replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "[URL]")
    val withoutTokenParams = withoutUrls.replace(
        Regex("([?&](token|auth|key|longurl)=)[^&\\s]+", RegexOption.IGNORE_CASE),
        "$1***"
    )
    val compact = withoutTokenParams.replace(Regex("\\s+"), " ").trim()
    return compact.take(160).ifBlank { "Netzwerkfehler beim iCal-Sync." }
}
