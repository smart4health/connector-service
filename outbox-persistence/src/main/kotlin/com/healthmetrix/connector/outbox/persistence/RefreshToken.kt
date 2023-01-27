package com.healthmetrix.connector.outbox.persistence

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import org.springframework.stereotype.Service
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

data class RefreshToken(
    val internalCaseId: InternalCaseId,
    val value: String,
)

@Entity
@Table(name = "refresh_tokens")
data class RefreshTokenEntity(
    @Id
    val internalCaseId: InternalCaseId,
    @Convert(converter = B64StringConverter::class)
    val encryptedRefreshToken: B64String,
)

interface RefreshTokenCrudRepository : CrudRepository<RefreshTokenEntity, InternalCaseId>

@Service
class RefreshTokenEntityMapper(@Qualifier("databaseEncryptionKey") private val databaseEncryptionKey: LazySecret<AesKey>) {
    fun toEntity(refreshToken: RefreshToken): RefreshTokenEntity =
        refreshToken.value.toByteArray(Charsets.UTF_8)
            .let(databaseEncryptionKey.requiredValue::encrypt)
            .encodeBase64()
            .let { encryptedToken ->
                RefreshTokenEntity(
                    internalCaseId = refreshToken.internalCaseId,
                    encryptedRefreshToken = encryptedToken,
                )
            }

    fun toDomain(entity: RefreshTokenEntity): RefreshToken? {
        val token = entity.encryptedRefreshToken.decode()
            ?.let(databaseEncryptionKey.requiredValue::decrypt)
            ?.toString(Charsets.UTF_8)

        if (token == null) {
            logger.error(
                "RefreshToken for caseId {} could not be decrypted when reading from database",
                StructuredArguments.kv(
                    "internalCaseId",
                    entity.internalCaseId,
                ),
            )
            return null
        }

        return RefreshToken(
            internalCaseId = entity.internalCaseId,
            value = token,
        )
    }
}

@Repository
class RefreshTokenRepository(
    private val refreshTokenCrudRepository: RefreshTokenCrudRepository,
    private val mapper: RefreshTokenEntityMapper,
) {
    fun save(refreshToken: RefreshToken): RefreshToken = mapper.toEntity(refreshToken)
        .let(refreshTokenCrudRepository::save)
        .let(mapper::toDomain)!!

    fun findById(internalCaseId: InternalCaseId): RefreshToken? = refreshTokenCrudRepository
        .findByIdOrNull(internalCaseId)
        ?.let(mapper::toDomain)

    fun delete(refreshToken: RefreshToken) = mapper.toEntity(refreshToken)
        .let(refreshTokenCrudRepository::delete)

    fun deleteById(internalCaseId: InternalCaseId) =
        refreshTokenCrudRepository.deleteById(internalCaseId)

    fun findAll(): Iterable<RefreshToken> = refreshTokenCrudRepository.findAll()
        .mapNotNull(mapper::toDomain)
}
