package com.healthmetrix.connector.outbox.api.pairing.internal.addcase

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.RsaPrivateKey
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET
import com.healthmetrix.connector.commons.secrets.Secrets
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType

class AddCaseRequestDeserializer(
    private val objectMapper: ObjectMapper,
    private val xmlMapper: XmlMapper,
    private val decryptionKey: LazySecret<RsaPrivateKey>,
) {

    private val applicationJsonEncrypted = MediaType("application", "json+encrypted")
    private val applicationXmlEncrypted = MediaType("application", "xml+encrypted")

    operator fun invoke(requestWrapper: AddCaseRequestWrapper): AddCaseRequest =
        try {
            val content = when (requestWrapper.contentType) {
                MediaType.APPLICATION_JSON_VALUE -> requestWrapper.content.deserializeJson()
                MediaType.APPLICATION_XML_VALUE -> requestWrapper.content.deserializeXml()
                applicationJsonEncrypted.toString() -> requestWrapper.content.decrypt().deserializeJson()
                applicationXmlEncrypted.toString() -> requestWrapper.content.decrypt().deserializeXml()
                else -> throw IllegalArgumentException("Content type ${requestWrapper.contentType} not supported for AddCaseRequest")
            }

            AddCaseRequest(content.email, content.mobileNumber, B64String(requestWrapper.publicKey), content.lang)
        } catch (ex: Exception) {
            logger.warn("Error deserializing addCaseRequest", ex)
            throw AddCaseRequestDeserializationException("Error deserializing addCaseRequest", ex)
        }

    private fun ByteArray.deserializeXml() = xmlMapper.readValue(this, AddCaseRequestContent::class.java)

    private fun ByteArray.deserializeJson() = objectMapper.readValue(this, AddCaseRequestContent::class.java)

    private fun ByteArray.decrypt(): ByteArray = decryptionKey.requiredValue.decrypt(this)
        ?: throw IllegalArgumentException("Failed decrypting addCaseRequest")
}

@Configuration
class AddCaseRequestDeserializationConfiguration {

    @Bean("addCaseDecryptionKey")
    fun getAddCaseDecryptionKeySecret(secrets: Secrets): LazySecret<RsaPrivateKey> =
        secrets.lazyGet(OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET, "private", RsaPrivateKey.Companion::buildFromB64String)

    @Bean
    fun getAddCaseRequestDeserialization(
        @Qualifier("addCaseDecryptionKey")
        decryptionKey: LazySecret<RsaPrivateKey>,
        objectMapper: ObjectMapper,
    ) = AddCaseRequestDeserializer(objectMapper, XmlMapper(), decryptionKey)
}

class AddCaseRequestDeserializationException(message: String, cause: Throwable) : Exception(message, cause)
