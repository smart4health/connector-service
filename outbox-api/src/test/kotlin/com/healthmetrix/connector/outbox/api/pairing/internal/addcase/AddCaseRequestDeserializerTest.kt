package com.healthmetrix.connector.outbox.api.pairing.internal.addcase

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.RsaPrivateKey
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.SecretNotFoundException
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.http.MediaType
import java.io.IOException
import java.util.Locale

internal class AddCaseRequestDeserializerTest {

    private val objectMapper: ObjectMapper = mockk()
    private val xmlMapper: XmlMapper = mockk()
    private val rsaPrivateKey: RsaPrivateKey = mockk()
    private val secret: LazySecret<RsaPrivateKey> = mockk() {
        every { requiredValue } returns rsaPrivateKey
    }

    private val underTest = AddCaseRequestDeserializer(objectMapper, xmlMapper, secret)

    private val content = "le content"
    private val pubKey = "le public key"
    private val email = "email"
    private val mobileNumber = "mobileNumber"
    private val lang = Locale.GERMAN.toLanguageTag()
    private val addCaseRequestContent = AddCaseRequestContent(email, mobileNumber, lang)
    private val expected = AddCaseRequest(email, mobileNumber, B64String(pubKey), lang)

    @Test
    fun `AddCaseRequestDeserialization should deserialize json correctly`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), MediaType.APPLICATION_JSON_VALUE, pubKey)
        every { objectMapper.readValue(any<ByteArray>(), any<Class<Any>>()) } returns addCaseRequestContent

        val result = underTest.invoke(requestWrapper)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `AddCaseRequestDeserialization should deserialize xml correctly`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), MediaType.APPLICATION_XML_VALUE, pubKey)
        every { xmlMapper.readValue(any<ByteArray>(), any<Class<Any>>()) } returns addCaseRequestContent

        val result = underTest.invoke(requestWrapper)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `AddCaseRequestDeserialization should deserialize encrypted json correctly`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), "application/json+encrypted", pubKey)
        every { rsaPrivateKey.decrypt(any()) } returns "encrypted".toByteArray()
        every { objectMapper.readValue(any<ByteArray>(), any<Class<Any>>()) } returns addCaseRequestContent

        val result = underTest.invoke(requestWrapper)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `AddCaseRequestDeserialization should deserialize encrypted xml correctly`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), "application/xml+encrypted", pubKey)
        every { rsaPrivateKey.decrypt(any()) } returns "encrypted".toByteArray()
        every { xmlMapper.readValue(any<ByteArray>(), any<Class<Any>>()) } returns addCaseRequestContent

        val result = underTest.invoke(requestWrapper)

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `AddCaseRequestDeserialization should throw an Exception for unsupported request content types`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), MediaType.TEXT_PLAIN_VALUE, pubKey)

        assertThrows<AddCaseRequestDeserializationException> { underTest.invoke(requestWrapper) }
    }

    @Test
    fun `AddCaseRequestDeserialization should throw an Exception if deserialization fails`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), MediaType.APPLICATION_JSON_VALUE, pubKey)
        every { objectMapper.readValue(any<ByteArray>(), any<Class<Any>>()) } throws IOException()

        assertThrows<AddCaseRequestDeserializationException> { underTest.invoke(requestWrapper) }
    }

    @Test
    fun `AddCaseRequestDeserialization should throw an Exception if decryption fails`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), "application/json+encrypted", pubKey)
        every { rsaPrivateKey.decrypt(any()) } returns null

        assertThrows<AddCaseRequestDeserializationException> { underTest.invoke(requestWrapper) }
    }

    @Test
    fun `AddCaseRequestDeserialization should throw an Exception if decryption key could not be retrieved`() {
        val requestWrapper = AddCaseRequestWrapper(content.toByteArray(), "application/json+encrypted", pubKey)
        every { secret.requiredValue } throws SecretNotFoundException()

        assertThrows<AddCaseRequestDeserializationException> { underTest.invoke(requestWrapper) }
    }
}
