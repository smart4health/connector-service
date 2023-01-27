package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.outbox.oauth.OauthClient
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.persistence.OauthStateEntity
import com.healthmetrix.connector.outbox.persistence.OauthStateRepository
import com.healthmetrix.connector.outbox.persistence.RefreshToken
import com.healthmetrix.connector.outbox.persistence.RefreshTokenRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

class OauthSuccessUseCaseTest {
    private val caseId: InternalCaseId = InternalCaseId.randomUUID()
    private val oauthStateRepository: OauthStateRepository = mockk()
    private val caseRepository: CaseRepository = mockk()
    private val refreshTokenRepository: RefreshTokenRepository = mockk()
    private val oauthClient: OauthClient = mockk()

    private val oauthState = OauthStateEntity(caseId, "fake status")
    private val refreshToken = RefreshToken(caseId, "le refresh token")

    private val underTest =
        OauthSuccessUseCase(caseRepository, refreshTokenRepository, oauthStateRepository, oauthClient)

    @Nested
    inner class SuccessResult {
        private val case =
            CaseEntity(caseId, CaseEntity.Status.PIN_SUCCEEDED, B64String("pub key"), Bcp47LanguageTag(Locale.GERMAN))

        @BeforeEach
        internal fun setup() {
            every { oauthStateRepository.findByState(any()) } returns listOf(oauthState)
            every { caseRepository.findById(any()) } returns case
            every { oauthClient.getRefreshToken(any()) } returns "le refresh token"
            every { refreshTokenRepository.save(any()) } answers { firstArg() }
            every { caseRepository.save(any()) } answers { firstArg() }
        }

        @Test
        fun `returns success result`() {
            val result = underTest("le state", "le auth code")
            assertThat(result)
                .isInstanceOf(OauthSuccessUseCase.Result.Success::class.java)
            assertThat((result as OauthSuccessUseCase.Result.Success).lang).isEqualTo(Bcp47LanguageTag(Locale.GERMAN))
        }

        @Test
        fun `success result creates refresh token`() {
            underTest("le state", "le auth code")
            verify(exactly = 1) { refreshTokenRepository.save(refreshToken) }
        }

        @Test
        fun `success result updates case status`() {
            underTest("le state", "le auth code")
            verify(exactly = 1) { caseRepository.save(case.copy(status = CaseEntity.Status.OAUTH_SUCCEEDED)) }
        }
    }

    @Test
    fun `returns OauthStateNotFound result when no oauth state found`() {
        every { oauthStateRepository.findByState(any()) } returns listOf()
        assertThat(underTest("le state", "le auth code"))
            .isEqualTo(OauthSuccessUseCase.Result.OauthStateNotFound)
    }

    @Test
    fun `returns InvalidCaseStatus when case cannot be found`() {
        every { oauthStateRepository.findByState(any()) } returns listOf(oauthState)
        every { caseRepository.findById(any()) } returns null
        assertThat(underTest("le state", "le auth code"))
            .isEqualTo(OauthSuccessUseCase.Result.InvalidCaseStatus)
    }

    @Test
    fun `returns InvalidCaseStatus result when case status is not PIN_SUCCEEDED`() {
        val case =
            CaseEntity(caseId, CaseEntity.Status.INVITATION_SENT, B64String("pub key"), Bcp47LanguageTag(Locale.GERMAN))
        every { oauthStateRepository.findByState(any()) } returns listOf(oauthState)
        every { caseRepository.findById(any()) } returns case
        assertThat(underTest("le state", "le auth code"))
            .isEqualTo(OauthSuccessUseCase.Result.InvalidCaseStatus)
    }

    @Test
    fun `results OauthRefreshFailed result when oauth client returns null`() {
        val case =
            CaseEntity(caseId, CaseEntity.Status.PIN_SUCCEEDED, B64String("pub key"), Bcp47LanguageTag(Locale.GERMAN))
        every { oauthStateRepository.findByState(any()) } returns listOf(oauthState)
        every { caseRepository.findById(any()) } returns case
        every { oauthClient.getRefreshToken(any()) } returns null

        assertThat(underTest("le state", "le auth code"))
            .isEqualTo(OauthSuccessUseCase.Result.OauthRefreshFailed)
    }
}
