package com.healthmetrix.connector.inbox.outbox

import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_GATEWAY_BASIC_AUTH
import com.healthmetrix.connector.commons.secrets.Secrets
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OutboxConfig {
    @Bean("outboxGatewayUser")
    fun provideOutboxGatewayUser(secrets: Secrets): LazySecret<String> = secrets.lazyGet(
        OUTBOX_GATEWAY_BASIC_AUTH,
        "user",
    )

    @Bean("outboxGatewayPassword")
    fun provideOutboxGatewayPassword(secrets: Secrets): LazySecret<String> = secrets.lazyGet(
        OUTBOX_GATEWAY_BASIC_AUTH,
        "password",
    )

    @Bean
    fun provideOutboxGatewayCredentials(
        @Qualifier("outboxGatewayUser")
        user: LazySecret<String>,
        @Qualifier("outboxGatewayPassword")
        password: LazySecret<String>,
    ): OutboxGatewayBasicAuthCredentials = OutboxGatewayBasicAuthCredentials(user, password)
}

data class OutboxGatewayBasicAuthCredentials(val user: LazySecret<String>, val password: LazySecret<String>) {
    fun encode(): String = "${user.requiredValue}:${password.requiredValue}".toByteArray().encodeBase64().string
}
