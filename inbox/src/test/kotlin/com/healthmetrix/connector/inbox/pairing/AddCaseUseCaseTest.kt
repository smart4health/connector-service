package com.healthmetrix.connector.inbox.pairing

import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.inbox.outbox.AddCaseResult
import com.healthmetrix.connector.inbox.outbox.Outbox
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

class AddCaseUseCaseTest {

    private val externalCaseId = "developer one"
    private val contentBytes = "content".toByteArray()
    private val contentType = MediaType.APPLICATION_JSON_VALUE

    private val mockCaseRepository: CaseRepository = mock()
    private val mockRefreshTokenRepository: RefreshTokenRepository = mock()
    private val mockOutbox: Outbox = mock()
    private val aesKey: AesKey = mock()
    private val encryptionKey: LazySecret<AesKey> = mock() {
        on { it.requiredValue }.thenReturn(aesKey)
    }

    private val underTest = AddCaseUseCase(mockCaseRepository, mockRefreshTokenRepository, mockOutbox, encryptionKey)

    @Test
    fun `adding a new case returns SUCCESS_CREATED`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn("encrypted".toByteArray())

        assertThat(underTest(externalCaseId, contentBytes, contentType)).isEqualTo(AddCaseResult.SuccessCreated)
    }

    @Test
    fun `adding a new case saves case`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn("encrypted".toByteArray())

        underTest(externalCaseId, contentBytes, contentType)

        verify(mockCaseRepository).save(any())
    }

    @Test
    fun `adding a new case removes refresh tokens`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())) doReturn AddCaseResult.SuccessCreated
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf()
        whenever(aesKey.encrypt(any())) doReturn "encrypted".toByteArray()

        underTest(externalCaseId, contentBytes, contentType)

        verify(mockRefreshTokenRepository).deleteById(any())
    }

    @Test
    fun `adding an existing case returns SUCCESS_OVERRIDDEN`() {
        whenever(
            mockOutbox.addCase(
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(AddCaseResult.SuccessOverridden)
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn("encrypted".toByteArray())

        assertThat(underTest(externalCaseId, contentBytes, contentType)).isEqualTo(AddCaseResult.SuccessOverridden)
    }

    @Test
    fun `error reason from outbox is passed back`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())).thenReturn(AddCaseResult.Error("reason"))
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn("encrypted".toByteArray())

        assertThat(underTest(externalCaseId, contentBytes, contentType)).isEqualTo(AddCaseResult.Error("reason"))
    }

    @Test
    fun `case is not saved on error from outbox`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())).thenReturn(AddCaseResult.Error("reason"))
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn("encrypted".toByteArray())

        verify(mockCaseRepository, never()).save(any())
    }

    @Test
    fun `error should be returned when encrypting the private key fails`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())).thenReturn(AddCaseResult.Error("reason"))
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn("encrypted".toByteArray())

        assertThat(underTest(externalCaseId, contentBytes, contentType)).isEqualTo(AddCaseResult.Error("reason"))
    }

    @Test
    fun `case is not saved when encrypting the private key fails`() {
        whenever(mockOutbox.addCase(any(), any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)
        whenever(mockCaseRepository.findByExternalCaseId(any())).thenReturn(listOf())
        whenever(aesKey.encrypt(any())).thenReturn(null)

        verify(mockCaseRepository, never()).save(any())
    }
}
