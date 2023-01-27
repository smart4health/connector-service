package com.healthmetrix.connector.inbox.oauth

import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.encodeHex
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!oauth")
class MockOauthClient : OauthClient {
    override fun exchangeRefreshTokenForAccessToken(refreshToken: String) =
        Ok(OauthClient.TokenPair(refreshToken.toByteArray().encodeHex(), "newRefresh"))
}
