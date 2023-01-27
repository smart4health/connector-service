package com.healthmetrix.connector.inbox.persistence.refreshtokens

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.inbox.persistence.B64StringConverter
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

data class RefreshToken(
    val internalCaseId: InternalCaseId,
    val value: String,
    val fetchedAt: LocalDateTime?,
)

@Entity
@Table(name = "refresh_tokens")
data class RefreshTokenEntity(
    @Id
    val internalCaseId: InternalCaseId,
    @Convert(converter = B64StringConverter::class)
    val encryptedRefreshToken: B64String,
    val fetchedAt: Timestamp?,
)

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
                    fetchedAt = refreshToken.fetchedAt?.let(Timestamp::valueOf),
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
            fetchedAt = entity.fetchedAt?.toLocalDateTime(),
        )
    }
}
