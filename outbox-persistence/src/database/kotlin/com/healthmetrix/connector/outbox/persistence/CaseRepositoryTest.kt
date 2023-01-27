package com.healthmetrix.connector.outbox.persistence

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@TestPropertySource(properties = ["spring.liquibase.change-log=classpath:db/changelog.yaml"])
class CaseRepositoryTest {

    @Autowired
    private lateinit var repo: CaseRepository

    @Test
    fun `context loads`() {
        assertThat(repo).isNotNull
    }

    @Test
    fun `create read update delete works`() {
        val case = CaseEntity(
            InternalCaseId.randomUUID(),
            CaseEntity.Status.UNPAIRED,
            B64String("hello"),
            Bcp47LanguageTag("en-US"),
        )
        repo.save(case)
        val saved = repo.findById(case.internalCaseId)
        assertThat(case).isEqualTo(saved)
        assertThat(case.lang).isEqualTo(Bcp47LanguageTag("en-US"))

        val updated = repo.save(saved!!.copy(status = CaseEntity.Status.PIN_SENT))
        assertThat(updated.internalCaseId).isEqualTo(case.internalCaseId)
        assertThat(updated.status).isNotEqualTo(case.status)
        assertThat(updated.status).isEqualTo(CaseEntity.Status.PIN_SENT)

        repo.delete(updated)
        val deleted = repo.findById(case.internalCaseId)
        assertThat(deleted).isNull()
    }
}
