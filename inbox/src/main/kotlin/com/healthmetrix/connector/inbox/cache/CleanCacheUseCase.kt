package com.healthmetrix.connector.inbox.cache

import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

@Component
class CleanCacheUseCase(
    @Value("\${clean-cache.max-lifetime}")
    private val maxLifetime: Duration,
    private val domainResourceRepository: DomainResourceRepository,
) {
    operator fun invoke() {
        val deleteBefore = Timestamp.from(Instant.now() - maxLifetime)

        domainResourceRepository.deleteResourcesBeforeTimestamp(deleteBefore) { docs ->
            val formatString = "cleanCache {}"
            val data = kv("deleted", docs.size)
            when (docs.size) {
                0 -> logger.debug(formatString, data)
                else -> logger.info(formatString, data)
            }
        }
    }
}
