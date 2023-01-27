package com.healthmetrix.connector.inbox.persistence.domainresources

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.inbox.persistence.B64StringConverter
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

data class DomainResource(
    val internalResourceId: UUID,
    val externalCaseId: String,
    val insertedAt: Timestamp,
    val json: String,
)

@Entity
@Table(name = "domain_resources")
data class DomainResourceEntity(
    @Id
    val internalResourceId: UUID,
    val externalCaseId: String,
    val insertedAt: Timestamp,
    @Convert(converter = B64StringConverter::class)
    val encryptedJson: B64String,
)

@Service
class DomainResourceEntityMapper(
    @Qualifier("databaseEncryptionKey")
    private val encryptionKey: LazySecret<AesKey>,
) {
    fun toDomain(entity: DomainResourceEntity): DomainResource? {
        val json = entity.encryptedJson.decode()
            ?.let(encryptionKey.requiredValue::decrypt)
            ?.toString(Charsets.UTF_8)

        if (json == null) {
            logger.error(
                "DomainResource with id {} for externalCaseId {} could not be decrypted when reading from database",
                StructuredArguments.kv(
                    "internalResourceId",
                    entity.internalResourceId,
                ),
                StructuredArguments.kv(
                    "externalCaseId",
                    entity.externalCaseId,
                ),
            )
            return null
        }

        return DomainResource(
            internalResourceId = entity.internalResourceId,
            externalCaseId = entity.externalCaseId,
            json = json,
            insertedAt = entity.insertedAt,
        )
    }

    fun toEntity(domainResource: DomainResource): DomainResourceEntity {
        val encryptedJson = domainResource.json.toByteArray(Charsets.UTF_8)
            .let(encryptionKey.requiredValue::encrypt)
            .encodeBase64()

        return DomainResourceEntity(
            internalResourceId = domainResource.internalResourceId,
            externalCaseId = domainResource.externalCaseId,
            encryptedJson = encryptedJson,
            insertedAt = domainResource.insertedAt,
        )
    }
}
