package com.healthmetrix.connector.inbox.outbox

import com.fasterxml.jackson.annotation.JsonProperty
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.NoArg
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.security.PublicKey

interface Outbox {
    fun addCase(
        internalCaseId: InternalCaseId,
        content: ByteArray,
        contentType: String,
        publicKey: PublicKey,
    ): AddCaseResult

    fun getRefreshTokens(): List<RefreshToken>

    fun deleteRefreshTokens(ids: List<InternalCaseId>): List<InternalCaseId>

    fun simpleHealthCheck(): Boolean
}

sealed class AddCaseResult {
    object SuccessCreated : AddCaseResult()
    object SuccessOverridden : AddCaseResult()

    @NoArg
    data class Error(
        @JsonProperty(value = "internalMessage", required = true)
        val internalMessage: String = "Internal server error",
    ) : AddCaseResult()
}

@NoArg
data class RefreshToken(
    @JsonProperty(value = "internalCaseId", required = true)
    val internalCaseId: InternalCaseId,
    @JsonProperty(value = "refreshToken", required = true)
    val refreshToken: String,
)

@Configuration
class OutboxConfiguration {
    @Bean
    @Profile("outbox")
    fun provideHealthmetrixOutbox(
        @Value("\${outbox.host}")
        outboxHost: String,
        @Value("\${outbox.paths.add-case}")
        addCaseEndpoint: String,
        @Value("\${outbox.paths.get-refresh-tokens}")
        getRefreshTokensEndpoint: String,
        @Value("\${outbox.paths.delete-refresh-tokens}")
        deleteRefreshTokensEndpoint: String,
        @Value("\${outbox.paths.simple-health-check}")
        simpleHealthCheckEndpoint: String,
        outboxGatewayBasicAuthCredentials: OutboxGatewayBasicAuthCredentials,
    ) = HealthmetrixOutbox(
        WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(HttpClient.create().proxyWithSystemProperties()))
            .baseUrl(outboxHost)
            .build(),
        addCaseEndpoint,
        getRefreshTokensEndpoint,
        deleteRefreshTokensEndpoint,
        simpleHealthCheckEndpoint,
        outboxGatewayBasicAuthCredentials,
    )

    @Bean
    @Profile("!outbox")
    fun provideMockOutbox() = MockOutbox()
}
