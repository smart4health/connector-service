package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.outbox.persistence.RefreshToken
import com.healthmetrix.connector.outbox.persistence.RefreshTokenRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class GetRefreshTokensUseCaseTest {
    private val mockRefreshTokenRepository: RefreshTokenRepository = mock()
    private val underTest = GetRefreshTokensUseCase(mockRefreshTokenRepository)

    @Test
    fun `happy path results in list of all keys and does not delete them`() {
        val keys = listOf(
            RefreshToken(InternalCaseId.randomUUID(), "auth code"),
        )

        whenever(mockRefreshTokenRepository.findAll()).thenReturn(keys.toMutableList())

        val result = underTest.invoke()

        result.zip(keys).forEach { (code, entity) ->
            assertThat(code.internalCaseId).isEqualTo(entity.internalCaseId)
            assertThat(code.refreshToken).isEqualTo(entity.value)
        }
        verify(mockRefreshTokenRepository, never()).delete(any())
    }
}
