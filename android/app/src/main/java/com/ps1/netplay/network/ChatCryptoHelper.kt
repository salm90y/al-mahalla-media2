package com.ps1.netplay.network

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object ChatCryptoHelper {
    private const val PREFIX = "ENC::"

    private fun normalizeConvId(convId: String): String {
        return convId.split("_").map { it.trim().lowercase() }.sorted().joinToString("_")
    }

    private fun deriveKey(convId: String): Pair<SecretKeySpec, IvParameterSpec> {
        val norm = normalizeConvId(convId)
        val sha = MessageDigest.getInstance("SHA-256").digest(("ps1_chat_salt_" + norm).toByteArray(Charsets.UTF_8))
        val keyBytes = sha.copyOfRange(0, 16) // 128-bit key
        val ivBytes = sha.copyOfRange(16, 32)  // 128-bit IV
        return Pair(SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
    }

    private fun deriveUniversalKey(): Pair<SecretKeySpec, IvParameterSpec> {
        val sha = MessageDigest.getInstance("SHA-256").digest("ps1_chat_universal_v1_salt".toByteArray(Charsets.UTF_8))
        val keyBytes = sha.copyOfRange(0, 16)
        val ivBytes = sha.copyOfRange(16, 32)
        return Pair(SecretKeySpec(keyBytes, "AES"), IvParameterSpec(ivBytes))
    }

    /**
     * Encrypts plain text message for Cloudflare D1/R2 storage
     */
    fun encrypt(plainText: String, convId: String): String {
        if (plainText.isEmpty()) return ""
        return try {
            val (key, iv) = deriveUniversalKey()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, key, iv)
            val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            PREFIX + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
        } catch (e: Exception) {
            try {
                val (key, iv) = deriveKey(convId)
                val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                cipher.init(Cipher.ENCRYPT_MODE, key, iv)
                val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
                PREFIX + Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)
            } catch (e2: Exception) {
                plainText
            }
        }
    }

    /**
     * Decrypts encrypted message retrieved from Cloudflare
     */
    fun decrypt(cipherText: String, convId: String): String {
        if (!cipherText.startsWith(PREFIX)) return cipherText
        val clean = cipherText.removePrefix(PREFIX)
        val decodedBytes = try {
            Base64.decode(clean, Base64.DEFAULT)
        } catch (e: Exception) {
            return cipherText
        }

        // 1. Try Universal Key first
        try {
            val (key, iv) = deriveUniversalKey()
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        // 2. Try Conversation-specific key
        try {
            val (key, iv) = deriveKey(convId)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, key, iv)
            val decryptedBytes = cipher.doFinal(decodedBytes)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {}

        return cipherText
    }
}
