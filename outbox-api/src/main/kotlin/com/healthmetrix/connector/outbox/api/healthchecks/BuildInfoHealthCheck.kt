package com.healthmetrix.connector.outbox.api.healthchecks

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class BuildInfoHealthCheck(
    @Value("\${info.image-tag}")
    private val imageTag: String,
) : HealthIndicator {
    override fun health(): Health {
        val details = mapOf("imageTag" to imageTag)
        return Health.up().withDetail("n", details.size).withDetails(details).build()
    }
}
