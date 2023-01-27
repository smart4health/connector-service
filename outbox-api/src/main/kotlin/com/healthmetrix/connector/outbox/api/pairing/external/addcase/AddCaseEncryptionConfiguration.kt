package com.healthmetrix.connector.outbox.api.pairing.external.addcase

import com.healthmetrix.connector.commons.AllOpen
import com.healthmetrix.connector.commons.crypto.RsaPublicKey
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET
import com.healthmetrix.connector.commons.secrets.Secrets
import com.nimbusds.jose.Algorithm
import com.nimbusds.jose.Requirement
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AddCaseEncryptionConfiguration {

    @Bean("addCaseEncryptionKey")
    fun getAddCaseEncryptionKeySecret(secrets: Secrets): LazySecret<RSAKey> =
        secrets.lazyGet(OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET, "public", Jwk::buildFromB64String)
}

@AllOpen
object Jwk {

    fun buildFromB64String(key: String): RSAKey? = try {
        val rsaPublicKey = RsaPublicKey.buildFromB64String(key)
            ?: throw IllegalArgumentException("Failed decoding base64 secret value to RSA public key")

        RSAKey.Builder(rsaPublicKey)
            .algorithm(Algorithm("RSA-OAEP-256", Requirement.REQUIRED))
            .keyIDFromThumbprint()
            .keyUse(KeyUse.ENCRYPTION)
            .build()
    } catch (ex: IllegalArgumentException) {
        logger.warn("Error occurred trying to serialize RSAPublicKey as JWK RSAKey", ex)
        null
    }
}
