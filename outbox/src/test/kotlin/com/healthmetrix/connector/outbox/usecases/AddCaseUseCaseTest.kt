package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.PhoneValidator
import com.healthmetrix.connector.outbox.config.InvitationEmailFactory
import com.healthmetrix.connector.outbox.email.EmailService
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceRepository
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.usecases.AddCaseUseCase.Result
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.UUID

class AddCaseUseCaseTest {

    private val emailService: EmailService = mockk() {
        every { sendEmail(any()) } returns true
    }
    private val emailFactory: InvitationEmailFactory = mockk() {
        every { make(any(), any(), any()) } returns mockk()
    }

    private val caseRepository: CaseRepository = mockk() {
        every { save(any()) } answers { firstArg() }
        every { findById(any()) } returns null
    }

    private val encryptedToken = "encrypted".toByteArray()
    private val invitationTokenEncryptionKey: LazySecret<AesKey> = mockk() {
        every { requiredValue } returns mockk()
    }

    private val phoneValidator: PhoneValidator = mockk()
    private val caseNonceRepository: CaseNonceRepository = mockk() {
        every { save(any()) } answers { firstArg() }
    }

    private val caseId = UUID.randomUUID()
    private val germanPhone = "8675309"
    private val email = "test@mctestface.de"
    private val pubKey = B64String("pub_key")
    private val defaultLocale = "de-DE"

    private val underTest = AddCaseUseCase(
        emailService,
        emailFactory,
        caseRepository,
        invitationTokenEncryptionKey,
        phoneValidator,
        caseNonceRepository,
        defaultLocale,
    )

    @BeforeEach
    internal fun setUp() {
        mockkObject(InvitationToken)
        every { InvitationToken.encrypt(any(), any()) } returns encryptedToken.encodeBase64()

        // TODO workaround here
        every { phoneValidator.invoke(any(), any(), any()) } returns germanPhone
    }

    @Test
    fun `adding a new case returns SUCCESS_CREATED`() {
        val result = underTest(caseId, germanPhone, email, pubKey, null)

        assertThat(result).isEqualTo(Result.SUCCESS_CREATED)
    }

    @Test
    fun `adding an existing case returns SUCCESS_OVERRIDDEN`() {
        every { caseRepository.findById(any()) } returns CaseEntity(
            caseId,
            B64String(""),
            Bcp47LanguageTag(defaultLocale),
        )

        val result = underTest(caseId, germanPhone, email, pubKey, null)

        assertThat(result).isEqualTo(Result.SUCCESS_OVERRIDDEN)
    }

    @Test
    fun `adding a new case should save case to database`() {
        underTest(caseId, germanPhone, email, pubKey, null)

        verify(exactly = 1) {
            caseRepository.save(
                CaseEntity(
                    caseId,
                    CaseEntity.Status.INVITATION_SENT,
                    pubKey,
                    Bcp47LanguageTag(
                        defaultLocale,
                    ),
                ),
            )
        }
    }

    @Test
    fun `adding a new case should save caseNonce to database`() {
        underTest(caseId, germanPhone, email, pubKey, null)

        verify(exactly = 1) { caseNonceRepository.save(match { it.internalCaseId == caseId }) }
    }

    @Test
    fun `adding a case should send email with encrypted invitation token`() {
        underTest(caseId, germanPhone, email, pubKey, null)

        verify { emailFactory.make(email, encryptedToken.encodeBase64(), Bcp47LanguageTag(defaultLocale)) }
        verify(exactly = 1) { emailService.sendEmail(any()) }
    }

    @Test
    fun `invitation token has default locale`() {
        underTest(caseId, germanPhone, email, pubKey, null)

        verify { InvitationToken.encrypt(match { it.locale.toString() == defaultLocale }, any()) }
    }

    @Test
    fun `invitation token has set locale`() {
        val setLocale = Locale.ENGLISH.toLanguageTag()
        underTest(caseId, germanPhone, email, pubKey, setLocale)

        verify { InvitationToken.encrypt(match { it.locale.toString() == setLocale }, any()) }
    }

    @Test
    fun `invitation token has correctly formatted mobile number`() {
        val internationalPhoneNumber = "i am super correct"
        every { phoneValidator.invoke(any(), any(), any()) } returns internationalPhoneNumber

        underTest(caseId, germanPhone, email, pubKey, null)

        verify { InvitationToken.encrypt(match { it.phone == internationalPhoneNumber }, any()) }
    }

    @Test
    fun `adding a case with an invalid phone number returns INVALID_PHONE`() {
        every { phoneValidator.invoke(any(), any(), any()) } returns null

        val res = underTest(caseId, germanPhone, email, pubKey, null)

        assertThat(res).isEqualTo(Result.INVALID_PHONE)
    }

    @Test
    fun `adding a case with an invalid phone number should not send an email`() {
        every { phoneValidator.invoke(any(), any(), any()) } returns null

        underTest(caseId, germanPhone, email, pubKey, null)

        verify(exactly = 0) { emailService.sendEmail(any()) }
    }

    @Test
    fun `adding a case with an invalid phone number should save case with status UNPAIRED`() {
        every { phoneValidator.invoke(any(), any(), any()) } returns null

        underTest(caseId, germanPhone, email, pubKey, null)

        verify(exactly = 1) {
            caseRepository.save(
                match {
                    it.internalCaseId == caseId && it.status == CaseEntity.Status.UNPAIRED
                },
            )
        }
    }

    @Test
    fun `adding a case and failing to send an email returns INVALID_EMAIL`() {
        every { emailService.sendEmail(any()) } returns false

        val result = underTest(caseId, germanPhone, email, pubKey, null)

        assertThat(result).isEqualTo(Result.INVALID_EMAIL)
    }

    @Test
    fun `adding a case and failing to send an email should save case with status UNPAIRED`() {
        every { emailService.sendEmail(any()) } returns false

        underTest(caseId, germanPhone, email, pubKey, null)

        verify(exactly = 1) {
            caseRepository.save(
                match {
                    it.internalCaseId == caseId && it.status == CaseEntity.Status.UNPAIRED
                },
            )
        }
    }
}
