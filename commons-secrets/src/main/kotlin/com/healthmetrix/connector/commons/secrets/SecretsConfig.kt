package com.healthmetrix.connector.commons.secrets

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class SecretsConfig {

    @Bean
    @Profile("!secrets")
    fun mockSecrets(
        @Value("\${spring.datasource.password}")
        postgresPassword: String,
        objectMapper: ObjectMapper,
    ) = Secrets(objectMapper, MockSecrets(postgresPassword))

    @Bean
    @Profile("secrets")
    fun secrets(
        @Value("\${connector.client}")
        client: String,
        @Value("\${connector.stage}")
        stage: String,
        objectMapper: ObjectMapper,
    ) = Secrets(objectMapper, AwsSecrets(client, stage))
}
