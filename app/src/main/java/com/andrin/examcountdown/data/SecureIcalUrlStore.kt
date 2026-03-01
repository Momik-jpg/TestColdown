package com.andrin.examcountdown.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

internal class SecureIcalUrlStore(context: Context) {
    private val appContext = context.applicationContext

    private val securePreferences: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFERENCES_NAME,
            masterKeyAlias,
            appContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun read(): String? = readAll().firstOrNull()

    fun readAll(): List<String> {
        val combined = securePreferences.getString(KEY_ICAL_URLS, null)
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

        // Backward compatibility: older app versions stored one URL.
        return securePreferences.getString(KEY_ICAL_URL_LEGACY, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { listOf(it) }
            ?: emptyList()
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

        securePreferences.edit().apply {
            remove(KEY_ICAL_URL_LEGACY)
            if (normalized.isEmpty()) {
                remove(KEY_ICAL_URLS)
            } else {
                putString(KEY_ICAL_URLS, normalized.joinToString("\n"))
            }
        }.apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "secure_exam_store"
        private const val KEY_ICAL_URLS = "secure_ical_urls"
        private const val KEY_ICAL_URL_LEGACY = "secure_ical_url"
        private const val MAX_ICAL_URLS = 2
    }
}
