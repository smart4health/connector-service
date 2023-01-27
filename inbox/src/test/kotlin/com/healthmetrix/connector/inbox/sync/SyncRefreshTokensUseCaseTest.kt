package com.healthmetrix.connector.inbox.sync

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.outbox.Outbox
import com.healthmetrix.connector.inbox.outbox.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class SyncRefreshTokensUseCaseTest {
    private val mockRefreshTokenRepository: RefreshTokenRepository = mockk() {
        every { saveAll(any()) } answers { arg(0) }
    }
    private val mockOutbox: Outbox = mockk() {
        every { deleteRefreshTokens(any()) } answers { arg(0) }
    }

    private val underTest = SyncRefreshTokensUseCase(
        mockOutbox,
        mockRefreshTokenRepository,
    )

    @Test
    fun `gotten auth codes are exchanged and stored in the repository`() {
        every { mockOutbox.getRefreshTokens() } returns listOf(
            RefreshToken(InternalCaseId.randomUUID(), "asdf"),
            RefreshToken(InternalCaseId.randomUUID(), "hello"),
        )

        underTest.invoke()

        verify { mockRefreshTokenRepository.saveAll(match { it.size == 2 }) }
        verify { mockOutbox.deleteRefreshTokens(match { it.size == 2 }) }
    }

    @Test
    fun `when no auth codes are gotten, no refresh tokens are stored`() {
        every { mockOutbox.getRefreshTokens() } returns listOf()

        underTest.invoke()

        verify { mockRefreshTokenRepository.saveAll(match { it.isEmpty() }) }
    }
}
