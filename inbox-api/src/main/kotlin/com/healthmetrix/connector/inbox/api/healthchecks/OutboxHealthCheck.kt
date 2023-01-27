package com.healthmetrix.connector.inbox.api.healthchecks

import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.outbox.Outbox
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class OutboxHealthCheck(private val outbox: Outbox) : HealthIndicator {
    override fun health(): Health = when (outbox.simpleHealthCheck()) {
        true -> Health.up()
        false -> {
            logger.warn("Outbox could not be reached")
            Health.down()
        }
    }.build()
}
