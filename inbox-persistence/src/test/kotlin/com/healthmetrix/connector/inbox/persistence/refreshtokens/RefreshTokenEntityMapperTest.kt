package com.healthmetrix.connector.inbox.persistence.refreshtokens

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class RefreshTokenEntityMapperTest {

    private val internalCaseId = UUID.randomUUID()
    private val token = "test"
    private val encryptedToken = "encrypted".toByteArray(Charsets.UTF_8).encodeBase64()
    private val refreshToken =
        RefreshToken(
            internalCaseId,
            token,
            null,
        )
    private val entity =
        RefreshTokenEntity(
            internalCaseId,
            encryptedToken,
            null,
        )

    private val databaseEncryptionKey: LazySecret<AesKey> = mockk()

    private val underTest = RefreshTokenEntityMapper(databaseEncryptionKey)

    @Test
    fun `toEntity should map correctly`() {
        every { databaseEncryptionKey.requiredValue.encrypt("test".toByteArray(Charsets.UTF_8)) } returns "encrypted".toByteArray()

        val result = underTest.toEntity(refreshToken)

        assertThat(result).isEqualTo(entity)
    }

    @Test
    fun `toDomain should map correctly`() {
        every { databaseEncryptionKey.requiredValue.decrypt(encryptedToken.decode()!!) } returns token.toByteArray()

        val result = underTest.toDomain(entity)

        assertThat(result).isEqualTo(refreshToken)
    }

    @Test
    fun `toDomain should return null when encrypted token is not a correct base64 string`() {
        val result = underTest.toDomain(entity.copy(encryptedRefreshToken = B64String("no base64")))

        assertThat(result).isNull()
    }

    @Test
    fun `toDomain should return null when encrypted token can not be decrypted`() {
        every { databaseEncryptionKey.requiredValue.decrypt(encryptedToken.decode()!!) } returns null

        val result = underTest.toDomain(entity)

        assertThat(result).isNull()
    }
}
