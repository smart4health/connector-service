package com.healthmetrix.connector.inbox.oauth

import com.github.michaelbull.result.Result
import org.springframework.http.HttpStatus

interface OauthClient {
    /**
     * Turn a refresh token into a short lived access token
     *
     * According to the OAuth spec, a refresh token MAY be returned
     * in which case, the client should overwrite old refresh token.
     * This is represented by the .second of the Pair
     */
    fun exchangeRefreshTokenForAccessToken(refreshToken: String): Result<TokenPair, Error>

    data class TokenPair(
        val access: String,
        val refresh: String,
    )

    sealed class Error {
        data class Network(val t: Throwable) : Error() {
            override fun shortDescription(): String = t.javaClass.name
        }

        /**
         * 401 errors where the body matches a known response
         *
         * This indicates a token that cannot be revived essentially
         */
        object InvalidGrant : Error() {
            override fun shortDescription(): String = "InvalidGrant"
        }

        /**
         * Responses that are not:
         * - successful with a parseable body
         * - 401 with a parseable body
         */
        data class UnknownResponse(
            val httpStatus: HttpStatus,
            val body: String,
        ) : Error() {
            override fun shortDescription(): String = httpStatus.toString()
        }

        abstract fun shortDescription(): String
    }
}
