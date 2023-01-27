package com.healthmetrix.connector.inbox.sync

import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.outbox.Outbox
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class SyncRefreshTokensUseCase(
    private val outbox: Outbox,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) {
    operator fun invoke() {
        val refreshTokenEntities = outbox.getRefreshTokens()
            .also { refreshTokens -> logger.debug("syncRefreshTokens {}", kv("found", refreshTokens.size)) }
            .map {
                RefreshToken(
                    it.internalCaseId,
                    it.refreshToken,
                    clock(),
                )
            }
            .let(refreshTokenRepository::saveAll)
            .filterNotNull()

        with(refreshTokenEntities.map(RefreshToken::internalCaseId)) {
            val notDeleted = this - outbox.deleteRefreshTokens(this)
            if (notDeleted.isNotEmpty()) {
                logger.warn("syncRefreshTokens {}", kv("notDeleted", notDeleted))
            }
        }
    }
}
