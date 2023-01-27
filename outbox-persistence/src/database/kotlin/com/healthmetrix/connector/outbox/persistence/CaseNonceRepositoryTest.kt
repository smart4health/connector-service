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
class CaseNonceRepositoryTest {
    @Autowired
    private lateinit var repo: CaseNonceRepository

    @Autowired
    private lateinit var caseRepo: CaseRepository

    @Test
    fun `findByIdAndNonce finds the case nonce when both match`() {
        val case = caseRepo.save(CaseEntity(InternalCaseId.randomUUID(), B64String(""), Bcp47LanguageTag(Locale.GERMAN)))
        val caseNonce = repo.save(CaseNonceEntity(case.internalCaseId, 1))

        val saved = repo.findByIdAndNonce(internalCaseId = case.internalCaseId, nonce = caseNonce.nonce)
        assertThat(saved).isEqualTo(caseNonce)
    }
}
