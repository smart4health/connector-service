package com.healthmetrix.connector.outbox.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.config.SmsFactory
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import com.healthmetrix.connector.outbox.invitationtoken.TimeFrame
import com.healthmetrix.connector.outbox.persistence.CaseEntity.Status.INVITATION_SENT
import com.healthmetrix.connector.outbox.persistence.CaseEntity.Status.OAUTH_SUCCEEDED
import com.healthmetrix.connector.outbox.persistence.CaseEntity.Status.PIN_SENT
import com.healthmetrix.connector.outbox.persistence.CaseEntity.Status.PIN_SUCCEEDED
import com.healthmetrix.connector.outbox.persistence.CaseNonceRepository
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.sms.SmsService
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class SendPinUseCase(
    @Qualifier("invitationTokenEncryptionKey")
    private val invitationTokenEncryptionKey: LazySecret<AesKey>,
    private val smsService: SmsService,
    private val caseRepository: CaseRepository,
    private val timeFrame: TimeFrame,
    private val smsFactory: SmsFactory,
    private val caseNonceRepository: CaseNonceRepository,
) {
    operator fun invoke(invitationToken: B64String, now: Long = System.currentTimeMillis()): Result<Unit, SendPinError> {
        val token = InvitationToken.decrypt(invitationToken, invitationTokenEncryptionKey.requiredValue)
            ?: return Err(SendPinError.MALFORMED_INVITATION_TOKEN)

        logger.debug("sendingPin to {}", kv("internalCaseId", token.caseId))

        val case = caseRepository.findById(token.caseId)
            ?: return Err(SendPinError.INVALID_CASE_ID)

        if (caseNonceRepository.findByIdAndNonce(case.internalCaseId, token.nonce) == null) {
            return Err(SendPinError.EXPIRED_INVITATION_TOKEN)
        }

        if (case.status !in listOf(INVITATION_SENT, PIN_SENT, PIN_SUCCEEDED)) {
            logger.info(
                "Case in invalid status tried to send pin: {} {}",
                kv("status", case.status),
                kv("internalCaseId", token.caseId),
            )
            if (case.status == OAUTH_SUCCEEDED) {
                return Err(SendPinError.ALREADY_PAIRED)
            }
            return Err(SendPinError.INVALID_STATUS)
        }

        val pin = token.pins(timeFrame, now = now).first()

        val smsSuccess = smsService.sendSms(smsFactory.make(token.locale, token.phone, pin))

        if (smsSuccess is Err) {
            return Err(SendPinError.SMS_ERROR)
        }

        caseRepository.save(case.copy(status = PIN_SENT))

        return Ok(Unit)
    }

    enum class SendPinError {
        MALFORMED_INVITATION_TOKEN,
        EXPIRED_INVITATION_TOKEN,
        INVALID_STATUS,
        ALREADY_PAIRED,
        INVALID_CASE_ID,
        SMS_ERROR,
    }
}
