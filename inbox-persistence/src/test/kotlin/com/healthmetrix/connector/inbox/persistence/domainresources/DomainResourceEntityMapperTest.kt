package com.healthmetrix.connector.inbox.persistence.domainresources

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

internal class DomainResourceEntityMapperTest {

    private val externalCaseId = "le case numero uno"
    private val internalResourceId = UUID.randomUUID()
    private val json = "json"
    private val encryptedJson = "encrypted".toByteArray().encodeBase64()
    private val now = Timestamp(Instant.now().epochSecond)

    private val domainResource = DomainResource(
        internalResourceId = internalResourceId,
        externalCaseId = externalCaseId,
        insertedAt = now,
        json = json,
    )

    private val domainResourceEntity = DomainResourceEntity(
        internalResourceId = internalResourceId,
        externalCaseId = externalCaseId,
        insertedAt = now,
        encryptedJson = encryptedJson,
    )

    private val databaseEncryptionKey: LazySecret<AesKey> = mockk()

    private val underTest = DomainResourceEntityMapper(databaseEncryptionKey)

    @Test
    fun `toDomain should map correctly`() {
        every {
            databaseEncryptionKey.requiredValue.decrypt(any())
        } returns json.toByteArray(Charsets.UTF_8)

        val result = underTest.toDomain(domainResourceEntity)

        assertThat(result).isEqualTo(domainResource)
    }

    @Test
    fun `toDomain should return null when encrypted json is no valid base64`() {
        val result = underTest.toDomain(domainResourceEntity.copy(encryptedJson = B64String("i am not base64")))

        assertThat(result).isNull()
    }

    @Test
    fun `toDomain should return null when encrypted json can not be decrypted`() {
        every { databaseEncryptionKey.requiredValue.decrypt(any()) } returns null

        val result = underTest.toDomain(domainResourceEntity)

        assertThat(result).isNull()
    }

    @Test
    fun `toEntity should map correctly`() {
        every { databaseEncryptionKey.requiredValue.encrypt(any()) } returns "encrypted".toByteArray()

        val result = underTest.toEntity(domainResource)

        assertThat(result).isEqualTo(domainResourceEntity)
    }
}
