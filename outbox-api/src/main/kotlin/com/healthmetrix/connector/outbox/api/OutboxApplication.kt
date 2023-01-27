package com.healthmetrix.connector.outbox.api

import com.healthmetrix.connector.outbox.config.InvitationEmailConfig
import com.healthmetrix.connector.outbox.config.SmsFactory
import com.healthmetrix.connector.outbox.config.TimeFrameFactory
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(
    scanBasePackages = [
        "com.healthmetrix.connector.outbox",
        "com.healthmetrix.connector.commons",
    ],
)
@EnableConfigurationProperties(
    value = [
        InvitationEmailConfig::class,
        TimeFrameFactory::class,
        SmsFactory::class,
    ],
)
class OutboxApplication

fun main(args: Array<String>) {
    runApplication<OutboxApplication>(*args)
}
