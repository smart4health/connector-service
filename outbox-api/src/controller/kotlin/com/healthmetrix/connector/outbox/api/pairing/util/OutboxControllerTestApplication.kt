package com.healthmetrix.connector.outbox.api.pairing.util

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

/**
 * Just a dummy spring boot application for spinning up the web server on controller tests
 */
@SpringBootApplication(scanBasePackages = ["com.healthmetrix.connector.commons.web"])
class OutboxControllerTestApplication

fun main(args: Array<String>) {
    runApplication<OutboxControllerTestApplication>(*args)
}
