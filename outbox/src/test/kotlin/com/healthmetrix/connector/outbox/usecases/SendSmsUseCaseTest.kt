package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SendSmsUseCaseTest {
    private val encryptionKey: LazySecret<AesKey> = mockk() {
        every { requiredValue } returns mockk()
    }

    private val last4Digits = "1111"
    private val phoneNumber = "I'm a real phone number. call me.$last4Digits"
    private val fakeToken: InvitationToken = mockk() {
        every { phone } returns phoneNumber
    }

    private val encryptedToken = "I'm an encrypted token!".toByteArray().encodeBase64()

    private val underTest = SendSmsUseCase(encryptionKey)

    @BeforeEach
    internal fun setUp() {
        mockkObject(InvitationToken)
    }

    @Test
    fun `returns success result with stripped phone number`() {
        every { InvitationToken.decrypt(any(), any()) } returns fakeToken

        val result = underTest.invoke(encryptedToken)

        assertThat(result).isEqualTo(SendSmsUseCase.Result.Success(last4Digits))
    }

    @Test
    fun `returns InvalidToken result if invalid token is provided`() {
        every { InvitationToken.decrypt(any(), any()) } returns null

        val result = underTest.invoke(encryptedToken)

        assertThat(result).isEqualTo(SendSmsUseCase.Result.InvalidToken)
    }
}
