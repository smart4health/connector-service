package com.healthmetrix.connector.outbox.usecases

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import com.healthmetrix.connector.outbox.invitationtoken.TimeFrame
import com.healthmetrix.connector.outbox.oauth.OauthClient
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseNonceRepository
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.persistence.OauthStateEntity
import com.healthmetrix.connector.outbox.persistence.OauthStateRepository
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class CheckPinUseCase(
    @Qualifier("invitationTokenEncryptionKey") val invitationTokenEncryptionKey: LazySecret<AesKey>,
    val caseRepository: CaseRepository,
    val oauthStateRepository: OauthStateRepository,
    val timeFrame: TimeFrame,
    val caseNonceRepository: CaseNonceRepository,
    val oauthClient: OauthClient,
) {
    // note: not url encoded
    operator fun invoke(
        invitationToken: B64String,
        pin: String,
        now: Long = System.currentTimeMillis(),
    ): Result<String, CheckPinError> {
        val token = InvitationToken.decrypt(invitationToken, invitationTokenEncryptionKey.requiredValue)
            ?: return Err(CheckPinError.MALFORMED_INVITATION_TOKEN)

        logger.debug("checkPin for {}", kv("internalCaseId", token.caseId))

        val case = caseRepository.findById(token.caseId)
            ?: return Err(CheckPinError.INVALID_CASE_ID)

        // check for expiration i.e. another token was sent
        if (caseNonceRepository.findByIdAndNonce(case.internalCaseId, token.nonce) == null) {
            return Err(CheckPinError.EXPIRED_INVITATION_TOKEN)
        }

        if (case.status != CaseEntity.Status.PIN_SENT) {
            return Err(CheckPinError.PIN_NOT_SENT)
        }

        if (token.pins(timeFrame, now = now).none { it == pin }) {
            return Err(CheckPinError.INVALID_PIN)
        }

        val state = OauthClient.createOauthState()

        oauthStateRepository.save(OauthStateEntity(token.caseId, state))

        val updatedCase = caseRepository.save(case.copy(status = CaseEntity.Status.PIN_SUCCEEDED))

        return Ok(oauthClient.buildAuthorizationUrl(state, updatedCase.publicKey.string))
    }

    enum class CheckPinError {
        MALFORMED_INVITATION_TOKEN,
        INVALID_CASE_ID,
        EXPIRED_INVITATION_TOKEN,
        PIN_NOT_SENT,
        INVALID_PIN,
    }
}
