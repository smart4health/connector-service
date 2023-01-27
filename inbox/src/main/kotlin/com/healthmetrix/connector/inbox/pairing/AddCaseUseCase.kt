package com.healthmetrix.connector.inbox.pairing

import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.inbox.outbox.AddCaseResult
import com.healthmetrix.connector.inbox.outbox.Outbox
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.security.KeyPairGenerator

@Service
class AddCaseUseCase(
    private val caseRepository: CaseRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val outbox: Outbox,
    @Qualifier("databaseEncryptionKey")
    private val encryptionKey: LazySecret<AesKey>,
) {
    operator fun invoke(
        externalCaseId: ExternalCaseId,
        content: ByteArray,
        contentType: String,
    ): AddCaseResult {
        val (priv, pub) = with(KeyPairGenerator.getInstance("RSA")) {
            initialize(2048)
            val pair = generateKeyPair()
            pair.private to pair.public
        }

        val internalCaseId = caseRepository.findByExternalCaseId(listOf(externalCaseId)).firstOrNull()?.internalCaseId
            ?: InternalCaseId.randomUUID()

        val response = outbox.addCase(
            internalCaseId = internalCaseId,
            content = content,
            contentType = contentType,
            publicKey = pub,
        )

        // TODO: move this to persistence layer
        val encryptedPrivateKey = encryptionKey
            .requiredValue
            .encrypt(priv.encoded)

        if (response !is AddCaseResult.Error) {
            caseRepository.save(
                CaseEntity(
                    internalCaseId,
                    externalCaseId,
                    encryptedPrivateKey.encodeBase64(),
                ),
            )
            refreshTokenRepository.deleteById(internalCaseId)
        }

        return response
    }
}
