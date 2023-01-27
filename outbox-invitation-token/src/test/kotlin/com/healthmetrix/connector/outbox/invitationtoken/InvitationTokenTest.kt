package com.healthmetrix.connector.outbox.invitationtoken

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.outbox.invitationtoken.totp.TotpKey
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.Locale

class InvitationTokenTest {

    @Nested
    @DisplayName("InvitationToken")
    inner class InvitationTokenTests {

        private val internalUUID: InternalCaseId = InternalCaseId.randomUUID()
        private val phone = "123456789"
        private val totpSeed = TotpKey.random()

        private val timeFrame = TimeFrame(num = 5)

        private val underTest = InvitationToken(internalUUID, phone, totpSeed, 1, Bcp47LanguageTag(Locale.GERMANY))

        @Test
        fun `should serialize to json string correctly`() {
            val res = Json.encodeToString(InvitationToken.serializer(), underTest)

            assertThat(res).contains(internalUUID.toString(), phone, totpSeed.toString(), "1", "de-DE")
        }

        @Test
        fun `kotlinx serialization should round trip correctly`() {
            val string = Json.encodeToString(InvitationToken.serializer(), underTest)
            val after = Json.decodeFromString(InvitationToken.serializer(), string)

            assertThat(underTest).isEqualTo(after)
        }

        @Test
        fun `encryption should round trip correctly`() {
            val aesKey: AesKey = mockk() {
                every { encrypt(any()) } returns "encrypted".toByteArray()
                every { decrypt("encrypted".toByteArray()) } returns Json.encodeToString(InvitationToken.serializer(), underTest).toByteArray(Charsets.UTF_8)
            }

            val encryptedToken = InvitationToken.encrypt(underTest, aesKey)
            val after = InvitationToken.decrypt(encryptedToken, aesKey)

            assertThat(underTest).isEqualTo(after)
        }

        @Test
        fun `pins should create 5 tokens`() {
            val result = underTest.pins(timeFrame, now = System.currentTimeMillis())

            assertThat(result.count()).isEqualTo(5)
        }

        @Test
        fun `last pin should be invalid after 30 seconds`() {
            val now = Instant.now()
            val initialPins = underTest.pins(timeFrame, now = now.toEpochMilli())

            val thirtySecondsLater = now.plusSeconds(30)
            val pinsAfter30Seconds = underTest.pins(timeFrame, now = thirtySecondsLater.toEpochMilli())

            assertThat(pinsAfter30Seconds).doesNotContain(initialPins.last())
        }

        @Test
        fun `every pin but the last should still be valid after 30 seconds`() {
            val now = Instant.now()
            val initialPins = underTest.pins(timeFrame, now = now.toEpochMilli())

            val thirtySecondsLater = now.plusSeconds(30)
            val pinsAfter30Seconds = underTest.pins(timeFrame, now = thirtySecondsLater.toEpochMilli())

            initialPins.dropLast(1).forEach { pin -> assertThat(pinsAfter30Seconds).contains(pin) }
        }
    }

    @Nested
    @DisplayName("TimeFrame")
    inner class TimeFrameTests {

        @Test
        fun `windows should generate correct time windows inside of a window`() {
            val timeFrame = TimeFrame(
                duration = Duration.ofSeconds(30),
                num = 3,
            )
            val now = Duration.ofSeconds(30 * 5 + 15).toMillis()
            val windows = timeFrame.windows(now)

            val expected = listOf(
                Duration.ofSeconds(30 * 5),
                Duration.ofSeconds(30 * 4),
                Duration.ofSeconds(30 * 3),
            ).map(Duration::toMillis)

            assertThat(windows).isEqualTo(expected)
        }

        @Test
        fun `windows should generate the correct time windows at boundaries`() {
            val now = Duration.ofMinutes(10)
            val timeFrame = TimeFrame(
                duration = Duration.ofSeconds(30),
                num = 3,
            )
            val expected = listOf(
                now,
                now - Duration.ofSeconds(30),
                now - Duration.ofSeconds(60),
            ).map(Duration::toMillis)

            assertThat(timeFrame.windows(now.toMillis())).isEqualTo(expected)
        }
    }
}
