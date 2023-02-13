package com.healthmetrix.connector.inbox.persistence.domainresources

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.crypto.RsaPrivateKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

@DataJpaTest(
    properties = ["spring.liquibase.change-log=classpath:db/changelog.yaml"],
)
class DomainResourceRepositoryTest {

    @Autowired
    private lateinit var domainResourceRepository: DomainResourceRepository

    @Autowired
    private lateinit var caseRepository: CaseRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @Autowired
    @Qualifier("databaseEncryptionKey")
    private lateinit var databaseEncryption: LazySecret<AesKey>

    private lateinit var privateKey: PrivateKey
    private lateinit var encryptedPrivateKey: B64String

    @BeforeEach
    internal fun setUp() {
        privateKey = with(KeyPairGenerator.getInstance("RSA")) {
            initialize(2048)
            generateKeyPair().private
        }

        // remove this as soon as the private key will be encrypted / decrypted inside the repository
        encryptedPrivateKey = databaseEncryption.requiredValue.encrypt(privateKey.encoded).encodeBase64()
    }

    @Test
    fun `finding domain resources with refresh tokens excludes those without`() {
        val internalCaseId = UUID.randomUUID()

        caseRepository.save(
            CaseEntity(
                internalCaseId,
                "externalCaseId1",
                encryptedPrivateKey,
            ),
        )
        caseRepository.save(
            CaseEntity(
                UUID.randomUUID(),
                "externalCaseId2",
                encryptedPrivateKey,
            ),
        )

        refreshTokenRepository.saveAll(
            listOf(
                RefreshToken(
                    internalCaseId,
                    "refreshToken",
                    null,
                ),
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                UUID.randomUUID(),
                "externalCaseId1",
                Timestamp.from(Instant.now()),
                "document",
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                UUID.randomUUID(),
                "externalCaseId2",
                Timestamp.from(Instant.now()),
                "document",
            ),
        )

        val toUpload = domainResourceRepository.getResourcesWithRefreshTokens()

        assertThat(toUpload.size).isEqualTo(1)
    }

    @Test
    fun `finding domain resources with refresh tokens returns the correct projection`() {
        val internalCaseId = UUID.randomUUID()
        val firstResourceId = UUID.randomUUID()

        caseRepository.save(
            CaseEntity(
                internalCaseId,
                "externalCaseId1",
                encryptedPrivateKey,
            ),
        )
        caseRepository.save(
            CaseEntity(
                UUID.randomUUID(),
                "externalCaseId2",
                encryptedPrivateKey,
            ),
        )

        refreshTokenRepository.saveAll(
            listOf(
                RefreshToken(
                    internalCaseId,
                    "refreshToken",
                    null,
                ),
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                firstResourceId,
                "externalCaseId1",
                Timestamp.from(Instant.now()),
                "document",
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                UUID.randomUUID(),
                "externalCaseId2",
                Timestamp.from(Instant.now()),
                "document",
            ),
        )

        val toUpload = domainResourceRepository.getResourcesWithRefreshTokens().first()

        assertThat(toUpload.internalCaseId).isEqualTo(internalCaseId)
        assertThat(toUpload.internalResourceId).isEqualTo(firstResourceId)
        assertThat(toUpload.refreshToken).isEqualTo("refreshToken")
        assertThat(toUpload.privateKeyPemString).isEqualTo(RsaPrivateKey.toPemKeyString(privateKey.encoded))
    }

    @Test
    fun `resources older than the given timestamp are deleted`() {
        val now = Instant.now()

        (1L until 5L).map { n ->
            val externalCaseId = "externalCaseId$n"
            caseRepository.save(
                CaseEntity(
                    UUID.randomUUID(),
                    externalCaseId,
                    encryptedPrivateKey,
                ),
            )
            val t = Timestamp.from(now - (Duration.ofMinutes(15 * n)))
            DomainResource(
                UUID.randomUUID(),
                externalCaseId,
                t,
                "json $n",
            )
        }.forEach { entity ->
            domainResourceRepository.save(entity)
        }

        val deleteBefore = Timestamp.from(now - Duration.ofMinutes((15 * 2) + 5))

        domainResourceRepository.deleteResourcesBeforeTimestamp(deleteBefore) {
            assertThat(it.size).isEqualTo(2)
        }
    }

    @Test
    fun `query limits are obeyed`() {
        val internalCaseId = UUID.randomUUID()
        val internalCaseId2 = UUID.randomUUID()

        caseRepository.save(
            CaseEntity(
                internalCaseId,
                "externalCaseId1",
                encryptedPrivateKey,
            ),
        )
        caseRepository.save(
            CaseEntity(
                internalCaseId2,
                "externalCaseId2",
                encryptedPrivateKey,
            ),
        )

        refreshTokenRepository.saveAll(
            listOf(
                RefreshToken(
                    internalCaseId,
                    "refreshToken",
                    null,
                ),
                RefreshToken(
                    internalCaseId2,
                    "refreshToken2",
                    null,
                ),
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                UUID.randomUUID(),
                "externalCaseId1",
                Timestamp.from(Instant.now()),
                "document",
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                UUID.randomUUID(),
                "externalCaseId2",
                Timestamp.from(Instant.now()),
                "document",
            ),
        )

        val toUpload = domainResourceRepository.getResourcesWithRefreshTokens(1)

        assertThat(toUpload.size).isEqualTo(1)
    }
}
