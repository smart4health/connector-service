package com.healthmetrix.connector.outbox.oauth

import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.commons.encodeURL
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_OAUTH_CLIENT_SECRET_ID
import com.healthmetrix.connector.commons.secrets.Secrets
import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import java.time.Duration
import java.util.concurrent.TimeUnit

@Configuration
class OauthClientConfig {
    @Bean("oauthWebClient")
    fun provideWebClient(
        @Value("\${oauth.host}")
        host: String,
        @Value("\${oauth.connect-timeout:30s}")
        connectTimeout: Duration,
        @Value("\${oauth.read-timeout:30s}")
        readTimeout: Duration,
    ): WebClient {
        val httpClient = HttpClient.create()
            .proxyWithSystemProperties()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.toMillis().toInt())
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(readTimeout.toMillis(), TimeUnit.MILLISECONDS))
            }

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(host)
            .build()
    }

    @Bean("oauthClientId")
    fun provideClientId(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(OUTBOX_OAUTH_CLIENT_SECRET_ID, "id")

    @Bean("oauthClientSecret")
    fun provideClientSecret(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(OUTBOX_OAUTH_CLIENT_SECRET_ID, "secret")

    @Bean("oauthClientRedirectUri")
    fun provideClientRedirectUri(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(OUTBOX_OAUTH_CLIENT_SECRET_ID, "redirect_uri")

    @Bean
    fun provideCredentials(
        @Qualifier("oauthClientId")
        id: LazySecret<String>,
        @Qualifier("oauthClientSecret")
        secret: LazySecret<String>,
    ): OauthCredentials = OauthCredentials(id, secret)
}

data class OauthCredentials(val id: LazySecret<String>, val secret: LazySecret<String>) {
    fun encoded() = "${id.requiredValue.encodeURL()}:${secret.requiredValue.encodeURL()}".toByteArray().encodeBase64().string
}
