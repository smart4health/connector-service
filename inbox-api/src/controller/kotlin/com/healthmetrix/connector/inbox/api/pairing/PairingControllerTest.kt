package com.healthmetrix.connector.inbox.api.pairing

import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.inbox.api.util.InboxControllerTestApplication
import com.healthmetrix.connector.inbox.outbox.AddCaseResult
import com.healthmetrix.connector.inbox.pairing.AddCaseUseCase
import com.healthmetrix.connector.inbox.pairing.QueryStatusUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.reset
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [
        InboxControllerTestApplication::class,
        PairingController::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class PairingControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var mockAddCaseUseCase: AddCaseUseCase

    @MockBean
    private lateinit var mockQueryStatusUseCase: QueryStatusUseCase

    private val externalCaseId = "EXTERNAL-CASE-1"
    private val encryptedJsonContentType = MediaType("application", "json+encrypted")
    private val encryptedXmlContentType = MediaType("application", "xml+encrypted")

    private val requestContent = "content".toByteArray()

    @Nested
    @DisplayName("PUT /:version/his/patients/:patientId/cases/:caseId")
    inner class PutAddCaseEndpointTest {

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new json case returns 201`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.andExpect {
                status { isCreated() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new json case should call addCaseUseCase`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }

            verify(mockAddCaseUseCase).invoke(externalCaseId, requestContent, MediaType.APPLICATION_JSON_VALUE)
            reset(mockAddCaseUseCase) // spring does not automatically reset between parameterized tests
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new encrypted json case returns 201`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedJsonContentType
                content = requestContent
            }.andExpect {
                status { isCreated() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new encrypted json case should call addCaseUseCase`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedJsonContentType
                content = requestContent
            }

            verify(mockAddCaseUseCase).invoke(externalCaseId, requestContent, encryptedJsonContentType.toString())
            reset(mockAddCaseUseCase) // spring does not automatically reset between parameterized tests
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new encrypted xml case returns 201`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedXmlContentType
                content = requestContent
            }.andExpect {
                status { isCreated() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new xml case returns 201`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_XML
                content = requestContent
            }.andExpect {
                status { isCreated() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new xml case should call addCaseUseCase`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_XML
                content = requestContent
            }

            verify(mockAddCaseUseCase).invoke(externalCaseId, requestContent, MediaType.APPLICATION_XML_VALUE)
            reset(mockAddCaseUseCase) // spring does not automatically reset between parameterized tests
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a new encrypted xml case should call addCaseUseCase`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessCreated)

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedXmlContentType
                content = requestContent
            }

            verify(mockAddCaseUseCase).invoke(externalCaseId, requestContent, encryptedXmlContentType.toString())
            reset(mockAddCaseUseCase) // spring does not automatically reset between parameterized tests
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a case that already exists returns 200`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.SuccessOverridden)

            mockMvc.put("/$version/his/patients/developerone/cases/EXTERNAL-CASE-2") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.andExpect {
                status { isOk() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a case that is rejected by outbox returns 400`(version: String) {
            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.Error("error message"))

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.andExpect {
                status { isBadRequest() }
                content {
                    json(
                        json {
                            "internalMessage" to "error message"
                        }.toString(),
                    )
                }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a case with an invalid content type returns 415`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.TEXT_PLAIN
                content = requestContent
            }.andExpect {
                status { isUnsupportedMediaType() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `adding a case should return ApiError when the AddCaseUseCase results in an error`(version: String) {
            val errorMessage = "error message"
            val expected = json {
                "internalMessage" to errorMessage
            }.toString()

            whenever(mockAddCaseUseCase.invoke(any(), any(), any())).thenReturn(AddCaseResult.Error(errorMessage))

            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.andExpect {
                status { isBadRequest() }
                content { json(expected) }
            }
        }
    }
}
