package com.healthmetrix.connector.inbox.upload

import com.healthmetrix.connector.inbox.persistence.domainresources.UploadableResource
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.pow

@Component
class ExponentialBackoffFilter(
    private val uploadAttemptRepository: UploadAttemptRepository,
    @Value("\${upload.fixed-rate}")
    private val uploadRate: Duration,
    @Value("\${upload.min-rate}")
    private val maxDeltaT: Duration,
) : BackoffPredicate {
    override fun invoke(resource: UploadableResource): Boolean {
        val (lastAttempt, count) = uploadAttemptRepository.getLastAttemptAndCountById(resource.internalResourceId)
            ?: return true

        val currentDeltaT = Duration.between(lastAttempt.toInstant(), Instant.now())
        val n = max(count - 1, 0) // count shouldn't be zero but it doesn't hurt
        val minDeltaT = try {
            uploadRate.multipliedBy(2.0.pow(n).toLong())
        } catch (ex: Exception) {
            // exponentials can easily create numbers that are too big,
            // so catch exceptions (from Duration construction or if n
            // is bigger than 63 or so)
            maxDeltaT
        }

        return currentDeltaT >= min(minDeltaT, maxDeltaT)
    }

    private fun min(a: Duration, b: Duration): Duration = if (a > b) b else a
}
