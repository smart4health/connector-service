package com.healthmetrix.connector.inbox.persistence.uploadattempts

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResource
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

@DataJpaTest(
    properties = ["spring.liquibase.change-log=classpath:db/changelog.yaml"],
)
class UploadAttemptRepositoryTest {

    @Autowired
    private lateinit var caseRepository: CaseRepository

    @Autowired
    private lateinit var domainResourceRepository: DomainResourceRepository

    @Autowired
    private lateinit var uploadAttemptRepository: UploadAttemptRepository

    private val internalResourceId = UUID.randomUUID()
    private val now = Timestamp.from(Instant.now())

    @BeforeEach
    fun beforeAll() {
        caseRepository.save(
            CaseEntity(
                UUID.randomUUID(),
                "externalCaseId1",
                B64String("blahblah"),
            ),
        )
        val domainResource =
            DomainResource(
                internalResourceId,
                "externalCaseId1",
                now,
                "fake json",
            )
        domainResourceRepository.save(domainResource)
    }

    @Test
    fun `getting last attempt and count with three attempts returns the latest attempt and 3`() {
        uploadAttemptRepository.save(
            UploadAttemptEntity(
                UUID.randomUUID(),
                internalResourceId,
                now + 5.minutes,
            ),
        )
        uploadAttemptRepository.save(
            UploadAttemptEntity(
                UUID.randomUUID(),
                internalResourceId,
                now + 10.minutes,
            ),
        )
        uploadAttemptRepository.save(
            UploadAttemptEntity(
                UUID.randomUUID(),
                internalResourceId,
                now + 15.minutes,
            ),
        )

        val pair = uploadAttemptRepository.getLastAttemptAndCountById(internalResourceId)

        assertThat(pair).isEqualTo(now + 15.minutes to 3)
    }

    @Test
    fun `deleting the domain resource deletes the upload attempts`() {
        uploadAttemptRepository.save(
            UploadAttemptEntity(
                UUID.randomUUID(),
                internalResourceId,
                now + 5.minutes,
            ),
        )
        uploadAttemptRepository.save(
            UploadAttemptEntity(
                UUID.randomUUID(),
                internalResourceId,
                now + 10.minutes,
            ),
        )
        uploadAttemptRepository.save(
            UploadAttemptEntity(
                UUID.randomUUID(),
                internalResourceId,
                now + 15.minutes,
            ),
        )

        domainResourceRepository.deleteById(internalResourceId)

        assertThat(uploadAttemptRepository.getLastAttemptAndCountById(internalResourceId)).isNull()
    }

    private operator fun Timestamp.plus(duration: Duration): Timestamp =
        Timestamp.from(toInstant() + duration)

    private val Number.minutes: Duration
        get() = Duration.ofMinutes(this.toLong())
}
