package com.healthmetrix.connector.inbox.upload

import com.github.michaelbull.result.onFailure
import com.github.michaelbull.result.onSuccess
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import com.healthmetrix.connector.inbox.persistence.domainresources.UploadableResource
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptEntity
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptRepository
import com.healthmetrix.connector.inbox.upload.AccessTokenMapper.ResourceWithAccessToken
import com.healthmetrix.connector.inbox.upload.UploadDocumentsUseCase.GroupedCaseInfo
import com.healthmetrix.connector.inbox.upload.UploadDocumentsUseCase.GroupedResource
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

interface AccessTokenFetcher : (Map.Entry<GroupedCaseInfo, List<GroupedResource>>) -> List<ResourceWithAccessToken>
interface BackoffPredicate : (UploadableResource) -> Boolean

@Component
class UploadDocumentsUseCase(
    private val domainResourceRepository: DomainResourceRepository,
    private val uploadAttemptRepository: UploadAttemptRepository,
    private val backoffFilter: BackoffPredicate,
    private val accessTokenMapper: AccessTokenFetcher,
    private val resourceUploader: ResourceUploader,
    private val decryptResourceUseCase: DecryptResourceUseCase,
) {

    /**
     * 1. Get resource ids with refresh tokens and private keys
     * 2. filter out if too many attempts in X amount of time
     * 3. group by case, in order to only exchange a refresh token once
     * 4. exchange the refresh token for an access token
     * 5. load and decrypt the resource one at a time, and upload
     */
    operator fun invoke() = domainResourceRepository
        .getResourcesWithRefreshTokens()
        .filter(backoffFilter)
        .also { if (it.isNotEmpty()) logger.info("uploadResources {}", kv("toUpload", it.size)) }
        .groupBy(::GroupedCaseInfo, ::GroupedResource)
        .flatMap(accessTokenMapper)
        .forEach { resourceWithAccessToken ->

            decryptResourceUseCase(resourceWithAccessToken.internalResourceId)
                .onFailure {
                    logger.warn(
                        "Failed to decrypt resource {}",
                        kv("internalResourceId", resourceWithAccessToken.internalResourceId),
                    )
                }
                // not the prettiest
                .onSuccess { json ->
                    resourceUploader(
                        json = json,
                        accessToken = resourceWithAccessToken.accessToken,
                        privateKeyPemString = resourceWithAccessToken.privateKeyPemString,
                        internalResourceId = resourceWithAccessToken.internalResourceId,
                    ).onSuccess { uuid ->
                        domainResourceRepository.deleteById(uuid)
                    }.onFailure { (uuid, _) ->
                        uploadAttemptRepository.save(uuid.toAttempt())
                    }
                }
        }

    private fun UUID.toAttempt() =
        UploadAttemptEntity(
            UUID.randomUUID(),
            this,
            Timestamp.from(Instant.now()),
        )

    // these have a 1 to 1 relationship, so safe to group by
    data class GroupedCaseInfo(
        val internalCaseId: InternalCaseId,
        val privateKeyPemString: String,
        val refreshToken: String,
    ) {
        constructor(resource: UploadableResource) : this(
            resource.internalCaseId,
            resource.privateKeyPemString,
            resource.refreshToken,
        )
    }

    data class GroupedResource(
        val internalResourceId: UUID,
    ) {
        constructor(resource: UploadableResource) : this(
            resource.internalResourceId,
        )
    }
}
