package com.healthmetrix.connector.inbox.persistence

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.secrets.INBOX_DATABASE_ENCRYPTION_AES_KEY
import com.healthmetrix.connector.commons.secrets.Secrets
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.transaction.annotation.EnableTransactionManagement
import javax.persistence.AttributeConverter
import javax.persistence.Converter

@Configuration
@EnableJpaRepositories
@EntityScan
@EnableTransactionManagement
class RepositoryConfig {

    @Bean("databaseEncryptionKey")
    fun databaseEncryptionKey(secrets: Secrets) =
        secrets.lazyGet(INBOX_DATABASE_ENCRYPTION_AES_KEY, AesKey.Companion::buildFromB64String)
}

// NOTE: copied from outbox-persistence
//       could probably move to commons but it would add a dependency on jakarta
@Converter
class B64StringConverter : AttributeConverter<B64String, String> {
    override fun convertToDatabaseColumn(attribute: B64String) = attribute.string

    override fun convertToEntityAttribute(dbData: String) = B64String(dbData)
}
