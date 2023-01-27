package com.healthmetrix.connector.outbox.api.healthchecks

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class SimpleHealthController {
    @GetMapping("/v1/internal/health")
    fun simpleHealthCheck(): ResponseEntity<Any?> = ResponseEntity.ok().build()
}
