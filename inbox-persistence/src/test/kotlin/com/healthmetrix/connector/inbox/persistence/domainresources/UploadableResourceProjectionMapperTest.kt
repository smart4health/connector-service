package com.healthmetrix.connector.inbox.persistence.domainresources

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class UploadableResourceProjectionMapperTest {

    private val privateKey = "private"
    private val privateKeyPemString = "-----BEGIN RSA PRIVATE KEY-----\n${privateKey.toByteArray(Charsets.UTF_8)
        .encodeBase64().string}\n-----END RSA PRIVATE KEY-----\n"

    private val token = "test"

    private val projection = object : UploadableResourceProjection {
        override val internalCaseId = UUID.randomUUID()

        override val internalResourceId = UUID.randomUUID()

        override val encryptedRefreshToken = "encrypted".toByteArray(Charsets.UTF_8).encodeBase64()

        override val encryptedPrivateKey = B64String("encryptedKey")
    }

    private val uploadableResource =
        UploadableResource(
            internalCaseId = projection.internalCaseId,
            internalResourceId = projection.internalResourceId,
            refreshToken = token,
            privateKeyPemString = privateKeyPemString,
        )

    private val databaseEncryptionKey: LazySecret<AesKey> = mockk()
    private val underTest = UploadableResourceProjectionMapper(databaseEncryptionKey)

    @Test
    fun `toDomain should map correctly`() {
        every {
            databaseEncryptionKey.requiredValue.decrypt(projection.encryptedRefreshToken.decode()!!)
        } returns token.toByteArray(Charsets.UTF_8)

        every {
            databaseEncryptionKey.requiredValue.decrypt(projection.encryptedPrivateKey.decode()!!)
        } returns privateKey.toByteArray(Charsets.UTF_8)

        val result = underTest.toDomain(projection)

        assertThat(result).isEqualTo(uploadableResource)
    }

    @Test
    fun `toDomain returns null when encrypted refresh token is not correct base64`() {
        val result = underTest.toDomain(
            object : UploadableResourceProjection {
                override val internalCaseId = projection.internalCaseId

                override val internalResourceId = projection.internalResourceId

                override val encryptedRefreshToken = B64String("not base 64")

                override val encryptedPrivateKey = projection.encryptedPrivateKey
            },
        )

        assertThat(result).isNull()
    }

    @Test
    fun `toDomain returns null when encrypted private key is not correct base64`() {
        every {
            databaseEncryptionKey.requiredValue.decrypt(any())
        } returns token.toByteArray(Charsets.UTF_8)

        val result = underTest.toDomain(
            object : UploadableResourceProjection {
                override val internalCaseId = projection.internalCaseId

                override val internalResourceId = projection.internalResourceId

                override val encryptedRefreshToken = projection.encryptedRefreshToken

                override val encryptedPrivateKey = B64String("not base 64")
            },
        )

        assertThat(result).isNull()
    }

    @Test
    fun `toDomain return null when encrypted refresh token can not be decrypted`() {
        every { databaseEncryptionKey.requiredValue.decrypt(any()) } returns null

        val result = underTest.toDomain(projection)

        assertThat(result).isNull()
    }

    @Test
    fun `toDomain return null when encrypted private key can not be decrypted`() {
        every {
            databaseEncryptionKey.requiredValue.decrypt(any())
        } returns token.toByteArray(Charsets.UTF_8)

        every {
            databaseEncryptionKey.requiredValue.decrypt(any())
        } returns null

        val result = underTest.toDomain(projection)

        assertThat(result).isNull()
    }

    @Test
    fun `toDomain return null when encrypted json can not be decrypted`() {
        every {
            databaseEncryptionKey.requiredValue.decrypt(any())
        } returns token.toByteArray(Charsets.UTF_8)

        every {
            databaseEncryptionKey.requiredValue.decrypt(any())
        } returns privateKey.toByteArray(Charsets.UTF_8)

        every { databaseEncryptionKey.requiredValue.decrypt(any()) } returns null

        val result = underTest.toDomain(projection)

        assertThat(result).isNull()
    }
}
