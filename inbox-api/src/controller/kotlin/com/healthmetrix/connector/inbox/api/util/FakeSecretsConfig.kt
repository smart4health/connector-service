package com.healthmetrix.connector.inbox.api.util

import com.healthmetrix.connector.commons.secrets.LazySecret
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FakeSecretsConfig {
    @Bean("oauthClientId")
    fun provideFakeOauthClientId(): LazySecret<String> = LazySecret("id", { it }) {
        "fake-oauth-client-id"
    }
}
