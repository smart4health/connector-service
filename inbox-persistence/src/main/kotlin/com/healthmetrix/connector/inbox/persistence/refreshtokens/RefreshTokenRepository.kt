package com.healthmetrix.connector.inbox.persistence.refreshtokens

import com.healthmetrix.connector.commons.InternalCaseId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

interface RefreshTokenCrudRepository : CrudRepository<RefreshTokenEntity, InternalCaseId> {

    @Query("SELECT r FROM RefreshTokenEntity r WHERE r.fetchedAt < :fetchDate OR r.fetchedAt IS NULL")
    fun findFetchedAtBefore(fetchDate: Timestamp): List<RefreshTokenEntity>
}

@Repository
class RefreshTokenRepository(
    private val refreshTokenCrudRepository: RefreshTokenCrudRepository,
    private val mapper: RefreshTokenEntityMapper,
) {
    fun save(refreshToken: RefreshToken) = refreshToken
        .let(mapper::toEntity)
        .let(refreshTokenCrudRepository::save)
        .let(mapper::toDomain)!!

    fun saveAll(refreshTokens: List<RefreshToken>): Iterable<RefreshToken> =
        refreshTokens
            .map(mapper::toEntity)
            .let { encryptedRefreshTokens ->
                // Unfortunately type inference fails here when trying to use method references instead
                refreshTokenCrudRepository.saveAll(encryptedRefreshTokens)
            }
            .map { entity -> mapper.toDomain(entity)!! }

    fun findByIdOrNull(internalCaseId: InternalCaseId) =
        refreshTokenCrudRepository.findByIdOrNull(internalCaseId)?.let(mapper::toDomain)

    fun findByIds(internalCaseIds: List<InternalCaseId>): List<RefreshToken> {
        return refreshTokenCrudRepository.findAllById(internalCaseIds).mapNotNull(mapper::toDomain)
    }

    fun deleteById(internalCaseId: InternalCaseId) {
        if (refreshTokenCrudRepository.findByIdOrNull(internalCaseId) != null) {
            refreshTokenCrudRepository.deleteById(internalCaseId)
        }
    }

    fun findFetchedAtBefore(fetchDate: LocalDateTime): List<RefreshToken> = refreshTokenCrudRepository
        .findFetchedAtBefore(Timestamp.valueOf(fetchDate))
        .mapNotNull(mapper::toDomain)
}
