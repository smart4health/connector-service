package com.healthmetrix.connector.outbox.api.healthchecks

import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.boot.actuate.health.Health
import org.springframework.boot.actuate.health.HealthIndicator
import org.springframework.stereotype.Component

@Component
class SecretsHealthCheck<in T : Any>(private val secretBeans: List<LazySecret<T>>) : HealthIndicator {
    override fun health(): Health {
        val details = secretBeans.map {
            it.id to (it.value != null)
        }.toMap()

        return (
            if (details.containsValue(false)) {
                val nonExistingSecrets = details.entries.filter { it.value.not() }.joinToString(separator = ",") { it.key }
                logger.warn("Missing secrets: {}", kv("missing-secrets", nonExistingSecrets))
                Health.down()
            } else {
                Health.up()
            }
            ).withDetail("n", details.size).withDetails(details).build()
    }
}
