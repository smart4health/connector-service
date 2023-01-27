package com.healthmetrix.connector.inbox.persistence.cases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.persistence.DatabaseTestConfiguration
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest(
    properties = ["spring.liquibase.change-log=classpath:db/changelog.yaml"],
)
@ImportAutoConfiguration(classes = [DatabaseTestConfiguration::class])
class CaseRepositoryTest {

    @Autowired
    private lateinit var repo: CaseRepository

    @Test
    fun `context loads`() {
        assertThat(repo).isNotNull
    }

    @Test
    fun `create read update work`() {
        val case = CaseEntity(
            InternalCaseId.randomUUID(),
            "external case id",
            B64String("hello world"),
        )

        repo.save(case)
        val saved = repo.findByExternalCaseId(listOf("external case id")).first()
        assertThat(case).isEqualTo(saved)

        val updated = repo.save(case.copy(privateKey = B64String("new key")))
        assertThat(updated.internalCaseId).isEqualTo(case.internalCaseId)
        assertThat(updated.externalCaseId).isEqualTo(case.externalCaseId)
        assertThat(updated.privateKey).isNotEqualTo(case.privateKey)
    }
}
