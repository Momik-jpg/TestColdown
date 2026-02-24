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
            when {
                raw.contains("HTTP-401") -> "Zugriff verweigert (HTTP 401). Prüfe den iCal-Link."
                raw.contains("HTTP-403") -> "Zugriff verweigert (HTTP 403)."
                raw.contains("HTTP-404") -> "iCal-Link nicht gefunden (HTTP 404)."
                raw.contains("HTTP-410") -> "iCal-Link ist abgelaufen (HTTP 410)."
                raw.contains("HTTP-") -> "iCal-Serverfehler: $raw"
                raw.isBlank() -> "Netzwerkfehler beim iCal-Sync."
                else -> raw
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
        is IOException -> throwable !is FileNotFoundException
        else -> false
    }
}
