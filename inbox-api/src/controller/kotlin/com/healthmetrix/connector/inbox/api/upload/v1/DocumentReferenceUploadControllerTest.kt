package com.healthmetrix.connector.inbox.api.upload.v1

import com.healthmetrix.connector.inbox.api.util.FakeSecretsConfig
import com.healthmetrix.connector.inbox.api.util.InboxControllerTestApplication
import com.healthmetrix.connector.inbox.upload.FhirConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest(
    classes = [
        InboxControllerTestApplication::class,
        DocumentReferenceUploadController::class,
        FakeSecretsConfig::class,
        FhirConfig::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class DocumentReferenceUploadControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Value("classpath:test-data/minimal_document_reference.json")
    private lateinit var exampleFhirJson: Resource

    @Value("classpath:test-data/minimal_document_reference.xml")
    private lateinit var exampleFhirXml: Resource

    @Autowired
    private lateinit var controller: DocumentReferenceUploadController

    @Test
    fun `context loads`() {
        assertThat(controller).isNotNull
    }

    @Nested
    @DisplayName("PUT /v1/his/patients/:patientId/cases/:caseId/fhir/stu3/documentreference/:documentId")
    inner class UploadDocumentEndpointTest {
        @Test
        fun `uploading invalid json doc returns 410`() {
            mockMvc.put("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreference/docid1") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText().run { take(length / 2) }
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading invalid xml doc returns 410`() {
            mockMvc.put("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreference/docid1") {
                contentType = MediaType("application", "fhir+xml")
                content = exampleFhirXml.file.readText().run { take(length / 2) }
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a fhir json doc that does not match known profiles returns 410`() {
            mockMvc.put("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreference/docid1") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText()
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }

        @Test
        fun `uploading a valid fhir json doc without adding a case returns 410`() {
            mockMvc.put("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreference/docid1") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText()
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a valid fhir json doc for a valid case returns 410`() {
            mockMvc.put("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreference/docid1") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText()
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a valid fhir xml doc for a valid case returns 410`() {
            mockMvc.put("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreference/docid1") {
                contentType = MediaType("application", "fhir+xml")
                content = exampleFhirXml.file.readText()
            }.andExpect {
                status { isGone() }
            }
        }
    }

    @Nested
    @DisplayName("POST /v1/his/patients/:patientId/cases/:caseId/fhir/stu3/documentreferences")
    inner class NewDocumentEndpointTest {
        @Test
        fun `uploading invalid json doc returns 410`() {
            mockMvc.post("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreferences") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText().run { take(length / 2) }
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading invalid xml doc returns 410`() {
            mockMvc.post("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreferences") {
                contentType = MediaType("application", "fhir+xml")
                content = exampleFhirXml.file.readText().run { take(length / 2) }
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a fhir json doc that does not match known profiles returns 410`() {
            mockMvc.post("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreferences") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText()
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }

        @Test
        fun `uploading a valid fhir json doc without adding a case returns 410`() {
            mockMvc.post("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreferences") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText()
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a valid fhir json doc for a valid case returns 410`() {
            mockMvc.post("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreferences") {
                contentType = MediaType("application", "fhir+json")
                content = exampleFhirJson.file.readText()
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a valid fhir xml doc for a valid case returns 410`() {
            mockMvc.post("/v1/his/patients/examplepatient/cases/caseid1/fhir/stu3/documentreferences") {
                contentType = MediaType("application", "fhir+xml")
                content = exampleFhirXml.file.readText()
            }.andExpect {
                status { isGone() }
            }
        }
    }
}
