package com.healthmetrix.connector.inbox.api.config

import com.healthmetrix.connector.commons.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

private const val JAVAX_NET_SSL_TRUST_STORE = "javax.net.ssl.trustStore"
private const val JAVAX_NET_SSL_TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword"
private const val TLS_VERSION = "TLSv1.2"

@Configuration
class SslConfig {

    @Bean
    fun x509TrustManager(trustManagerFactory: TrustManagerFactory): X509TrustManager =
        // there should be exactly one Trust Manager and it should be of type X509TrustManager
        trustManagerFactory.trustManagers.filterIsInstance<X509TrustManager>().first()

    @Bean
    @ConditionalOnProperty(prefix = "truststore", name = ["location", "password"], matchIfMissing = false)
    fun sslContext(trustManager: X509TrustManager): SSLContext = SSLContext.getInstance(TLS_VERSION).apply {
        init(null, arrayOf(trustManager), null)
    }

    @Bean
    @ConditionalOnMissingBean
    fun defaultSslContext(trustManager: X509TrustManager): SSLContext = SSLContext.getDefault()

    @Bean
    @ConditionalOnProperty(prefix = "truststore", name = ["location", "password"], matchIfMissing = false)
    fun trustManagerFactory(@Value("\${truststore.location}") trustStoreLocation: String, @Value("\${truststore.password}") trustStorePassword: String): TrustManagerFactory {
        if (trustStoreLocation.isBlank() || trustStorePassword.isBlank()) {
            throw IllegalArgumentException("truststore.location and truststore.password must not be blank")
        }

        System.setProperty(JAVAX_NET_SSL_TRUST_STORE, trustStoreLocation)
        System.setProperty(JAVAX_NET_SSL_TRUST_STORE_PASSWORD, trustStorePassword)

        return TrustManagerFactory.getInstance("pkix").apply {
            logger.info("Using JVM trust store at location $trustStoreLocation")
            init(getJvmKeyStore(trustStoreLocation, trustStorePassword))
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun defaultTrustManagerFactory(): TrustManagerFactory =
        TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm()).apply {
            logger.info("Using JVM default trust store")
            init(null as KeyStore?)
        }

    private fun getJvmKeyStore(trustStoreLocation: String, trustStorePassword: String): KeyStore = KeyStore.getInstance("jks").apply {
        load(FileInputStream(trustStoreLocation), trustStorePassword.toCharArray())
    }
}
