package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.persistence.OauthStateEntity
import com.healthmetrix.connector.outbox.persistence.OauthStateRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.Locale

internal class OauthErrorUseCaseTest {
    private val caseId: InternalCaseId = InternalCaseId.randomUUID()
    private val oauthStateRepository: OauthStateRepository = mockk()
    private val caseRepository: CaseRepository = mockk()
    private val oauthState = OauthStateEntity(caseId, "fake status")
    private val underTest = OauthErrorUseCase(caseRepository, oauthStateRepository)

    @Nested
    inner class SuccessResult {
        private val case =
            CaseEntity(caseId, CaseEntity.Status.PIN_SUCCEEDED, B64String("pub key"), Bcp47LanguageTag(Locale.GERMAN))

        @BeforeEach
        internal fun setup() {
            every { oauthStateRepository.findByState(any()) } returns listOf(oauthState)
            every { caseRepository.findById(any()) } returns case
        }

        @Test
        fun `returns success result type`() {
            val result = underTest("state")
            assertThat(result).isInstanceOf(OauthErrorUseCase.Result.Success::class.java)
        }

        @Test
        fun `returns the lang`() {
            val result = underTest("state")
            assertThat((result as OauthErrorUseCase.Result.Success).lang).isEqualTo(Bcp47LanguageTag(Locale.GERMAN))
        }
    }

    @Nested
    inner class OauthStateNotFoundResult {
        @BeforeEach
        internal fun setup() {
            every { oauthStateRepository.findByState(any()) } returns listOf()
        }

        @Test
        fun `returns OauthStateNotFound Result`() {
            val result = underTest("state")
            assertThat(result).isInstanceOf(OauthErrorUseCase.Result.OauthStateNotFound::class.java)
        }
    }

    @Nested
    inner class CaseNotFoundResult {
        @BeforeEach
        internal fun setup() {
            every { oauthStateRepository.findByState(any()) } returns listOf(oauthState)
            every { caseRepository.findById(any()) } returns null
        }

        @Test
        fun `returns CaseNotFoundResult`() {
            val result = underTest("state")
            assertThat(result).isInstanceOf(OauthErrorUseCase.Result.CaseNotFound::class.java)
        }
    }
}
