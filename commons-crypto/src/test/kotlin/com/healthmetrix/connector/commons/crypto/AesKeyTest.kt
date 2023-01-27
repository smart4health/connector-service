package com.healthmetrix.connector.commons.crypto

import com.healthmetrix.connector.commons.encodeBase64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import javax.crypto.spec.SecretKeySpec

class AesKeyTest {
    private val secretKeySpec = SecretKeySpec(ByteArray(16), "AES")
    private val underTest = AesKey(secretKeySpec)

    @Test
    fun `encryption roundtrip is successful`() {
        val message = "hello world"
        val encrypted = underTest.encrypt(message.toByteArray(Charsets.UTF_8))
        val result = underTest.decrypt(encrypted)?.toString(Charsets.UTF_8)

        assertThat(result).isEqualTo(message)
    }

    @Test
    fun `encryption key with 16 bytes should be valid`() {
        val result = AesKey.buildFromB64String(aesKeyB64String(16))

        assertThat(result).isInstanceOf(AesKey::class.java)
    }

    @Test
    fun `encryption key with 17 bytes should throw InvalidAesKeyException`() {
        assertThrows<InvalidAesKeyException> { AesKey.buildFromB64String(aesKeyB64String(17)) }
    }

    @Test
    fun `encryption key with 0 bytes should throw InvalidAesKeyException`() {
        assertThrows<InvalidAesKeyException> { AesKey.buildFromB64String(aesKeyB64String(0)) }
    }

    @Test
    fun `encryption key from non valid base64 should throw InvalidAesKeyException`() {
        assertThrows<InvalidAesKeyException> { AesKey.buildFromB64String(aesKeyB64String(15) + "X") }
    }

    private fun aesKeyB64String(size: Int): String = ByteArray(size).encodeBase64().string
}
