package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.PhoneValidator
import com.healthmetrix.connector.outbox.config.InvitationEmailFactory
import com.healthmetrix.connector.outbox.email.EmailService
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceRepository
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class AddCaseUseCase(
    private val emailService: EmailService,
    private val emailFactory: InvitationEmailFactory,
    private val caseRepository: CaseRepository,
    @Qualifier("invitationTokenEncryptionKey") private val invitationTokenEncryptionKey: LazySecret<AesKey>,
    private val phoneValidator: PhoneValidator,
    private val caseNonceRepository: CaseNonceRepository,
    @Value("\${default-locale}") private val defaultLanguageTag: String,
) {
    operator fun invoke(
        internalCaseId: InternalCaseId,
        mobileNumber: String,
        emailAddress: String,
        publicKey: B64String,
        lang: String?,
    ): Result {
        val locale = Bcp47LanguageTag(lang ?: defaultLanguageTag)

        val case = CaseEntity(internalCaseId, publicKey, locale)
        val exists = caseRepository.findById(case.internalCaseId) != null

        caseRepository.save(case)

        val internationalPhoneNumber = phoneValidator(internalCaseId, mobileNumber, locale)
        if (internationalPhoneNumber.isNullOrBlank()) {
            logger.info("addCase found invalid mobileNumber for {}", kv("internalCaseId", internalCaseId))
            return Result.INVALID_PHONE
        }

        val invitationToken = InvitationToken(internalCaseId, internationalPhoneNumber, locale)
        val encryptedInvitationToken =
            InvitationToken.encrypt(invitationToken, invitationTokenEncryptionKey.requiredValue)

        val email = emailFactory.make(emailAddress, encryptedInvitationToken, locale)
        if (!emailService.sendEmail(email)) {
            logger.info("addCase found invalid email for {}", kv("internalCaseId", internalCaseId))
            return Result.INVALID_EMAIL
        }

        caseNonceRepository.save(CaseNonceEntity(internalCaseId, invitationToken.nonce))
        caseRepository.save(case.copy(status = CaseEntity.Status.INVITATION_SENT))

        return if (exists) Result.SUCCESS_OVERRIDDEN else Result.SUCCESS_CREATED
    }

    enum class Result {
        SUCCESS_CREATED,
        SUCCESS_OVERRIDDEN,
        INVALID_EMAIL,
        INVALID_PHONE,
        INVALID_STATUS,
    }
}
