package com.healthmetrix.connector.inbox.outbox

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.encodeHex
import com.healthmetrix.connector.commons.logger
import net.logstash.logback.argument.StructuredArguments.kv
import java.security.PublicKey

class MockOutbox : Outbox {

    private val refreshTokens: MutableMap<InternalCaseId, String> = mutableMapOf()

    override fun addCase(
        internalCaseId: InternalCaseId,
        content: ByteArray,
        contentType: String,
        publicKey: PublicKey,
    ) = refreshTokens[internalCaseId]
        .let { if (it == null) AddCaseResult.SuccessCreated else AddCaseResult.SuccessOverridden }
        .also { res ->
            logger.info(
                "MockOutbox: Adding case {} {}",
                kv("internalCaseId", internalCaseId),
                kv("result", res.toString()),
            )
            if (res == AddCaseResult.SuccessCreated) {
                refreshTokens[internalCaseId] = internalCaseId.toString().toByteArray().encodeHex()
            }
        }

    override fun getRefreshTokens(): List<RefreshToken> = refreshTokens.entries.map { (internalCaseId, refreshToken) ->
        RefreshToken(internalCaseId, refreshToken)
    }.also { tokens ->
        logger.info("MockOutbox ${tokens.size} tokens fetched")
    }

    override fun deleteRefreshTokens(ids: List<InternalCaseId>) = ids.mapNotNull { internalCaseId ->
        refreshTokens.remove(internalCaseId)?.let { internalCaseId }
    }.also { deleted ->
        logger.info("MockOutbox ${deleted.size} tokens deleted")
    }

    override fun simpleHealthCheck() = true
}
