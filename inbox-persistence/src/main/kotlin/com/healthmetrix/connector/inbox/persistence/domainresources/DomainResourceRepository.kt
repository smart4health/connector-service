package com.healthmetrix.connector.inbox.persistence.domainresources

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.util.UUID

interface DomainResourceCrudRepository : CrudRepository<DomainResourceEntity, UUID> {
    @Query(
        "SELECT c.internalCaseId as internalCaseId, d.internalResourceId as internalResourceId, r.encryptedRefreshToken as encryptedRefreshToken, c.privateKey as encryptedPrivateKey " +
            "FROM DomainResourceEntity d, CaseEntity c, RefreshTokenEntity r " +
            "WHERE d.externalCaseId = c.externalCaseId AND c.internalCaseId = r.internalCaseId",
    )
    fun getResourcesWithRefreshTokens(pageable: Pageable): List<UploadableResourceProjection>

    @Query("SELECT d.internalResourceId FROM DomainResourceEntity d WHERE d.insertedAt < :timestamp")
    fun getInternalResourceIdsInsertedBeforeTimestamp(timestamp: Timestamp): List<UUID>

    @Modifying
    @Query("DELETE FROM DomainResourceEntity d WHERE d.internalResourceId in :ids")
    fun deleteByInternalResourceIds(ids: List<UUID>)
}

@Repository
class DomainResourceRepository(
    private val domainResourceCrudRepository: DomainResourceCrudRepository,
    private val entityMapper: DomainResourceEntityMapper,
    private val projectionMapper: UploadableResourceProjectionMapper,
) {
    fun save(domainResource: DomainResource): DomainResource =
        entityMapper.toEntity(domainResource)
            .let(domainResourceCrudRepository::save)
            .let(entityMapper::toDomain)!!

    fun getResourcesWithRefreshTokens(limit: Int = 50): List<UploadableResource> {
        val pageable = Pageable.ofSize(limit)
        return domainResourceCrudRepository.getResourcesWithRefreshTokens(pageable)
            .mapNotNull(projectionMapper::toDomain)
    }

    fun deleteById(internalResourceId: UUID) =
        domainResourceCrudRepository.deleteById(internalResourceId)

    fun getInternalResourceIdsInsertedBeforeTimestamp(timestamp: Timestamp): List<UUID> =
        domainResourceCrudRepository.getInternalResourceIdsInsertedBeforeTimestamp(timestamp)

    fun deleteByInternalResourceIds(ids: List<UUID>) = domainResourceCrudRepository.deleteByInternalResourceIds(ids)

    @Transactional
    fun deleteResourcesBeforeTimestamp(timestamp: Timestamp, block: (List<UUID>) -> Unit) =
        getInternalResourceIdsInsertedBeforeTimestamp(timestamp)
            .also(block)
            .let(this::deleteByInternalResourceIds)

    // yes the entity "leaks through" but the layers were never super clear before
    fun findById(internalResourceId: UUID): DomainResourceEntity? =
        domainResourceCrudRepository.findById(internalResourceId)
            .orElse(null)
}
