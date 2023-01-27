package com.healthmetrix.connector.inbox.persistence.refreshtokens

import com.healthmetrix.connector.inbox.persistence.DatabaseTestConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.util.UUID

@DataJpaTest(
    properties = ["spring.liquibase.change-log=classpath:db/changelog.yaml"],
)
@ImportAutoConfiguration(classes = [DatabaseTestConfiguration::class])
class RefreshTokenRepositoryTest {

    private val internalCaseId1 = UUID.randomUUID()
    private val refreshToken1 =
        RefreshToken(
            internalCaseId1,
            "test_1",
            null,
        )

    private val internalCaseId2 = UUID.randomUUID()
    private val refreshToken2 =
        RefreshToken(
            internalCaseId2,
            "test_2",
            null,
        )

    @Autowired
    private lateinit var repo: RefreshTokenRepository

    @Test
    fun `context loads`() {
        assertThat(repo).isNotNull
    }

    @Test
    fun `a single refresh token can be saved`() {
        val persisted = repo.save(refreshToken1)

        assertThat(persisted).isEqualTo(refreshToken1)
    }

    @Test
    fun `a single refresh token can be retrieved after saving`() {
        repo.save(refreshToken1)

        val res = repo.findByIdOrNull(internalCaseId1)

        assertThat(res).isEqualTo(refreshToken1)
    }

    @Test
    fun `multiple refresh tokens can be saved`() {
        val persisted = repo.saveAll(listOf(refreshToken1, refreshToken2))

        assertThat(persisted).isEqualTo(listOf(refreshToken1, refreshToken2))
    }

    @Test
    fun `multiple refresh tokens can be retrieved after saving`() {
        repo.saveAll(listOf(refreshToken1, refreshToken2))

        assertThat(repo.findByIdOrNull(internalCaseId1)).isEqualTo(refreshToken1)
        assertThat(repo.findByIdOrNull(internalCaseId2)).isEqualTo(refreshToken2)
    }

    @Test
    fun `findByIdOrNull should return null when refresh token does not exist`() =
        assertThat(repo.findByIdOrNull(UUID.randomUUID())).isNull()

    @Test
    fun `a refresh token can be deleted`() {
        repo.save(refreshToken1)

        repo.deleteById(internalCaseId1)

        assertThat(repo.findByIdOrNull(internalCaseId1)).isNull()
    }
}
