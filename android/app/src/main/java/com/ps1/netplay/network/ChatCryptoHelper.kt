package com.ps1.netplay.network

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ChatCryptoHelper {
    private const val PREFIX_LEGACY = "ENC::"
    private const val PREFIX_GCM = "ENC::GCM::"
    private const val GCM_TAG_LENGTH = 128
    private const val GCM_IV_LENGTH = 12

    private val secureRandom = SecureRandom()

    private fun normalizeConvId(convId: String): String {
        return convId.split("_").map { it.trim().lowercase() }.sorted().joinToString("_")
    }

    private fun deriveKey256(convId: String): SecretKeySpec {
        val norm = normalizeConvId(convId)
        val sha = MessageDigest.getInstance("SHA-256").digest(("almahalla_conv_salt_" + norm).toByteArray(Charsets.UTF_8))
        return SecretKeySpec(sha, "AES")
    }

    private fun deriveKeyLegacy(convId: String): Pair<SecretKeySpec, IvParameterSpec> {
        val norm = normalizeConvId(convId)
        val sha = MessageDigest.getInstance("SHA-256").digest(("ps1_chat_salt_" + norm).toByteArray(Charsets.UTF_8))
        val keyBytes = sha.copyOfRange(0, 16)
        val ivBytes = sha.copyOfRange(16, 32)
        return Pair(SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
    }

    private fun deriveUniversalKeyLegacy(): Pair<SecretKeySpec, IvParameterSpec> {
        val sha = MessageDigest.getInstance("SHA-256").digest("ps1_chat_universal_v1_salt".toByteArray(Charsets.UTF_8))
        val keyBytes = sha.copyOfRange(0, 16)
        val ivBytes = sha.copyOfRange(16, 32)
        return Pair(SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
    }

    /**
     * Encrypts plain text message using AES-256-GCM with a fresh random IV per message
     */
    fun encrypt(plainText: String, convId: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val key = deriveKey256(convId)
            val iv = ByteArray(GCM_IV_LENGTH)
            secureRandom.nextBytes(iv)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
            val cipherBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val ivB64 = Base64.encodeToString(iv, Base64.NO_WRAP)
            val cipherB64 = Base64.encodeToString(cipherBytes, Base64.NO_WRAP)
            "$PREFIX_GCM$ivB64::$cipherB64"
        } catch (_: Exception) {
            // Fallback to legacy CBC if GCM unavailable
            try {
                val (key, iv) = deriveKeyLegacy(convId)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key, iv)
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                PREFIX_LEGACY + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            } catch (_: Exception) {
                plainText
            }
        }
    }

    /**
     * Decrypts encrypted message with full support for AES-256-GCM and legacy CBC
     */
    fun decrypt(cipherText: String, convId: String): String {
        if (!cipherText.startsWith(PREFIX_LEGACY)) return cipherText

        // 1. New AES-256-GCM format
        if (cipherText.startsWith(PREFIX_GCM)) {
            try {
                val parts = cipherText.removePrefix(PREFIX_GCM).split("::")
                if (parts.size == 2) {
                    val iv = Base64.decode(parts[0], Base64.DEFAULT)
                    val cipherBytes = Base64.decode(parts[1], Base64.DEFAULT)
                    val key = deriveKey256(convId)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_LENGTH, iv))
                    val decrypted = cipher.doFinal(cipherBytes)
                    return String(decrypted, Charsets.UTF_8)
                }
            } catch (_: Exception) {}
        }

        // 2. Legacy CBC decryption (guarantees existing chat history remains 100% accessible)
        val clean = cipherText.removePrefix(PREFIX_LEGACY)
        val decodedBytes = try {
            Base64.decode(clean, Base64.DEFAULT)
        } catch (_: Exception) {
            return cipherText
        }

        // Try universal legacy key
        try {
            val (key, iv) = deriveUniversalKeyLegacy()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        // Try conversation-specific legacy key
        try {
            val (key, iv) = deriveKeyLegacy(convId)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        return cipherText
    }
}
