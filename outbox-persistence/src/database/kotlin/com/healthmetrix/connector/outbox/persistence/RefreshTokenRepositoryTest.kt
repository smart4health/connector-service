package com.healthmetrix.connector.outbox.persistence

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource
import java.util.Locale

@DataJpaTest
@TestPropertySource(properties = ["spring.liquibase.change-log=classpath:db/changelog.yaml"])
class RefreshTokenRepositoryTest {

    @Autowired
    private lateinit var repo: RefreshTokenRepository

    @Autowired
    private lateinit var caseRepo: CaseRepository

    @Test
    fun `context loads`() {
        assertThat(repo).isNotNull
    }

    @Test
    fun `saving a refresh token works`() {
        val case = caseRepo.save(
            CaseEntity(
                internalCaseId = InternalCaseId.randomUUID(),
                publicKey = B64String("i am a public key"),
                lang = Bcp47LanguageTag(Locale.GERMAN),
            ),
        )
        val refreshTokenEntity = RefreshToken(case.internalCaseId, "refresh token")

        repo.save(refreshTokenEntity)

        val saved = repo.findById(refreshTokenEntity.internalCaseId)
        assertThat(saved).isEqualTo(refreshTokenEntity)
    }

    @Test
    fun `findAll returns all saved refresh tokens`() {
        val case1 = caseRepo.save(
            CaseEntity(
                internalCaseId = InternalCaseId.randomUUID(),
                publicKey = B64String("i am a public key"),
                lang = Bcp47LanguageTag(Locale.GERMAN),
            ),
        )

        val case2 = caseRepo.save(
            CaseEntity(
                internalCaseId = InternalCaseId.randomUUID(),
                publicKey = B64String("i am a public key"),
                lang = Bcp47LanguageTag(Locale.GERMAN),
            ),
        )

        val case3 = caseRepo.save(
            CaseEntity(
                internalCaseId = InternalCaseId.randomUUID(),
                publicKey = B64String("i am a public key"),
                lang = Bcp47LanguageTag(Locale.GERMAN),
            ),
        )

        val tokens = listOf(
            RefreshToken(case1.internalCaseId, "refresh token 1"),
            RefreshToken(case2.internalCaseId, "refresh token 2"),
            RefreshToken(case3.internalCaseId, "refresh token 3"),
        )

        tokens.forEach { t -> repo.save(t) }

        val result = repo.findAll()

        assertThat(result).isEqualTo(tokens)
    }

    @Test
    fun `deleting refresh tokens works`() {
        val case = caseRepo.save(
            CaseEntity(
                internalCaseId = InternalCaseId.randomUUID(),
                publicKey = B64String("i am a public key"),
                lang = Bcp47LanguageTag(Locale.GERMAN),
            ),
        )
        val refreshTokenEntity = RefreshToken(case.internalCaseId, "refresh token")
        repo.save(refreshTokenEntity)

        repo.delete(refreshTokenEntity)

        val deleted = repo.findById(refreshTokenEntity.internalCaseId)
        assertThat(deleted).isNull()
    }
}
