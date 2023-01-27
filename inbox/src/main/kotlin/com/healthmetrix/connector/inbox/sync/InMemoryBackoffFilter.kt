package com.healthmetrix.connector.inbox.sync

import com.healthmetrix.connector.commons.InternalCaseId
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import kotlin.math.max
import kotlin.math.pow

/**
 * @param minDeltaT the duration between the first failed attempt and the second attempt.
 *                  the value name "max-rate" represents the rate of attempt/duration
 * @param maxDeltaT the maximum allowed duration between attempts.  The value name min-rate
 *                  represents the rate of attempt/duration
 */
@Component
class InMemoryBackoffFilter(
    @Value("\${renewal.backoff.max-rate}")
    private val minDeltaT: Duration,
    @Value("\${renewal.backoff.min-rate}")
    private val maxDeltaT: Duration,
    private val clock: () -> Instant = Instant::now,
) {

    private val attempts = mutableMapOf<InternalCaseId, List<Instant>>()

    fun failed(key: InternalCaseId, at: Instant = Instant.now()) {
        attempts[key] = attempts.getOrDefault(key, listOf()) + at
    }

    fun success(key: InternalCaseId) {
        attempts.remove(key)
    }

    /**
     * Return true if:
     *
     * the difference in time between now and the last attempt (currentDeltaT)
     * is greater than (or equal to) either the maximum attempt delta t or the
     * minimum attempt delta t * 2^(number of attempts)
     *
     * since this will generally be called at a fixed rate of 10 seconds or so,
     * which is also the maximum attempt rate,
     */
    fun shouldAttempt(key: InternalCaseId): Boolean {
        val attempts = attempts[key] ?: return true

        val currentDeltaT = attempts
            .lastOrNull()
            ?.let { Duration.between(it, clock()) }
            ?: return true

        // subtract one to start at maxRate instead of maxRate * 2
        val n = max(attempts.size - 1, 0)
        val targetDeltaT = try {
            minDeltaT.multipliedBy(2.0.pow(n).toLong())
        } catch (ex: Exception) {
            maxDeltaT
        }

        return currentDeltaT >= min(targetDeltaT, maxDeltaT)
    }

    private fun min(a: Duration, b: Duration): Duration = if (a > b) b else a
}
