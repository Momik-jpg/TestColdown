package com.andrin.examcountdown.data

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.VisibleForTesting
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

internal class SecureIcalUrlStore private constructor(
    private val securePreferences: SharedPreferences,
    private val legacyPreferences: SharedPreferences?
) {
    constructor(context: Context) : this(
        securePreferences = createEncryptedPreferences(
            context = context.applicationContext,
            fileName = PREFERENCES_NAME
        ),
        legacyPreferences = createEncryptedPreferences(
            context = context.applicationContext,
            fileName = LEGACY_PREFERENCES_NAME
        )
    )

    @VisibleForTesting
    internal constructor(
        securePreferencesOverride: SharedPreferences,
        legacyPreferencesOverride: SharedPreferences? = null,
        @Suppress("UNUSED_PARAMETER")
        marker: TestConstructorMarker = TestConstructorMarker
    ) : this(
        securePreferences = securePreferencesOverride,
        legacyPreferences = legacyPreferencesOverride
    )

    private fun migrateLegacyUrlsIfNeeded(): List<String> {
        val secureUrls = readUrlsFromPreferences(securePreferences)
        if (secureUrls.isNotEmpty()) {
            return secureUrls
        }

        val legacyUrls = readUrlsFromPreferences(legacyPreferences)
        if (legacyUrls.isEmpty()) {
            return emptyList()
        }

        persistUrls(legacyUrls)
        clearLegacyEntries()
        return legacyUrls
    }

    private fun readUrlsFromPreferences(preferences: SharedPreferences?): List<String> {
        if (preferences == null) return emptyList()

        val combined = preferences.getString(KEY_ICAL_URLS, null)
            ?.trim()
            .orEmpty()
        if (combined.isNotBlank()) {
            return combined
                .split('\n')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .take(MAX_ICAL_URLS)
        }

        val legacySingle = preferences.getString(KEY_ICAL_URL_LEGACY, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { listOf(it) }
            ?: emptyList()
        return legacySingle
            .distinct()
            .take(MAX_ICAL_URLS)
    }

    private fun persistUrls(urls: List<String>) {
        if (urls.isEmpty()) {
            securePreferences.edit()
                .remove(KEY_ICAL_URLS)
                .remove(KEY_ICAL_URL_LEGACY)
                .apply()
            return
        }

        securePreferences.edit()
            .remove(KEY_ICAL_URL_LEGACY)
            .putString(KEY_ICAL_URLS, urls.joinToString("\n"))
            .apply()
    }

    private fun clearLegacyEntries() {
        legacyPreferences?.edit()
            ?.remove(KEY_ICAL_URLS)
            ?.remove(KEY_ICAL_URL_LEGACY)
            ?.apply()
    }

    fun read(): String? = readAll().firstOrNull()

    fun readAll(): List<String> {
        return migrateLegacyUrlsIfNeeded()
    }

    fun write(url: String?) {
        writeAll(url?.trim()?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList())
    }

    fun writeAll(urls: List<String>) {
        val normalized = urls
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_ICAL_URLS)

        persistUrls(normalized)
        clearLegacyEntries()
    }

    companion object {
        private const val PREFERENCES_NAME = "secure_exam_store"
        private const val LEGACY_PREFERENCES_NAME = "secure_exam_store_legacy"
        private const val KEY_ICAL_URLS = "secure_ical_urls"
        private const val KEY_ICAL_URL_LEGACY = "secure_ical_url"
        private const val MAX_ICAL_URLS = 2
        internal object TestConstructorMarker

        private fun createEncryptedPreferences(
            context: Context,
            fileName: String
        ): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                fileName,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }
}
