package com.healthmetrix.connector.outbox.oauth

import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
@Profile("!oauth")
class MockOauthClient(
    @Qualifier("oauthClientRedirectUri")
    private val oauthRedirectUri: LazySecret<String>,
) : OauthClient {

    // cheat and redirect to our own success endpoint
    override fun buildAuthorizationUrl(state: String, publicKey: String): String =
        UriComponentsBuilder.fromHttpUrl(oauthRedirectUri.requiredValue).apply {
            queryParam("state", state)
            queryParam("code", state.toByteArray().encodeBase64().string)
        }.toUriString()

    override fun getRefreshToken(authCode: String): String? {
        return authCode.toByteArray().encodeBase64().string
    }
}
