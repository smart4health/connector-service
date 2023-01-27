package com.healthmetrix.connector.outbox.email.config

import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_MAILJET_SECRET_ID
import com.healthmetrix.connector.commons.secrets.Secrets
import com.healthmetrix.connector.outbox.email.MailjetEmailService
import com.healthmetrix.connector.outbox.email.MockEmailService
import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private const val API_KEY_KEY = "key"
private const val API_SECRET_KEY = "secret"

@Configuration
class MailjetConfig {

    @Bean("mailjetKey")
    fun provideMailjetKey(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(OUTBOX_MAILJET_SECRET_ID, API_KEY_KEY)

    @Bean("mailjetSecret")
    fun provideMailjetSecret(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(OUTBOX_MAILJET_SECRET_ID, API_SECRET_KEY)

    @Bean
    @Profile("email")
    fun emailService(
        @Qualifier("mailjetKey")
        apiKey: LazySecret<String>,
        @Qualifier("mailjetSecret")
        apiSecret: LazySecret<String>,
    ): MailjetEmailService = ClientOptions.builder()
        .apiKey(apiKey.requiredValue)
        .apiSecretKey(apiSecret.requiredValue)
        .build()
        .let(::MailjetClient)
        .let(::MailjetEmailService)

    @Bean
    @Profile("!email")
    fun mockEmailService() = MockEmailService()
}
