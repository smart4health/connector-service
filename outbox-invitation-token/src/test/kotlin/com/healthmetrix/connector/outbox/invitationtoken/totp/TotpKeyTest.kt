package com.healthmetrix.connector.outbox.invitationtoken.totp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.crypto.spec.SecretKeySpec

class TotpKeyTest {

    private val key = SecretKeySpec(
        byteArrayOf(0x01, 0x02),
        ALGORITHM,
    )
    private val asString = "0102"
    private val underTest = TotpKey(key)

    @Test
    fun `totp key toString() works`() {
        assertThat(underTest.toString()).isEqualTo(asString)
    }

    @Test
    fun `totp from() works`() {
        assertThat(
            TotpKey.from(
                asString,
            ),
        ).isEqualTo(underTest)
    }

    @Test
    fun `totp random round trip works`() {
        val totp = TotpKey.random()
        assertThat(TotpKey.from(totp.toString())).isEqualTo(totp)
    }

    @Test
    fun `ensure different keys are not equal`() {
        assertThat(TotpKey.random()).isNotEqualTo(
            TotpKey.random(),
        )
    }

    @Test
    fun `ensure pin output length matches input length`() {
        val full = underTest.pin(t = 1000, len = 8)
        assertThat(full.length).isEqualTo(8)

        with(underTest.pin(t = 1000, len = 7)) {
            assertThat(length).isEqualTo(7)
            assertThat(full).endsWith(this)
        }

        with(underTest.pin(t = 1000, len = 6)) {
            assertThat(length).isEqualTo(6)
            assertThat(full).endsWith(this)
        }
    }

    @Test
    fun `ensure that pin outputs change with t`() {
        assertThat(underTest.pin(t = 1000)).isNotEqualTo(underTest.pin(t = 1001))
    }

    @Test
    fun `ensure that pin outputs change with key`() {
        val other = TotpKey.random()
        assertThat(underTest.pin(t = 1000)).isNotEqualTo(other.pin(t = 1000))
    }

    @Test
    fun `ensure that pin outputs are the same with different objects but same key`() {
        val a = underTest
        val b = TotpKey(key)

        assertThat(a.pin(t = 1000)).isEqualTo(b.pin(t = 1000))
    }
}
