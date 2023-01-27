package com.healthmetrix.connector.outbox.sms.config

import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_MAILJET_SMS_TOKEN_SECRET_ID
import com.healthmetrix.connector.commons.secrets.Secrets
import com.healthmetrix.connector.outbox.sms.MailjetSmsService
import com.healthmetrix.connector.outbox.sms.MockSmsService
import com.mailjet.client.ClientOptions
import com.mailjet.client.MailjetClient
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

private const val SMS_TOKEN_SECRET_JSON_KEY = "token"

@Configuration
class SmsConfig {

    @Bean("mailjetSmsToken")
    fun provideSmsToken(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(OUTBOX_MAILJET_SMS_TOKEN_SECRET_ID, SMS_TOKEN_SECRET_JSON_KEY)

    @Bean
    @Profile("sms")
    fun smsService(
        @Qualifier("mailjetSmsToken")
        smsToken: LazySecret<String>,
    ): MailjetSmsService = ClientOptions.builder()
        .bearerAccessToken(smsToken.requiredValue)
        .build()
        .let(::MailjetClient)
        .let(::MailjetSmsService)

    @Bean
    @Profile("!sms")
    fun mockSmsService() = MockSmsService()
}
