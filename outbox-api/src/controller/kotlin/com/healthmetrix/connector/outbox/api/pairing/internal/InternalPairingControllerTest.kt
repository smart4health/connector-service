package com.healthmetrix.connector.outbox.api.pairing.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.crypto.RsaPublicKey
import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.commons.secrets.OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET
import com.healthmetrix.connector.commons.secrets.Secrets
import com.healthmetrix.connector.commons.secrets.SecretsConfig
import com.healthmetrix.connector.outbox.api.pairing.internal.addcase.AddCaseRequestContent
import com.healthmetrix.connector.outbox.api.pairing.internal.addcase.AddCaseRequestDeserializationConfiguration
import com.healthmetrix.connector.outbox.api.pairing.internal.addcase.AddCaseRequestWrapper
import com.healthmetrix.connector.outbox.api.pairing.util.OutboxControllerTestApplication
import com.healthmetrix.connector.outbox.usecases.AddCaseUseCase
import com.healthmetrix.connector.outbox.usecases.DeleteRefreshTokenUseCase
import com.healthmetrix.connector.outbox.usecases.GetRefreshTokensUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put
import org.springframework.web.util.NestedServletException
import java.util.Locale
import java.util.UUID

@SpringBootTest(
    classes = [
        OutboxControllerTestApplication::class,
        InternalPairingController::class,
        AddCaseRequestDeserializationConfiguration::class,
        SecretsConfig::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class InternalPairingControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var mockAddCaseUseCase: AddCaseUseCase

    @MockBean
    private lateinit var mockGetRefreshTokensUseCase: GetRefreshTokensUseCase

    @MockBean
    private lateinit var mockDeleteRefreshTokenUseCase: DeleteRefreshTokenUseCase

    @Autowired
    private lateinit var secrets: Secrets

    private val fakeId = InternalCaseId.randomUUID()

    private val xmlMapper = XmlMapper()
    private val jsonMapper = ObjectMapper()

    private val publicKey = "Not a real pub key"
    private val email = "hi@example.com"
    private val mobileNumber = "not a real phone number"
    private val locale = Locale.GERMAN.toLanguageTag()

    private val requestContent = AddCaseRequestContent(email, mobileNumber, locale)

    private val applicationJsonEncrypted = "application/json+encrypted"
    private val applicationXmlEncrypted = "application/xml+encrypted"

    @Nested
    @DisplayName("PUT /v1/internal/cases/:fakeId Deserialization")
    inner class AddCaseEndpointDeserializationTest {

        @Test
        fun `adding a new json case should unwrap and deserialize the addCaseRequest correctly`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toJson(), MediaType.APPLICATION_JSON_VALUE, publicKey)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }

            verify(mockAddCaseUseCase).invoke(
                internalCaseId = fakeId,
                mobileNumber = mobileNumber,
                emailAddress = email,
                publicKey = B64String(publicKey),
                lang = locale,
            )
        }

        @Test
        fun `adding a new xml case should unwrap and deserialize the addCaseRequest correctly`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toXml(), MediaType.APPLICATION_XML_VALUE, publicKey)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }

            verify(mockAddCaseUseCase).invoke(
                internalCaseId = fakeId,
                mobileNumber = mobileNumber,
                emailAddress = email,
                publicKey = B64String(publicKey),
                lang = locale,
            )
        }

        @Test
        fun `adding a new encrypted json case should unwrap and deserialize the addCaseRequest correctly`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toJson().encrypt()!!, applicationJsonEncrypted, publicKey)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }

            verify(mockAddCaseUseCase).invoke(
                internalCaseId = fakeId,
                mobileNumber = mobileNumber,
                emailAddress = email,
                publicKey = B64String(publicKey),
                lang = locale,
            )
        }

        @Test
        fun `adding a new encrypted xml case should unwrap and deserialize the addCaseRequest correctly`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toXml().encrypt()!!, applicationXmlEncrypted, publicKey)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }

            verify(mockAddCaseUseCase).invoke(
                internalCaseId = fakeId,
                mobileNumber = mobileNumber,
                emailAddress = email,
                publicKey = B64String(publicKey),
                lang = locale,
            )
        }

        private fun ByteArray.encrypt(): ByteArray? {
            val pubKey = secrets
                .lazyGet(OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET, "public", RsaPublicKey.Companion::buildFromB64String)
                .requiredValue

            return RsaPublicKey(pubKey).encrypt(this)
        }
    }

    @Nested
    @DisplayName("PUT /v1/internal/cases/:fakeId")
    inner class PutAddCaseEndpointTest {

        private val requestWrapper = AddCaseRequestWrapper(
            content = requestContent.toJson(),
            contentType = MediaType.APPLICATION_JSON_VALUE,
            publicKey = publicKey,
        )

        @Test
        fun `adding a new json case returns 201`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toJson(), MediaType.APPLICATION_JSON_VALUE, publicKey)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `adding a case twice returns 200`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_OVERRIDDEN)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }.andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `adding a new case should throw IllegalArgumentException if contentType of wrapped request is not supported`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toJson(), MediaType.TEXT_PLAIN_VALUE, publicKey)

            // mockMvc doesn't do the regular spring exception handling for some reason
            assertThrows<NestedServletException> {
                mockMvc.put("/v1/internal/cases/$fakeId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = requestWrapper.serialize()
                }
            }
        }

        @Test
        fun `adding a new case should throw IllegalArgumentException if content can't be deserialized to an addCaseRequest`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(
                    json { "some" to "wrong" }.toString().toByteArray(),
                    MediaType.APPLICATION_JSON_VALUE,
                    publicKey,
                )

            // mockMvc doesn't do the regular spring exception handling for some reason
            assertThrows<NestedServletException> {
                mockMvc.put("/v1/internal/cases/$fakeId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = requestWrapper.serialize()
                }
            }
        }

        @Test
        fun `adding a new case should throw IllegalArgumentException if content is not valid for the provided contentType`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(
                    "something not even json".toByteArray(),
                    MediaType.APPLICATION_JSON_VALUE,
                    publicKey,
                )

            // mockMvc doesn't do the regular spring exception handling for some reason
            assertThrows<NestedServletException> {
                mockMvc.put("/v1/internal/cases/$fakeId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = requestWrapper.serialize()
                }
            }
        }

        @Test
        fun `adding a new case should not call addCaseUseCase if addCaseRequest can not be deserialized`() {
            reset(mockAddCaseUseCase)
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestWrapper =
                AddCaseRequestWrapper(
                    json { "some" to "wrong" }.toString().toByteArray(),
                    MediaType.APPLICATION_JSON_VALUE,
                    publicKey,
                )

            try {
                // mockMvc doesn't do the regular spring exception handling for some reason
                mockMvc.put("/v1/internal/cases/$fakeId") {
                    contentType = MediaType.APPLICATION_JSON
                    content = requestWrapper.serialize()
                }
            } catch (ex: Throwable) {
            }

            verifyZeroInteractions(mockAddCaseUseCase)
        }

        @Test
        fun `adding a case with a bad phone number returns 400`() {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.INVALID_PHONE)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.internalMessage") {
                    value("Invalid mobile number")
                }
            }
        }

        @Test
        fun `adding a case and failing to send email returns 400`() {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any(), any(), anyOrNull()))
                .thenReturn(AddCaseUseCase.Result.INVALID_EMAIL)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.internalMessage") {
                    value("Failed to send email")
                }
            }
        }

        @Test
        fun `adding a case with bad keys returns 400`() {
            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = json {
                    "not a key" to "hi@example.com"
                    "contentType" to MediaType.APPLICATION_JSON_VALUE
                    "publicKey" to "not a real public key"
                }
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.internalMessage") {
                    value("Malformed request body")
                }
            }
        }

        @Test
        fun `adding a case with bad values returns 400`() {
            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = json {
                    "content" to requestContent.toJson()
                    "contentType" to MediaType.APPLICATION_JSON_VALUE
                    "publicKey" to json {
                        "oops too much" to "json"
                    }
                }
            }.andExpect {
                status { isBadRequest() }
                jsonPath("$.internalMessage") {
                    value("Malformed request body")
                }
            }
        }

        @Test
        fun `adding a case with bad internal id returns 500`() {
            // mockMvc doesn't do the regular spring exception handling for some reason
            assertThrows<NestedServletException> {
                mockMvc.put("/v1/internal/cases/notauuid") {
                    contentType = MediaType.APPLICATION_JSON
                    content = requestWrapper.serialize()
                }
            }
        }

        @Test
        fun `adding a new json case without lang parameter returns 200`() {
            whenever(mockAddCaseUseCase(any(), any(), any(), any(), isNull()))
                .thenReturn(AddCaseUseCase.Result.SUCCESS_CREATED)

            val requestContent = requestContent.copy(lang = null)
            val requestWrapper =
                AddCaseRequestWrapper(requestContent.toJson(), MediaType.APPLICATION_JSON_VALUE, publicKey)

            mockMvc.put("/v1/internal/cases/$fakeId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestWrapper.serialize()
            }.andExpect {
                status { isCreated() }
            }
        }
    }

    @Nested
    @DisplayName("GET /v1/internal/pairing/refreshtokens")
    inner class GetRefreshTokensEndpointTest {
        @Test
        fun `getting refresh tokens when none exist returns an empty array`() {
            whenever(mockGetRefreshTokensUseCase.invoke())
                .thenReturn(listOf())

            mockMvc.get("/v1/internal/pairing/refreshtokens").andExpect {
                status { isOk() }
                jsonPath("$") { isArray() }
                jsonPath("$") { isEmpty() }
            }
        }
    }

    @Nested
    @DisplayName("DELETE /v1/internal/pairing/refreshtokens")
    inner class DeleteRefreshTokensEndpointTest {
        @Test
        fun `deleting a refresh token returns ok`() {
            whenever(mockDeleteRefreshTokenUseCase.invoke(any())) doReturn true

            mockMvc.delete("/v1/internal/pairing/refreshtokens/${UUID.randomUUID()}").andExpect {
                status { isOk() }
            }
        }
    }

    private fun AddCaseRequestContent.toJson() = jsonMapper.writeValueAsBytes(this)
    private fun AddCaseRequestContent.toXml() = xmlMapper.writeValueAsBytes(this)
    private fun AddCaseRequestWrapper.serialize() = jsonMapper.writeValueAsBytes(this)
}
