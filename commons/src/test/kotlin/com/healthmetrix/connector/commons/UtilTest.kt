package com.healthmetrix.connector.commons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class UtilTest {

    @Test
    fun `hex encoding roundtrips successfully`() {
        val buf = "hello world".toByteArray(Charsets.UTF_8)
        assertThat(buf.encodeHex().decodeHex()).isEqualTo(buf)
    }

    @Test
    fun `base64 encoding roundtrips successfully`() {
        val buf = "this is a slightly longer message".toByteArray(Charsets.UTF_8)
        assertThat(buf.encodeBase64().decode()).isEqualTo(buf)
    }

    @Test
    fun `url encoding roundtrips successfully`() {
        val message = "unsuitable/for/urls"
        assertThat(message.encodeURL().decodeURL()).isEqualTo(message)
    }

    @Test
    fun `different messages do not have the same encodings`() {
        val a = "hello world"
        val b = "goodbye"

        assertThat(a.toByteArray(Charsets.UTF_8).encodeHex())
            .isNotEqualTo(b.toByteArray(Charsets.UTF_8).encodeHex())

        assertThat(a.toByteArray(Charsets.UTF_8).encodeBase64())
            .isNotEqualTo(b.toByteArray(Charsets.UTF_8).encodeBase64())

        assertThat(a.encodeURL())
            .isNotEqualTo(b.encodeURL())
    }
}
