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

    fun read(): String? {
        return securePreferences.getString(KEY_ICAL_URL, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    fun write(url: String?) {
        val normalized = url?.trim().orEmpty()
        securePreferences.edit().apply {
            if (normalized.isBlank()) {
                remove(KEY_ICAL_URL)
            } else {
                putString(KEY_ICAL_URL, normalized)
            }
        }.apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "secure_exam_store"
        private const val KEY_ICAL_URL = "secure_ical_url"
    }
}
