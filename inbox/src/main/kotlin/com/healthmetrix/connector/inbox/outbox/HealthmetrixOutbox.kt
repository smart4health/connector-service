package com.healthmetrix.connector.inbox.outbox

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.commons.logger
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import java.security.PublicKey

/**
 * Abstraction for communications with outbox, can be
 * easily mocked
 *
 * open for mocking, but could eventually be behind an
 * interface
 */
// TODO: Add tests
class HealthmetrixOutbox(
    private val client: WebClient,
    private val addCaseEndpoint: String,
    private val getRefreshTokensEndpoint: String,
    private val deleteRefreshTokensEndpoint: String,
    private val simpleHealthCheckEndpoint: String,
    private val outboxGatewayBasicAuthCredentials: OutboxGatewayBasicAuthCredentials,
) : Outbox {

    @Suppress("EXPERIMENTAL_API_USAGE")
    override fun addCase(
        internalCaseId: InternalCaseId,
        content: ByteArray,
        contentType: String,
        publicKey: PublicKey,
    ): AddCaseResult {
        val requestBody = json {
            "content" to content
            "contentType" to contentType
            "publicKey" to publicKey.encoded.encodeBase64().string
        }

        val response = runBlocking {
            client
                .put()
                .uri(addCaseEndpoint.format(internalCaseId.toString()))
                .headers { it.setBasicAuth(outboxGatewayBasicAuthCredentials.encode()) }
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody.toString())
                .awaitExchange()
        }

        if (response.statusCode().is2xxSuccessful) {
            response.releaseBody()
        }

        return when (response.statusCode()) {
            HttpStatus.CREATED -> AddCaseResult.SuccessCreated
            HttpStatus.OK -> AddCaseResult.SuccessOverridden
            else -> runBlocking {
                response.awaitBody<AddCaseResult.Error>()
            }
        }
    }

    override fun getRefreshTokens(): List<RefreshToken> = try {
        runBlocking {
            client
                .get()
                .uri(getRefreshTokensEndpoint)
                .headers { it.setBasicAuth(outboxGatewayBasicAuthCredentials.encode()) }
                .retrieve()
                .awaitBody<List<RefreshToken>>()
        }
    } catch (ex: Exception) {
        logger.warn("Error fetching keys", ex)
        listOf()
    }

    override fun deleteRefreshTokens(ids: List<InternalCaseId>): List<InternalCaseId> = ids.filter { id ->
        try {
            runBlocking {
                client.delete()
                    .uri("${deleteRefreshTokensEndpoint.trimEnd('/')}/$id")
                    .headers { it.setBasicAuth(outboxGatewayBasicAuthCredentials.encode()) }
                    .awaitExchange().run {
                        releaseBody().awaitFirstOrNull()
                        statusCode().is2xxSuccessful
                    }
            }
        } catch (ex: Exception) {
            logger.warn("Error deleting refreshTokens", ex)
            false
        }
    }

    override fun simpleHealthCheck() = runBlocking {
        try {
            client.get()
                .uri(simpleHealthCheckEndpoint)
                .headers { it.setBasicAuth(outboxGatewayBasicAuthCredentials.encode()) }
                .awaitExchange()
                .awaitEntity<Void>()
                .statusCode == HttpStatus.OK
        } catch (ex: Exception) {
            false
        }
    }
}
