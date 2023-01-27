package com.healthmetrix.connector.inbox.api.query

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.array
import com.healthmetrix.connector.commons.json
import com.healthmetrix.connector.inbox.api.util.InboxControllerTestApplication
import com.healthmetrix.connector.inbox.pairing.QueryStatusUseCase
import com.healthmetrix.connector.inbox.query.FindPatientByInternalCaseIdUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.util.UUID

@SpringBootTest(
    classes = [
        InboxControllerTestApplication::class,
        QueryController::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["query.bulk-limit=5"],
)
@AutoConfigureMockMvc
class QueryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    @Suppress("unused")
    private lateinit var mockFindPatientByInternalCaseIdUseCase: FindPatientByInternalCaseIdUseCase

    @MockBean
    @Suppress("unused")
    private lateinit var mockQueryStatusUseCase: QueryStatusUseCase

    private val externalCaseId = "EXTERNAL-CASE-1"

    @Nested
    @DisplayName("GET /:version/his/patients/:patientId/cases/:caseId/status")
    inner class GetQueryStatusEndpointTest {

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `querying a case that does not exist returns 404`(version: String) {
            whenever(mockQueryStatusUseCase.invoke(any<ExternalCaseId>())) doReturn QueryStatusUseCase.Result.NO_CASE_ID

            mockMvc.get("/$version/his/patients/developerone/cases/$externalCaseId/status").andExpect {
                status { isNotFound() }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `querying a case that exists but is not paired returns 200 and false`(version: String) {
            whenever(mockQueryStatusUseCase.invoke(any<ExternalCaseId>())) doReturn QueryStatusUseCase.Result.NOT_PAIRED

            mockMvc.get("/$version/his/patients/developerone/cases/$externalCaseId/status").andExpect {
                status { isOk() }
                jsonPath("$.pairing_completed") { value(false) }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `querying a case that exists and is paired returns 200 and true`(version: String) {
            whenever(mockQueryStatusUseCase.invoke(any<ExternalCaseId>())) doReturn QueryStatusUseCase.Result.PAIRED

            mockMvc.get("/$version/his/patients/developerone/cases/$externalCaseId/status").andExpect {
                status { isOk() }
                jsonPath("$.pairing_completed") { value(true) }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `querying for multiple cases returns a map`(version: String) {
            whenever(mockQueryStatusUseCase.invoke(any<List<ExternalCaseId>>())) doReturn (
                hashMapOf(
                    "a" to QueryStatusUseCase.Result.NO_CASE_ID,
                    "b" to QueryStatusUseCase.Result.NOT_PAIRED,
                    "c" to QueryStatusUseCase.Result.PAIRED,
                ) to hashMapOf(
                    QueryStatusUseCase.Result.NO_CASE_ID to 1,
                    QueryStatusUseCase.Result.NOT_PAIRED to 1,
                    QueryStatusUseCase.Result.PAIRED to 1,
                )
                )

            mockMvc.post("/$version/his/patients/status") {
                contentType = MediaType.APPLICATION_JSON
                content = array {
                    +"a"
                    +"b"
                    +"c"
                }.toString()
            }.andExpect {
                status { isOk() }
                content {
                    json(
                        json {
                            "statuses" to json {
                                "a" to "NOT_FOUND"
                                "b" to "NOT_PAIRED"
                                "c" to "PAIRED"
                            }
                            "statistics" to json {
                                "NOT_FOUND" to 1
                                "NOT_PAIRED" to 1
                                "PAIRED" to 1
                                "TOTAL" to 3
                            }
                        }.toString(),
                    )
                }
            }
        }

        @ParameterizedTest(name = "version {0}")
        @ValueSource(strings = ["v3"])
        fun `querying for too many cases returns 400`(version: String) {
            mockMvc.post("/$version/his/patients/status") {
                contentType = MediaType.APPLICATION_JSON
                content = array {
                    +"1"
                    +"2"
                    +"3"
                    +"4"
                    +"5"
                    +"TOO MANY"
                }.toString()
            }.andExpect {
                status { isBadRequest() }
            }
        }
    }

    @Nested
    @DisplayName("GET /:version/his/patient")
    inner class FindPatientByInternalCaseIdEndpointTest {

        private val internalCaseId: InternalCaseId = UUID.randomUUID()

        @Test
        fun `finding a patient that exists returns 200`() {
            whenever(mockFindPatientByInternalCaseIdUseCase.invoke(any())) doReturn Ok(externalCaseId)

            mockMvc.get("/v3/his/patient?internalCaseId=$internalCaseId").andExpect {
                status { isOk() }
                jsonPath("$.external_case_id") { value(externalCaseId) }
            }
        }

        @Test
        fun `finding a patient that doesn't exist returns 404`() {
            whenever(mockFindPatientByInternalCaseIdUseCase.invoke(any())) doReturn Err(
                FindPatientByInternalCaseIdUseCase.Error.NotFound,
            )

            mockMvc.get("/v3/his/patient?internalCaseId=$internalCaseId").andExpect {
                status { isNotFound() }
            }
        }

        @Test
        fun `finding a patient with an internal case id that is not a UUID returns 404`() {
            whenever(mockFindPatientByInternalCaseIdUseCase.invoke(any())) doReturn Err(
                FindPatientByInternalCaseIdUseCase.Error.Format(Exception()),
            )

            mockMvc.get("/v3/his/patient?internalCaseId=$internalCaseId").andExpect {
                status { isNotFound() }
            }
        }
    }
}
