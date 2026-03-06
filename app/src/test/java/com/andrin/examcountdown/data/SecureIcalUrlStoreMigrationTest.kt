package com.andrin.examcountdown.data

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecureIcalUrlStoreMigrationTest {

    @Test
    fun readAll_whenLegacyHasSingleUrl_migratesToSecureStoreAndClearsLegacy() {
        val securePrefs = InMemorySharedPreferences()
        val legacyPrefs = InMemorySharedPreferences().apply {
            edit().putString("secure_ical_url", "https://example.com/legacy.ics").apply()
        }

        val store = SecureIcalUrlStore(
            securePreferencesOverride = securePrefs,
            legacyPreferencesOverride = legacyPrefs
        )

        val firstRead = store.readAll()
        val secondRead = store.readAll()

        assertEquals(listOf("https://example.com/legacy.ics"), firstRead)
        assertEquals(firstRead, secondRead)
        assertEquals(
            "https://example.com/legacy.ics",
            securePrefs.getString("secure_ical_urls", null)
        )
        assertNull(legacyPrefs.getString("secure_ical_url", null))
        assertNull(legacyPrefs.getString("secure_ical_urls", null))
    }

    @Test
    fun readAll_whenSecureExists_prefersSecureAndDoesNotOverwrite() {
        val securePrefs = InMemorySharedPreferences().apply {
            edit().putString("secure_ical_urls", "https://secure.one\nhttps://secure.two").apply()
        }
        val legacyPrefs = InMemorySharedPreferences().apply {
            edit().putString("secure_ical_url", "https://legacy.example").apply()
        }

        val store = SecureIcalUrlStore(
            securePreferencesOverride = securePrefs,
            legacyPreferencesOverride = legacyPrefs
        )

        val urls = store.readAll()

        assertEquals(listOf("https://secure.one", "https://secure.two"), urls)
        assertEquals("https://legacy.example", legacyPrefs.getString("secure_ical_url", null))
    }

    @Test
    fun writeAll_clearsLegacyEntries_andStoresNormalizedUrls() {
        val securePrefs = InMemorySharedPreferences()
        val legacyPrefs = InMemorySharedPreferences().apply {
            edit().putString("secure_ical_url", "https://legacy.example").apply()
        }

        val store = SecureIcalUrlStore(
            securePreferencesOverride = securePrefs,
            legacyPreferencesOverride = legacyPrefs
        )

        store.writeAll(
            listOf(
                "  https://one.example  ",
                "https://one.example",
                "https://two.example",
                "https://three.example"
            )
        )

        assertEquals(
            "https://one.example\nhttps://two.example",
            securePrefs.getString("secure_ical_urls", null)
        )
        assertNull(legacyPrefs.getString("secure_ical_url", null))
        assertNull(legacyPrefs.getString("secure_ical_urls", null))
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val values = linkedMapOf<String, Any?>()

        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String?, defValue: String?): String? {
            val actualKey = key ?: return defValue
            return values[actualKey] as? String ?: defValue
        }

        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? {
            val actualKey = key ?: return defValues
            @Suppress("UNCHECKED_CAST")
            return (values[actualKey] as? Set<String>)?.toMutableSet() ?: defValues
        }

        override fun getInt(key: String?, defValue: Int): Int {
            val actualKey = key ?: return defValue
            return values[actualKey] as? Int ?: defValue
        }

        override fun getLong(key: String?, defValue: Long): Long {
            val actualKey = key ?: return defValue
            return values[actualKey] as? Long ?: defValue
        }

        override fun getFloat(key: String?, defValue: Float): Float {
            val actualKey = key ?: return defValue
            return values[actualKey] as? Float ?: defValue
        }

        override fun getBoolean(key: String?, defValue: Boolean): Boolean {
            val actualKey = key ?: return defValue
            return values[actualKey] as? Boolean ?: defValue
        }

        override fun contains(key: String?): Boolean {
            val actualKey = key ?: return false
            return values.containsKey(actualKey)
        }

        override fun edit(): SharedPreferences.Editor = Editor(values)

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = Unit

        private class Editor(
            private val target: MutableMap<String, Any?>
        ) : SharedPreferences.Editor {
            private val stagedValues = linkedMapOf<String, Any?>()
            private val removedKeys = linkedSetOf<String>()
            private var clearRequested = false

            override fun putString(key: String?, value: String?): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                stagedValues[actualKey] = value
                removedKeys.remove(actualKey)
            }

            override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                stagedValues[actualKey] = values?.toSet()
                removedKeys.remove(actualKey)
            }

            override fun putInt(key: String?, value: Int): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                stagedValues[actualKey] = value
                removedKeys.remove(actualKey)
            }

            override fun putLong(key: String?, value: Long): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                stagedValues[actualKey] = value
                removedKeys.remove(actualKey)
            }

            override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                stagedValues[actualKey] = value
                removedKeys.remove(actualKey)
            }

            override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                stagedValues[actualKey] = value
                removedKeys.remove(actualKey)
            }

            override fun remove(key: String?): SharedPreferences.Editor = apply {
                val actualKey = key ?: return@apply
                removedKeys += actualKey
                stagedValues.remove(actualKey)
            }

            override fun clear(): SharedPreferences.Editor = apply {
                clearRequested = true
                stagedValues.clear()
                removedKeys.clear()
            }

            override fun commit(): Boolean {
                apply()
                return true
            }

            override fun apply() {
                if (clearRequested) {
                    target.clear()
                }
                removedKeys.forEach { key -> target.remove(key) }
                stagedValues.forEach { (key, value) ->
                    if (value == null) {
                        target.remove(key)
                    } else {
                        target[key] = value
                    }
                }
            }
        }
    }
}
