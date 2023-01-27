package com.healthmetrix.connector.inbox.sync

import com.healthmetrix.connector.commons.InternalCaseId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import kotlin.math.pow

class InMemoryBackoffFilterTest {

    private val minDeltaT: Duration = Duration.ofSeconds(10)

    private val now = Instant.now()

    private val underTest = InMemoryBackoffFilter(
        minDeltaT = minDeltaT,
        maxDeltaT = Duration.ofHours(24),
        clock = { now },
    )

    @Test
    fun `an id with no attempts is allowed to upload`() {
        assertThat(underTest.shouldAttempt(InternalCaseId.randomUUID())).isTrue()
    }

    @Test
    fun `an id with one attempt is allowed to upload after one minDeltaT`() {
        val id = InternalCaseId.randomUUID()
        underTest.failed(id, now - minDeltaT)

        assertThat(underTest.shouldAttempt(id)).isTrue()
    }

    @Test
    fun `an id with three attempts is allowed to upload after a duration of minDeltaT x 2^3`() {
        val id = InternalCaseId.randomUUID()
        underTest.failed(id, now - minDeltaT.multipliedBy(2.0.pow(3).toLong()))
        underTest.failed(id, now - minDeltaT.multipliedBy(2.0.pow(3).toLong()))
        underTest.failed(id, now - minDeltaT.multipliedBy(2.0.pow(3).toLong()))

        assertThat(underTest.shouldAttempt(id)).isTrue()
    }

    @Test
    fun `an id with multiple failed attempts and then a success is allowed to upload`() {
        val id = InternalCaseId.randomUUID()
        underTest.failed(id, now)
        underTest.failed(id, now)
        underTest.failed(id, now)
        underTest.success(id)

        assertThat(underTest.shouldAttempt(id)).isTrue()
    }

    @Test
    fun `an id with an attempt less than minDeltaT ago is not allowed to upload`() {
        val id = InternalCaseId.randomUUID()
        underTest.failed(id, now - minDeltaT.dividedBy(2))

        assertThat(underTest.shouldAttempt(id)).isFalse()
    }
}
