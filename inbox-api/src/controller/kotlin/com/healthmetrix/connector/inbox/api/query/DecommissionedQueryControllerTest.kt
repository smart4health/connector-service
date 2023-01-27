package com.healthmetrix.connector.inbox.api.query

import com.healthmetrix.connector.inbox.api.util.InboxControllerTestApplication
import com.healthmetrix.connector.inbox.pairing.QueryStatusUseCase
import com.healthmetrix.connector.inbox.query.FindPatientByInternalCaseIdUseCase
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get

@SpringBootTest(
    classes = [
        InboxControllerTestApplication::class,
        QueryController::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class DecommissionedQueryControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    @Suppress("unused")
    private lateinit var mockFindPatientByInternalCaseIdUseCase: FindPatientByInternalCaseIdUseCase

    @MockBean
    @Suppress("unused")
    private lateinit var mockQueryStatusUseCase: QueryStatusUseCase

    private val externalCaseId = "EXTERNAL-CASE-1"

    @ParameterizedTest(name = "version {0}")
    @ValueSource(strings = ["v1", "v2"])
    fun `querying a case that does not exist returns 404`(version: String) {
        mockMvc.get("/$version/his/patients/developerone/cases/$externalCaseId/status").isGone()
    }

    @ParameterizedTest(name = "version {0}")
    @ValueSource(strings = ["v1", "v2"])
    fun `querying a case that exists but is not paired returns 200 and false`(version: String) {
        mockMvc.get("/$version/his/patients/developerone/cases/$externalCaseId/status").isGone()
    }

    @ParameterizedTest(name = "version {0}")
    @ValueSource(strings = ["v1", "v2"])
    fun `querying a case that exists and is paired returns 200 and true`(version: String) {
        mockMvc.get("/$version/his/patients/developerone/cases/$externalCaseId/status").isGone()
    }

    private fun ResultActionsDsl.isGone() = andExpect { status { isGone() } }
}
