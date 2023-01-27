package com.healthmetrix.connector.outbox.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.connector.commons.NoArg
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import io.netty.channel.ConnectTimeoutException
import io.netty.handler.timeout.ReadTimeoutException
import kotlinx.coroutines.runBlocking
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange
import org.springframework.web.util.UriComponentsBuilder

private val SCOPES = listOf(
    "rec:a",
    "attachment:a",
    "user:r",
    "perm:r",
)

@Component
@Profile("oauth")
class D4LOauthClient(
    @Qualifier("oauthWebClient")
    private val webClient: WebClient,
    @Value("\${oauth.host}")
    private val d4lOauthHost: String,
    @Value("\${oauth.paths.authorization}")
    private val authorizationRequestPath: String,
    @Value("\${oauth.paths.refresh-token}")
    private val refreshTokenRequestPath: String,
    @Qualifier("oauthClientRedirectUri")
    private val oauthRedirectUri: LazySecret<String>,
    private val oauthCredentials: OauthCredentials,
    private val objectMapper: ObjectMapper,
) : OauthClient {

    /**
     * public key is already stored as a .encoded.encodeBase64() string
     */
    override fun buildAuthorizationUrl(state: String, publicKey: String): String {
        val serializedPublicKey = json {
            "t" to "apub"
            "v" to 1
            "pub" to publicKey
        }.toString().toByteArray().encodeBase64().string

        return UriComponentsBuilder.fromHttpUrl(d4lOauthHost).apply {
            path(authorizationRequestPath)
            queryParam("response_type", "code")
            queryParam("client_id", oauthCredentials.id.requiredValue)
            queryParam("scope", SCOPES.joinToString(" "))
            queryParam("state", state)
            queryParam("public_key", serializedPublicKey)
        }.toUriString()
    }

    override fun getRefreshToken(authCode: String): String? {
        val response = runBlocking {
            logger.info("POST to $refreshTokenRequestPath")

            try {
                webClient.post()
                    .uri(refreshTokenRequestPath)
                    .body(
                        BodyInserters.fromFormData(
                            LinkedMultiValueMap<String, String>().apply {
                                add("grant_type", "authorization_code")
                                add("code", authCode)
                                add("redirect_uri", oauthRedirectUri.requiredValue)
                            },
                        ),
                    )
                    .headers { headers ->
                        headers.setBasicAuth(oauthCredentials.encoded())
                    }
                    .awaitExchange()
                    .awaitEntity<String>()
            } catch (ex: ReadTimeoutException) {
                logger.warn("Read timeout", ex)
                null
            } catch (ex: ConnectTimeoutException) {
                logger.warn("Connect timeout", ex)
                null
            } catch (ex: Exception) {
                logger.warn("Other exception", ex)
                null
            }
        } ?: return null

        return try {
            logger.info("Response received {}", kv("statusCode", response.statusCode))
            objectMapper.readValue(response.body ?: "", RefreshTokenResponse::class.java).refreshToken
        } catch (ex: Exception) {
            if (!response.statusCode.is2xxSuccessful) {
                logger.warn("Exception parsing refresh token response {}", kv("body", response.body ?: ""))
            }
            null
        }
    }

    @NoArg
    data class RefreshTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("refresh_token")
        val refreshToken: String,
    )
}
