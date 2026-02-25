package com.andrin.examcountdown.data

import java.net.URI
import java.util.Locale

internal fun normalizeAndValidateIcalUrl(url: String): String {
    val raw = url.trim()
    require(raw.isNotBlank()) { "Ungültige URL" }

    val uri = runCatching { URI(raw) }
        .getOrElse { throw IllegalArgumentException("Ungültige URL", it) }

    val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
    require(scheme == "https") { "Nur sichere HTTPS-Links sind erlaubt." }
    require(uri.userInfo.isNullOrBlank()) { "Ungültige URL" }

    val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
    require(host.isNotBlank()) { "Ungültige URL" }
    require(uri.port == -1 || uri.port == 443) { "Ungültige URL" }
    require(!isLocalOrPrivateHost(host)) {
        "Lokale/private Hostnamen oder IPs sind nicht erlaubt."
    }

    val normalizedPath = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
    return URI(
        "https",
        null,
        host,
        -1,
        normalizedPath,
        uri.rawQuery,
        null
    ).toASCIIString()
}

internal fun normalizeImportedIcalUrlOrNull(raw: String?): String? {
    val value = raw?.trim().orEmpty()
    if (value.isBlank()) return null
    return runCatching { normalizeAndValidateIcalUrl(value) }.getOrNull()
}

private fun isLocalOrPrivateHost(host: String): Boolean {
    val normalized = host.trim().trimEnd('.').lowercase(Locale.ROOT)
    if (normalized.isBlank()) return true
    if (normalized == "localhost") return true
    if (
        normalized.endsWith(".localhost") ||
        normalized.endsWith(".local") ||
        normalized.endsWith(".internal")
    ) return true
    if (isPrivateIpv4Literal(normalized)) return true
    if (isPrivateIpv6Literal(normalized)) return true
    return false
}

private fun isPrivateIpv4Literal(host: String): Boolean {
    val parts = host.split('.')
    if (parts.size != 4) return false
    val octets = parts.map { it.toIntOrNull() ?: return false }
    if (octets.any { it !in 0..255 }) return false

    val first = octets[0]
    val second = octets[1]

    return when {
        first == 0 -> true
        first == 10 -> true
        first == 127 -> true
        first == 169 && second == 254 -> true
        first == 172 && second in 16..31 -> true
        first == 192 && second == 168 -> true
        first == 100 && second in 64..127 -> true
        first >= 224 -> true
        else -> false
    }
}

private fun isPrivateIpv6Literal(host: String): Boolean {
    val normalized = host
        .removePrefix("[")
        .removeSuffix("]")
        .substringBefore('%')
        .lowercase(Locale.ROOT)
    if (!normalized.contains(':')) return false
    if (normalized == "::") return true
    if (normalized == "::1") return true
    if (normalized.startsWith("fc") || normalized.startsWith("fd")) return true
    if (
        normalized.startsWith("fe8") ||
        normalized.startsWith("fe9") ||
        normalized.startsWith("fea") ||
        normalized.startsWith("feb")
    ) return true
    return false
}
