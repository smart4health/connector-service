package com.healthmetrix.connector.commons.crypto

import com.healthmetrix.connector.commons.AllOpen
import com.healthmetrix.connector.commons.decodeBase64
import com.healthmetrix.connector.commons.logger
import net.logstash.logback.argument.StructuredArguments
import java.nio.ByteBuffer
import java.security.SecureRandom
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class InvalidAesKeyException : Exception()

@AllOpen
class AesKey(private val aesKey: SecretKeySpec) {

    fun encrypt(input: ByteArray): ByteArray {
        // TODO any relation to the instance in with?
        val iv = with(SecureRandom()) {
            val buf = ByteArray(12)
            nextBytes(buf)
            buf
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        }

        val cipherText = cipher.doFinal(input)

        val msg = with(ByteBuffer.allocate(4 + iv.size + cipherText.size)) {
            putInt(iv.size)
            put(iv)
            put(cipherText)
        }.array()

        Arrays.fill(iv, 0)

        return msg
    }

    fun decrypt(input: ByteArray): ByteArray? = try {
        val (iv, cipherText) = with(ByteBuffer.wrap(input)) {
            val ivLength = int
            if (ivLength !in (12..16)) {
                throw IllegalArgumentException("invalid iv length")
            }

            val iv = ByteArray(ivLength).also { get(it) }
            val cipherText = ByteArray(remaining()).also { get(it) }

            iv to cipherText
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, aesKey, GCMParameterSpec(128, iv))
        }

        cipher.doFinal(cipherText)
    } catch (ex: Exception) {
        logger.warn("Error occurred AES decrypting ByteArray {}", StructuredArguments.kv("ex", ex::class.java.canonicalName))
        null
    }

    companion object {
        fun buildFromB64String(keyB64: String): AesKey {
            return keyB64.decodeBase64()
                .let(this::validateAesKeyBytes)
                .let { keyBytes -> SecretKeySpec(keyBytes, "AES") }
                .let(::AesKey)
        }

        private fun validateAesKeyBytes(keyBytes: ByteArray?): ByteArray {
            if (keyBytes == null) {
                logger.warn("AES key is not in valid base64 encoding")
                throw InvalidAesKeyException()
            }

            if (keyBytes.isEmpty()) {
                logger.warn("AES key size must not be empty")
                throw InvalidAesKeyException()
            }

            if (keyBytes.size > 16) {
                logger.warn("AES key size must not be greater than 16")
                throw InvalidAesKeyException()
            }

            return keyBytes
        }
    }
}
