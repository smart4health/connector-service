package com.healthmetrix.connector.inbox.sync

import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.inbox.oauth.OauthClient
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID

class RenewRefreshTokensUseCaseTest {

    private val mockRefreshTokenRepository: RefreshTokenRepository = mockk {
        every { save(any()) } answers { arg(0) }
    }

    private val mockOauthClient: OauthClient = mockk()

    private val underTest = RenewRefreshTokensUseCase(
        refreshTokenRepository = mockRefreshTokenRepository,
        oauthClient = mockOauthClient,
        refreshTokenRefreshAfter = Duration.ofDays(1),
        inMemoryBackoffFilter = InMemoryBackoffFilter(
            minDeltaT = Duration.ofSeconds(1),
            maxDeltaT = Duration.ofSeconds(10),
            clock = { Instant.now() },
        ),
    )

    @Test
    fun `refreshing with zero tokens does nothing`() {
        every { mockRefreshTokenRepository.findFetchedAtBefore(any()) } returns listOf()

        underTest()

        verify(exactly = 0) { mockOauthClient.exchangeRefreshTokenForAccessToken(any()) }
    }

    @Test
    fun `refreshing with one token calls exchangeRefreshTokenForAccessToken`() {
        val oldToken = "asdf"

        every { mockRefreshTokenRepository.findFetchedAtBefore(any()) } returns listOf(
            RefreshToken(
                UUID.randomUUID(),
                oldToken,
                null,
            ),
        )

        every { mockOauthClient.exchangeRefreshTokenForAccessToken(any()) } returns Ok(
            OauthClient.TokenPair(
                "access",
                "newRefresh",
            ),
        )

        underTest()

        verify(exactly = 1) { mockOauthClient.exchangeRefreshTokenForAccessToken(oldToken) }
    }
}
