package com.healthmetrix.connector.inbox.upload

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.persistence.domainresources.UploadableResource
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.UUID

class ExponentialBackoffFilterTest {
    private val mockUploadAttemptRepository: UploadAttemptRepository = mockk()

    private val fakeProjection =
        UploadableResource(
            internalCaseId = InternalCaseId.randomUUID(),
            internalResourceId = UUID.randomUUID(),
            refreshToken = "refresh token",
            privateKeyPemString = "very secret, much private",
        )

    private val uploadRate = Duration.ofSeconds(10)
    private val minRate = Duration.ofHours(1)

    private val underTest = ExponentialBackoffFilter(mockUploadAttemptRepository, uploadRate, minRate)

    @Test
    fun `a projection with no attempts is allowed to be uploaded`() {
        every { mockUploadAttemptRepository.getLastAttemptAndCountById(any()) } returns null

        assertThat(underTest(fakeProjection)).isEqualTo(true)
    }

    @Test
    fun `a projection with one attempt can be uploaded after one uploadRate`() {
        every { mockUploadAttemptRepository.getLastAttemptAndCountById(any()) } returns
            (Timestamp.from(Instant.now() - uploadRate) to 1)

        assertThat(underTest(fakeProjection)).isEqualTo(true)
    }

    @Test
    fun `a projection with three attempts can be uploaded after 4 upload ticks`() {
        every { mockUploadAttemptRepository.getLastAttemptAndCountById(any()) } returns
            (Timestamp.from(Instant.now() - uploadRate.multipliedBy(4)) to 3)

        assertThat(underTest(fakeProjection)).isEqualTo(true)
    }

    @Test
    fun `a projection with three attempts cannot be uploaded after 3 upload ticks`() {
        every { mockUploadAttemptRepository.getLastAttemptAndCountById(any()) } returns
            (Timestamp.from(Instant.now() - uploadRate.multipliedBy(3)) to 3)

        assertThat(underTest(fakeProjection)).isEqualTo(false)
    }

    @Test
    fun `a projection with a lot of upload attempts will be uploaded after maxRate`() {
        every { mockUploadAttemptRepository.getLastAttemptAndCountById(any()) } returns
            (Timestamp.from(Instant.now() - minRate) to 100)

        assertThat(underTest(fakeProjection)).isEqualTo(true)
    }

    @Test
    fun `a projection with a lot of upload attempts will not be uploaded before maxRate`() {
        every { mockUploadAttemptRepository.getLastAttemptAndCountById(any()) } returns
            (Timestamp.from(Instant.now() - (minRate - Duration.ofMinutes(1))) to 100)

        assertThat(underTest(fakeProjection)).isEqualTo(false)
    }
}
