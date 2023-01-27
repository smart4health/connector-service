package com.healthmetrix.connector.inbox.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.mapBoth
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.runCatching
import com.healthmetrix.connector.commons.NoArg
import com.healthmetrix.connector.commons.secrets.LazySecret
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitEntity
import org.springframework.web.reactive.function.client.awaitExchange

@Component
@Profile("oauth")
class D4LOauthClient(
    @Qualifier("oauthWebClient")
    private val webClient: WebClient,
    @Value("\${oauth.paths.access-token}")
    private val accessTokenRequestPath: String,
    @Qualifier("oauthClientRedirectUri")
    private val oauthRedirectUri: LazySecret<String>,
    private val oauthCredentials: OauthCredentials,
    private val objectMapper: ObjectMapper,
) : OauthClient {

    override fun exchangeRefreshTokenForAccessToken(
        refreshToken: String,
    ): Result<OauthClient.TokenPair, OauthClient.Error> = binding {
        val response = runCatching {
            runBlocking {
                webClient.post()
                    .uri(accessTokenRequestPath)
                    .body(
                        BodyInserters.fromFormData(
                            LinkedMultiValueMap<String, String>().apply {
                                add("grant_type", "refresh_token")
                                add("refresh_token", refreshToken)
                                add("redirect_uri", oauthRedirectUri.requiredValue)
                            },
                        ),
                    )
                    .headers { them ->
                        them.setBasicAuth(oauthCredentials.encoded())
                    }
                    .awaitExchange()
                    .awaitEntity<String>()
            }
        }.mapError(OauthClient.Error::Network).bind()

        val accessTokenResponse = when {
            response.statusCode.is2xxSuccessful -> runCatching {
                objectMapper.readValue(response.body.orEmpty(), AccessTokenResponse::class.java)
            }.mapError {
                OauthClient.Error.UnknownResponse(response.statusCode, response.body.orEmpty())
            }

            response.statusCode == HttpStatus.UNAUTHORIZED -> runCatching {
                objectMapper.readValue(response.body.orEmpty(), D4LErrorResponse::class.java)
            }.mapBoth(
                success = { OauthClient.Error.InvalidGrant },
                failure = { OauthClient.Error.UnknownResponse(response.statusCode, response.body.orEmpty()) },
            ).let(::Err)

            else -> OauthClient.Error.UnknownResponse(response.statusCode, response.body.orEmpty()).let(::Err)
        }.bind()

        OauthClient.TokenPair(accessTokenResponse.accessToken, accessTokenResponse.refreshToken)
    }

    @NoArg
    data class AccessTokenResponse(
        @JsonProperty("access_token")
        val accessToken: String,
        @JsonProperty("token_type")
        val tokenType: String,
        @JsonProperty("refresh_token")
        val refreshToken: String,
    )

    @NoArg
    data class D4LErrorResponse(
        val error: String,
        @JsonProperty("error_description")
        val errorDescription: String,
    )
}
