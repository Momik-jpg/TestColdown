package com.andrin.examcountdown.data

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object BackupCrypto {
    const val ENCRYPTED_PREFIX: String = "ECBKP1:"

    private const val SALT_SIZE_BYTES = 16
    private const val IV_SIZE_BYTES = 12
    private const val KEY_SIZE_BITS = 256
    private const val PBKDF2_ITERATIONS = 120_000
    private const val MIN_PASSWORD_LENGTH = 10

    fun isEncryptedPayload(raw: String): Boolean {
        return raw.trim().startsWith(ENCRYPTED_PREFIX)
    }

    fun encrypt(plainText: String, password: String): String {
        require(plainText.isNotBlank()) { "Backup ist leer." }
        val normalizedPassword = password.trim()
        require(normalizedPassword.length >= MIN_PASSWORD_LENGTH) {
            "Passwort muss mindestens $MIN_PASSWORD_LENGTH Zeichen haben."
        }

        val salt = ByteArray(SALT_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(IV_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(normalizedPassword, salt)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return ENCRYPTED_PREFIX + listOf(
            encodeBase64(salt),
            encodeBase64(iv),
            encodeBase64(encrypted)
        ).joinToString(".")
    }

    fun decrypt(payload: String, password: String): String {
        val normalizedPayload = payload.trim()
        require(isEncryptedPayload(normalizedPayload)) { "Backup ist nicht verschlüsselt." }
        val normalizedPassword = password.trim()
        require(normalizedPassword.isNotEmpty()) { "Passwort erforderlich." }

        val rawContent = normalizedPayload.removePrefix(ENCRYPTED_PREFIX)
        val parts = rawContent.split('.')
        require(parts.size == 3) { "Ungültiges Backup-Format." }

        val salt = decodeBase64(parts[0])
        val iv = decodeBase64(parts[1])
        val encrypted = decodeBase64(parts[2])
        val key = deriveKey(normalizedPassword, salt)

        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            val decrypted = cipher.doFinal(encrypted)
            String(decrypted, Charsets.UTF_8)
        } catch (_: Exception) {
            throw IllegalArgumentException("Entschlüsselung fehlgeschlagen. Passwort oder Datei ist falsch.")
        }
    }

    private fun deriveKey(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_SIZE_BITS)
        return try {
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val keyBytes = keyFactory.generateSecret(spec).encoded
            SecretKeySpec(keyBytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }

    private fun encodeBase64(bytes: ByteArray): String {
        return Base64.getEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun decodeBase64(raw: String): ByteArray {
        return try {
            Base64.getDecoder().decode(raw)
        } catch (_: Exception) {
            throw IllegalArgumentException("Ungültiges Backup-Format.")
        }
    }
}
