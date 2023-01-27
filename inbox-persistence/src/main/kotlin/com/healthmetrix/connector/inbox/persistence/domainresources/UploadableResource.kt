package com.healthmetrix.connector.inbox.persistence.domainresources

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.crypto.RsaPrivateKey
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.util.UUID

data class UploadableResource(
    val internalCaseId: InternalCaseId,
    val internalResourceId: UUID,
    val refreshToken: String,
    val privateKeyPemString: String,
)

interface UploadableResourceProjection {
    val internalCaseId: InternalCaseId
    val internalResourceId: UUID
    val encryptedRefreshToken: B64String
    val encryptedPrivateKey: B64String
}

@Service
class UploadableResourceProjectionMapper(
    @Qualifier("databaseEncryptionKey")
    private val encryptionKey: LazySecret<AesKey>,
) {
    fun toDomain(projection: UploadableResourceProjection): UploadableResource? {
        val refreshToken = projection.encryptedRefreshToken.decode()
            ?.let(encryptionKey.requiredValue::decrypt)
            ?.toString(Charsets.UTF_8)

        if (refreshToken == null) {
            logger.error(
                "RefreshToken for resourceId {} and caseId {} could not be decrypted when reading from database",
                StructuredArguments.kv(
                    "internalResourceId",
                    projection.internalResourceId,
                ),
                StructuredArguments.kv(
                    "internalCaseId",
                    projection.internalCaseId,
                ),
            )
            return null
        }

        val privateKeyPemString = projection.encryptedPrivateKey.decode()
            ?.let(encryptionKey.requiredValue::decrypt)
            ?.let(RsaPrivateKey.Companion::toPemKeyString)

        if (privateKeyPemString == null) {
            logger.warn(
                "Unable to read private key for resource {} and case {}",
                StructuredArguments.kv(
                    "internalResourceId",
                    projection.internalResourceId,
                ),
                StructuredArguments.kv(
                    "internalCaseId",
                    projection.internalCaseId,
                ),
            )

            return null
        }

        return UploadableResource(
            internalCaseId = projection.internalCaseId,
            internalResourceId = projection.internalResourceId,
            refreshToken = refreshToken,
            privateKeyPemString = privateKeyPemString,
        )
    }
}
