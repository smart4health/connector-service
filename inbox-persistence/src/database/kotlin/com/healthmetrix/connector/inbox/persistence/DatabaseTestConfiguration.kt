package com.healthmetrix.connector.inbox.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.SpringBootConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan

@SpringBootConfiguration
@ComponentScan(
    basePackages = [
        "com.healthmetrix.connector.inbox.persistence",
        "com.healthmetrix.connector.commons.secrets",
    ],
)
class DatabaseTestConfiguration {

    @Bean
    fun provideObjectMapper() = ObjectMapper()
}
