package com.healthmetrix.connector.inbox.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(
    scanBasePackages = [
        "com.healthmetrix.connector.inbox",
        "com.healthmetrix.connector.commons",
    ],
)
@EnableScheduling
class InboxApplication

fun main(args: Array<String>) {
    runApplication<InboxApplication>(*args)
}
