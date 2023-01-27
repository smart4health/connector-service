package com.healthmetrix.connector.outbox.usecases

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.unwrapError
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import com.healthmetrix.connector.outbox.invitationtoken.TimeFrame
import com.healthmetrix.connector.outbox.oauth.OauthClient
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceRepository
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.persistence.OauthStateEntity
import com.healthmetrix.connector.outbox.persistence.OauthStateRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.spyk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.UUID

class CheckPinUseCaseTest {

    private val caseId = UUID.randomUUID()
    private val invitationToken: InvitationToken =
        spyk(InvitationToken(caseId, "phoneNumber", Bcp47LanguageTag(Locale.US)))
    private val timeFrame = TimeFrame(num = 5)

    private val encryptionKey: LazySecret<AesKey> = mockk() {
        every { requiredValue } returns mockk()
    }

    private val pins = listOf("1", "2", "3", "4", "5")

    private val case =
        CaseEntity(invitationToken.caseId, CaseEntity.Status.PIN_SENT, B64String(""), Bcp47LanguageTag(Locale.GERMAN))
    private val caseRepository: CaseRepository = mockk() {
        every { save(any()) } answers { firstArg() }
        every { findById(any()) } returns case
    }

    private val caseNonce = CaseNonceEntity(invitationToken.caseId, invitationToken.nonce)
    private val caseNonceRepository: CaseNonceRepository = mockk() {
        every { findByIdAndNonce(any(), any()) } returns caseNonce
    }

    private val oauthStateRepository: OauthStateRepository = mockk() {
        every { save(any()) } answers { firstArg() }
    }
    private val encryptedToken = "I'm a token. Use me.".toByteArray().encodeBase64()

    private val oauthState = "super secret"
    private val oauthRedirectLocation = "go here. it's save. trust me."
    private val oauthClient: OauthClient = mockk {
        every { buildAuthorizationUrl(any(), any()) } returns oauthRedirectLocation
    }

    private val underTest = CheckPinUseCase(
        encryptionKey,
        caseRepository,
        oauthStateRepository,
        timeFrame,
        caseNonceRepository,
        oauthClient,
    )

    @BeforeEach
    internal fun setUp() {
        mockkObject(InvitationToken)
        every { InvitationToken.decrypt(any(), any()) } returns invitationToken
        every { invitationToken.pins(any(), any()) } returns pins

        mockkObject(OauthClient)
        every { OauthClient.createOauthState() } returns oauthState
    }

    @Test
    fun `happy path results in Success for all pins`() {
        pins.forEach { pin ->
            val result = underTest.invoke(encryptedToken, pin)
            assertThat(result).isInstanceOf(Ok::class.java)
        }
    }

    @Test
    fun `happy path should update case in database`() {
        underTest.invoke(encryptedToken, pins.first())

        verify(exactly = 1) { caseRepository.save(case.copy(status = CaseEntity.Status.PIN_SUCCEEDED)) }
    }

    @Test
    fun `happy path should save oauth state in database`() {
        underTest.invoke(encryptedToken, pins.first())

        verify(exactly = 1) { oauthStateRepository.save(OauthStateEntity(caseId, oauthState)) }
    }

    @Test
    fun `happy path should return oauth redirect location`() {
        val result = underTest.invoke(encryptedToken, pins.first())

        assertThat((result as Ok).value).isEqualTo(oauthRedirectLocation)
    }

    @Test
    fun `invalid invitation token results in InvalidInvitationToken`() {
        every { InvitationToken.decrypt(any(), any()) } returns null

        val result = underTest.invoke(encryptedToken, pins.first()).unwrapError()

        assertThat(result).isEqualTo(CheckPinUseCase.CheckPinError.MALFORMED_INVITATION_TOKEN)
    }

    @Test
    fun `invalid pin results in InvalidPin`() {
        val result = underTest.invoke(encryptedToken, "not a valid pin").unwrapError()

        assertThat(result).isEqualTo(CheckPinUseCase.CheckPinError.INVALID_PIN)
    }

    @Test
    fun `no stored case results in InvalidCaseId`() {
        every { caseRepository.findById(any()) } returns null

        val result = underTest.invoke(encryptedToken, pins.first()).unwrapError()

        assertThat(result).isEqualTo(CheckPinUseCase.CheckPinError.INVALID_CASE_ID)
    }

    @Test
    fun `checking the pin for a case with an incorrect status returns PinNotSent`() {
        every { caseRepository.findById(any()) } returns case.copy(status = CaseEntity.Status.UNPAIRED)

        val result = underTest.invoke(encryptedToken, pins.first()).unwrapError()

        assertThat(result).isEqualTo(CheckPinUseCase.CheckPinError.PIN_NOT_SENT)
    }

    @Test
    fun `non-matching case nonce results in InvalidToken`() {
        every { caseNonceRepository.findByIdAndNonce(any(), any()) } returns null

        val result = underTest.invoke(encryptedToken, pins.first()).unwrapError()

        assertThat(result).isEqualTo(CheckPinUseCase.CheckPinError.EXPIRED_INVITATION_TOKEN)
    }
}
