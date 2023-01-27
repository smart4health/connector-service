package com.healthmetrix.connector.inbox.persistence

import com.healthmetrix.connector.commons.secrets.INBOX_POSTGRES_PASSWORD
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.Secrets
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#howto-two-datasources
 *
 * No configuration properties on the second because we don't have any at this time
 */
@Configuration
class DataSourceConfiguration {

    @ConfigurationProperties("spring.datasource")
    @Bean
    @Primary
    @Profile("postgres")
    fun provideDataSourceProperties() = DataSourceProperties()

    @Bean("postgresPassword")
    @Profile("postgres")
    fun providePostgresPassword(secrets: Secrets): LazySecret<String> =
        secrets.lazyGet(INBOX_POSTGRES_PASSWORD)

    @Bean
    @Primary
    @Profile("postgres")
    fun provideDataSource(
        @Qualifier("postgresPassword")
        postgresPassword: LazySecret<String>,
        dataSourceProperties: DataSourceProperties,
    ): DataSource = dataSourceProperties.initializeDataSourceBuilder().apply {
        password(postgresPassword.requiredValue)
    }.build()
}
