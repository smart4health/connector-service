package com.healthmetrix.connector.inbox.upload

import com.github.michaelbull.result.getOrElse
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.oauth.OauthClient
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptEntity
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptRepository
import com.healthmetrix.connector.inbox.upload.UploadDocumentsUseCase.GroupedCaseInfo
import com.healthmetrix.connector.inbox.upload.UploadDocumentsUseCase.GroupedResource
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

@Component
class AccessTokenMapper(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val uploadAttemptRepository: UploadAttemptRepository,
    private val oauthClient: OauthClient,
    private val clock: () -> LocalDateTime = LocalDateTime::now,
) : AccessTokenFetcher {

    override fun invoke(entry: Map.Entry<GroupedCaseInfo, List<GroupedResource>>): List<ResourceWithAccessToken> {
        val (caseInfo, docs) = entry

        logger.info(
            "accessTokenMapper grouped {} {}",
            kv("internalCaseId", caseInfo.internalCaseId),
            kv("n", docs.size),
        )

        // add a failed attempt for each resource if the exchange fails
        val (accessToken, newRefreshToken) = oauthClient
            .exchangeRefreshTokenForAccessToken(caseInfo.refreshToken)
            .getOrElse { oauthClientError ->

                logger.info(
                    "Failed to get access token {} {}",
                    kv("internalCaseId", caseInfo.internalCaseId),
                    kv("kind", oauthClientError.shortDescription()),
                )

                docs.forEach { groupedResource ->
                    uploadAttemptRepository.save(
                        UploadAttemptEntity(
                            UUID.randomUUID(),
                            groupedResource.internalResourceId,
                            Instant.now().let(Timestamp::from),
                        ),
                    )
                }

                return listOf()
            }

        refreshTokenRepository.save(
            RefreshToken(
                caseInfo.internalCaseId,
                newRefreshToken,
                clock(),
            ),
        )

        return docs.map { doc ->
            ResourceWithAccessToken(
                doc.internalResourceId,
                accessToken,
                caseInfo.privateKeyPemString,
            )
        }
    }

    data class ResourceWithAccessToken(
        val internalResourceId: UUID,
        val accessToken: String,
        val privateKeyPemString: String,
    )
}
