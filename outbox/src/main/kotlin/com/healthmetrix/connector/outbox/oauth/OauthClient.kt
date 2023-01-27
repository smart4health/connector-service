package com.healthmetrix.connector.outbox.oauth

import com.healthmetrix.connector.commons.encodeHex
import java.security.SecureRandom

interface OauthClient {

    /**
     * Build a url to redirect the client to, in order to
     * start the oauth flow
     *
     * Follows RFC 6749 with the addition of a `public_key` param
     *
     * @return the url to redirect to
     */
    fun buildAuthorizationUrl(state: String, publicKey: String): String

    /**
     * Make a request to the auth server to exchange the auth
     * code for a refresh token
     *
     * According to the RFC (6749), the refresh token is optional
     * but since we require one, it is not for us
     */
    fun getRefreshToken(authCode: String): String?

    companion object {
        fun createOauthState() = with(SecureRandom()) {
            val buf = ByteArray(16)
            nextBytes(buf)
            buf.encodeHex()
        }
    }
}
