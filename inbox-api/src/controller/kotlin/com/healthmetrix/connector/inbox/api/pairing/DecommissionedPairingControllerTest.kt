package com.healthmetrix.connector.inbox.api.pairing

import com.healthmetrix.connector.inbox.api.util.InboxControllerTestApplication
import com.healthmetrix.connector.inbox.pairing.AddCaseUseCase
import com.healthmetrix.connector.inbox.pairing.QueryStatusUseCase
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
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [
        InboxControllerTestApplication::class,
        PairingController::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class DecommissionedPairingControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    @Suppress("unused")
    private lateinit var mockAddCaseUseCase: AddCaseUseCase

    @MockBean
    @Suppress("unused")
    private lateinit var mockQueryStatusUseCase: QueryStatusUseCase

    private val externalCaseId = "EXTERNAL-CASE-1"
    private val encryptedJsonContentType = MediaType("application", "json+encrypted")
    private val encryptedXmlContentType = MediaType("application", "xml+encrypted")

    private val requestContent = "content".toByteArray()

    @Nested
    @DisplayName("PUT /:version/his/patients/:patientId/cases/:caseId")
    inner class PutAddCaseEndpointTest {

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new json case returns 201`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new json case should call addCaseUseCase`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new encrypted json case returns 201`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedJsonContentType
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new encrypted json case should call addCaseUseCase`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedJsonContentType
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new encrypted xml case returns 201`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedXmlContentType
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new xml case returns 201`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_XML
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new xml case should call addCaseUseCase`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_XML
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a new encrypted xml case should call addCaseUseCase`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = encryptedXmlContentType
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a case that already exists returns 200`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/EXTERNAL-CASE-2") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a case that is rejected by outbox returns 400`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.isGone()
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a case with an invalid content type returns 415`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.TEXT_PLAIN
                content = requestContent
            }.andExpect {
                status { isUnsupportedMediaType() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v1", "v2"])
        fun `adding a case should return ApiError when the AddCaseUseCase results in an error`(version: String) {
            mockMvc.put("/$version/his/patients/developerone/cases/$externalCaseId") {
                contentType = MediaType.APPLICATION_JSON
                content = requestContent
            }.isGone()
        }
    }

    private fun ResultActionsDsl.isGone() = andExpect { status { isGone() } }
}
