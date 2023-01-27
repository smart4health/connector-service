package com.healthmetrix.connector.inbox.upload

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.oauth.OauthClient
import com.healthmetrix.connector.inbox.persistence.domainresources.UploadableResource
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class AccessTokenMapperTest {

    private val refreshTokenRepository: RefreshTokenRepository = mockk {
        every { save(any()) } answers { arg(0) }
    }

    private val uploadAttemptRepository: UploadAttemptRepository = mockk {
        every { save(any()) } answers { arg(0) }
    }

    private val oauthClient: OauthClient = mockk()

    private val underTest = AccessTokenMapper(
        refreshTokenRepository,
        uploadAttemptRepository,
        oauthClient,
    )

    private val document1 =
        UploadableResource(
            internalCaseId = InternalCaseId.randomUUID(),
            internalResourceId = UUID.randomUUID(),
            refreshToken = "refresh token",
            privateKeyPemString = "i am a valid pem string",
        )

    private val entry = mapOf(
        UploadDocumentsUseCase.GroupedCaseInfo(document1) to listOf(
            UploadDocumentsUseCase.GroupedResource(document1),
        ),
    ).entries.toList().first()

    @Test
    fun `access tokens are mapped to document`() {
        every { oauthClient.exchangeRefreshTokenForAccessToken(any()) } answers {
            Ok(OauthClient.TokenPair("access token", arg(0)))
        }

        val res = underTest(entry)

        assertThat(res).hasSize(1)
        assertThat(res.first()).matches {
            it.accessToken == "access token"
        }
    }

    @Test
    fun `failure to get access token results in empty list`() {
        every { oauthClient.exchangeRefreshTokenForAccessToken(any()) } returns Err(OauthClient.Error.InvalidGrant)

        val res = underTest(entry)

        assertThat(res).hasSize(0)
    }

    @Test
    fun `new refresh tokens are saved to the database`() {
        every { oauthClient.exchangeRefreshTokenForAccessToken(any()) } answers {
            Ok(OauthClient.TokenPair("access", arg(0)))
        }

        underTest(entry)

        verify { refreshTokenRepository.save(any()) }
    }
}
