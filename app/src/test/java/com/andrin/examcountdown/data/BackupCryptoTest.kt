package com.andrin.examcountdown.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {
    @Test
    fun encrypt_then_decrypt_roundtrip() {
        val raw = """{"schemaVersion":10,"exams":[]}"""
        val password = "StrongPass123"

        val encrypted = BackupCrypto.encrypt(raw, password)

        assertTrue(BackupCrypto.isEncryptedPayload(encrypted))
        assertNotEquals(raw, encrypted)
        assertEquals(raw, BackupCrypto.decrypt(encrypted, password))
    }

    @Test(expected = IllegalArgumentException::class)
    fun decrypt_with_wrong_password_throws() {
        val raw = """{"schemaVersion":10,"exams":[]}"""
        val encrypted = BackupCrypto.encrypt(raw, "CorrectPass")
        BackupCrypto.decrypt(encrypted, "WrongPass")
    }

    @Test(expected = IllegalArgumentException::class)
    fun encrypt_with_short_password_throws() {
        val raw = """{"schemaVersion":10,"exams":[]}"""
        BackupCrypto.encrypt(raw, "short")
    }
}
