package com.healthmetrix.connector.outbox.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrapError
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.config.SmsFactory
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import com.healthmetrix.connector.outbox.invitationtoken.TimeFrame
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceRepository
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.sms.SmsService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

class SendPinUseCaseTest {
    private val phoneNumber = "call me!"
    private val pin = "111111"
    private val caseId = InternalCaseId.randomUUID()
    private val invitationToken = spyk(InvitationToken(caseId, phoneNumber, Bcp47LanguageTag(Locale.US))) {
        every { pins(any(), any(), any()) } returns listOf(pin)
    }

    private val encrypted = B64String("encrytedToken")

    private val encryptionKey: LazySecret<AesKey> = mockk() {
        every { requiredValue } returns mockk()
    }

    private val case = CaseEntity(
        invitationToken.caseId,
        CaseEntity.Status.INVITATION_SENT,
        B64String(""),
        Bcp47LanguageTag(Locale.GERMAN),
    )
    private val caseRepository: CaseRepository = mockk() {
        every { save(any()) } answers { firstArg() }
        every { findById(any()) } returns case
    }
    private val caseNonce = CaseNonceEntity(invitationToken.caseId, 1)
    private val caseNonceRepository: CaseNonceRepository = mockk() {
        every { findByIdAndNonce(any(), any()) } returns caseNonce
    }

    private val smsService: SmsService = mockk() {
        every { sendSms(any()) } returns Ok(Unit)
    }

    private val timeFrame = TimeFrame(num = 5)
    private val senderName = "Healthmetrix"
    private val smsFactory = SmsFactory(senderName, mapOf("en-US" to "%s", "de-DE" to "%s"))
    private val underTest =
        SendPinUseCase(encryptionKey, smsService, caseRepository, timeFrame, smsFactory, caseNonceRepository)

    @BeforeEach
    internal fun setUp() {
        mockkObject(InvitationToken)
        every { InvitationToken.decrypt(any(), any()) } returns invitationToken
    }

    @Test
    fun `happy path results in SUCCESS`() {
        val result = underTest.invoke(encrypted)

        assertThat(result).isInstanceOf(Ok::class.java)
    }

    @Test
    fun `happy path should send sms`() {
        underTest.invoke(encrypted)

        verify(exactly = 1) {
            smsService.sendSms(
                match {
                    it.text.contains(pin) &&
                        it.destNumber == phoneNumber &&
                        it.srcName == senderName
                },
            )
        }
    }

    @Test
    fun `happy path should save case with status PIN_SENT`() {
        underTest.invoke(encrypted)

        verify(exactly = 1) { caseRepository.save(case.copy(status = CaseEntity.Status.PIN_SENT)) }
    }

    @Test
    fun `invalid invitation token results in INVALID_INVITATION_TOKEN`() {
        every { InvitationToken.decrypt(any(), any()) } returns null

        val result = underTest.invoke(encrypted).unwrapError()

        assertThat(result).isEqualTo(SendPinUseCase.SendPinError.MALFORMED_INVITATION_TOKEN)
    }

    @Test
    fun `invalid invitation token should not send sms`() {
        every { InvitationToken.decrypt(any(), any()) } returns null

        underTest.invoke(encrypted)

        verify(exactly = 0) { smsService.sendSms(any()) }
    }

    @Test
    fun `invalid invitation token should not update status of case`() {
        every { InvitationToken.decrypt(any(), any()) } returns null

        underTest.invoke(encrypted)

        verify(exactly = 0) { caseRepository.save(any()) }
    }

    @Test
    fun `failing to find the case results in INVALID_CASE_ID`() {
        every { caseRepository.findById(any()) } returns null

        val result = underTest.invoke(encrypted).unwrapError()

        assertThat(result).isEqualTo(SendPinUseCase.SendPinError.INVALID_CASE_ID)
    }

    @Test
    fun `failing to find the case should not send sms`() {
        every { caseRepository.findById(any()) } returns null

        underTest.invoke(encrypted)

        verify(exactly = 0) { smsService.sendSms(any()) }
    }

    @Test
    fun `failing to find the case should not update status of case`() {
        every { caseRepository.findById(any()) } returns null

        underTest.invoke(encrypted)

        verify(exactly = 0) { caseRepository.save(any()) }
    }

    @Test
    fun `failing to send sms results in SMS_ERROR`() {
        every { smsService.sendSms(any()) } returns Err(Exception(""))

        val result = underTest.invoke(encrypted).unwrapError()

        assertThat(result).isEqualTo(SendPinUseCase.SendPinError.SMS_ERROR)
    }

    @Test
    fun `failing to send sms should not update status of case`() {
        every { smsService.sendSms(any()) } returns Err(Exception(""))

        underTest.invoke(encrypted)

        verify(exactly = 0) { caseRepository.save(any()) }
    }

    @Test
    fun `sending the pin from an invalid status results in INVALID_STATUS`() {
        every { caseRepository.findById(any()) } returns case.copy(status = CaseEntity.Status.UNPAIRED)

        val result = underTest.invoke(encrypted).unwrapError()

        assertThat(result).isEqualTo(SendPinUseCase.SendPinError.INVALID_STATUS)
    }

    @Test
    fun `sending the pin from an invalid status should not send sms`() {
        every { caseRepository.findById(any()) } returns case.copy(status = CaseEntity.Status.UNPAIRED)

        underTest.invoke(encrypted)

        verify(exactly = 0) { smsService.sendSms(any()) }
    }

    @Test
    fun `sending the pin from an invalid status should not update status of case`() {
        every { caseRepository.findById(any()) } returns case.copy(status = CaseEntity.Status.UNPAIRED)

        underTest.invoke(encrypted)

        verify(exactly = 0) { caseRepository.save(any()) }
    }

    @Test
    fun `sending the pin from the PIN_SUCCEEDED status should succeed`() {
        every { caseRepository.findById(any()) } returns case.copy(status = CaseEntity.Status.PIN_SUCCEEDED)

        underTest.invoke(encrypted)

        verify(exactly = 1) { caseRepository.save(case.copy(status = CaseEntity.Status.PIN_SENT)) }
    }

    @Test
    fun `invitation token with a non-matching nonce results in EXPIRED_INVITATION_TOKEN`() {
        every { caseNonceRepository.findByIdAndNonce(any(), any()) } returns null

        val result = underTest.invoke(encrypted).unwrapError()

        assertThat(result).isEqualTo(SendPinUseCase.SendPinError.EXPIRED_INVITATION_TOKEN)
    }

    @Test
    fun `invitation token with a non-matching nonce should not send sms`() {
        every { caseNonceRepository.findByIdAndNonce(any(), any()) } returns null

        underTest.invoke(encrypted)

        verify(exactly = 0) { smsService.sendSms(any()) }
    }

    @Test
    fun `invitation token with a non-matching nonce should not update status of case`() {
        every { caseNonceRepository.findByIdAndNonce(any(), any()) } returns null

        underTest.invoke(encrypted)

        verify(exactly = 0) { caseRepository.save(any()) }
    }
}
