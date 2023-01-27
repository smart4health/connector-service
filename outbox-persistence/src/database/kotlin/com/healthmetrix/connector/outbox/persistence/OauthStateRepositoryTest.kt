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
class OauthStateRepositoryTest {

    @Autowired
    private lateinit var repo: OauthStateRepository

    @Autowired
    private lateinit var caseRepo: CaseRepository

    @Test
    fun `context loads`() {
        assertThat(repo).isNotNull
    }

    @Test
    fun `create read delete works`() {
        val case = caseRepo.save(CaseEntity(InternalCaseId.randomUUID(), B64String("External Case Id"), Bcp47LanguageTag(Locale.GERMAN)))
        val state = OauthStateEntity(case.internalCaseId, "wow much random")
        repo.save(state)

        val saved = repo.findById(state.internalCaseId)
        assertThat(state).isEqualTo(saved)

        repo.delete(state)

        val deleted = repo.findById(state.internalCaseId)
        assertThat(deleted).isNull()
    }
}
