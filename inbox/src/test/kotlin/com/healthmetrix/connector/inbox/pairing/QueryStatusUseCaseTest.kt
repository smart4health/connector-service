package com.healthmetrix.connector.inbox.pairing

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class QueryStatusUseCaseTest {

    private val mockCaseRepository: CaseRepository = mock()
    private val mockRefreshTokenRepository: RefreshTokenRepository = mock()
    private val fakeCaseEntity = CaseEntity(
        UUID.randomUUID(),
        "random",
        B64String("private"),
    )
    private val fakeRefreshTokenEntity =
        RefreshToken(
            fakeCaseEntity.internalCaseId,
            "refresh",
            null,
        )

    private val fakeCaseEntityNotPaired = CaseEntity(
        UUID.randomUUID(),
        "random2",
        B64String("private2"),
    )

    private val underTest = QueryStatusUseCase(mockCaseRepository, mockRefreshTokenRepository, 10)

    @Test
    fun `querying a case that does not exist in the case repository returns NO_CASE_ID`() {
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf()

        assertThat(underTest("id")).isEqualTo(QueryStatusUseCase.Result.NO_CASE_ID)
    }

    @Test
    fun `querying a case that exists but has no refresh token returns NOT_PAIRED`() {
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf(fakeCaseEntity)
        whenever(mockRefreshTokenRepository.findByIdOrNull(any())) doReturn null

        assertThat(underTest("id")).isEqualTo(QueryStatusUseCase.Result.NOT_PAIRED)
    }

    @Test
    fun `query a case that exists and has a refresh token returns PAIRED`() {
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf(fakeCaseEntity)
        whenever(mockRefreshTokenRepository.findByIdOrNull(any())) doReturn fakeRefreshTokenEntity

        assertThat(underTest("id")).isEqualTo(QueryStatusUseCase.Result.PAIRED)
    }

    @Test
    fun `querying multiple ids returns a map of successes and failures`() {
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf(fakeCaseEntity, fakeCaseEntityNotPaired)
        whenever(mockRefreshTokenRepository.findByIds(any())) doReturn listOf(fakeRefreshTokenEntity)

        val actual = underTest(
            listOf(
                "no case id",
                fakeCaseEntity.externalCaseId,
                fakeCaseEntityNotPaired.externalCaseId,
            ),
        )
        val expected = hashMapOf(
            "no case id" to QueryStatusUseCase.Result.NO_CASE_ID,
            fakeCaseEntityNotPaired.externalCaseId to QueryStatusUseCase.Result.NOT_PAIRED,
            fakeCaseEntity.externalCaseId to QueryStatusUseCase.Result.PAIRED,
        ) to hashMapOf(
            QueryStatusUseCase.Result.NO_CASE_ID to 1,
            QueryStatusUseCase.Result.NOT_PAIRED to 1,
            QueryStatusUseCase.Result.PAIRED to 1,
        )

        assertThat(actual).isEqualTo(expected)
    }
}
