package com.healthmetrix.connector.inbox.sync

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.oauth.OauthClient
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime

/**
 * After 28 days or so of inactivity, refresh tokens are deactivated
 *
 * To prevent this, do a fake refresh -> access x refresh token exchange
 * after a configurable amount of time
 */
@Component
class RenewRefreshTokensUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val oauthClient: OauthClient,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
    @Value("\${sync.refresh-after}")
    private val refreshTokenRefreshAfter: Duration,
    private val inMemoryBackoffFilter: InMemoryBackoffFilter,
) {

    // similar exchange logic to AccessTokenMapper
    operator fun invoke() {
        var invalidGrantCount = 0

        refreshTokenRepository
            .findFetchedAtBefore(clock().minus(refreshTokenRefreshAfter))
            .filter { oldRefreshToken ->
                inMemoryBackoffFilter.shouldAttempt(oldRefreshToken.internalCaseId)
            }
            .also { if (it.isNotEmpty()) logger.info("Refreshing ${it.size} tokens") }
            .forEach { oldRefreshToken ->
                oauthClient.exchangeRefreshTokenForAccessToken(oldRefreshToken.value)
                    .onSuccess { tokenPair ->
                        inMemoryBackoffFilter.success(oldRefreshToken.internalCaseId)
                        refreshTokenRepository.save(
                            RefreshToken(
                                internalCaseId = oldRefreshToken.internalCaseId,
                                value = tokenPair.refresh,
                                fetchedAt = clock(),
                            ),
                        )
                    }
                    .onFailure { oauthClientError ->
                        when (oauthClientError) {
                            is OauthClient.Error.InvalidGrant -> invalidGrantCount += 1
                            else -> logger.info(
                                "Failed to renew {} {}",
                                StructuredArguments.kv("internalCaseId", oldRefreshToken.internalCaseId),
                                StructuredArguments.kv("kind", oauthClientError.shortDescription()),
                            )
                        }

                        inMemoryBackoffFilter.failed(oldRefreshToken.internalCaseId)
                    }
            }

        if (invalidGrantCount > 0) {
            logger.info("Found {} invalid grants", StructuredArguments.kv("invalidGrantCount", invalidGrantCount))
        }
    }
}
