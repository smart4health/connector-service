package com.healthmetrix.connector.inbox.api.upload.v2

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

private val FHIR_XML = MediaType("application", "fhir+xml")
private val FHIR_JSON = MediaType("application", "fhir+json")

@SpringBootTest(
    classes = [
        InboxControllerTestApplication::class,
        DomainResourceUploadController::class,
        FakeSecretsConfig::class,
        FhirConfig::class,
    ],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@AutoConfigureMockMvc
class DomainResourceUploadControllerTest(
    @Value("classpath:test-data/minimal_document_reference.json")
    docRefJsonResource: Resource,
    @Value("classpath:test-data/minimal_document_reference.xml")
    docRefXmlResource: Resource,
    @Value("classpath:test-data/minimal_patient.json")
    patientJsonResource: Resource,
    @Value("classpath:test-data/minimal_patient.xml")
    patientXmlResource: Resource,
) {
    @Autowired
    private lateinit var mockMvc: MockMvc

    private val exampleDocumentReferenceJson = docRefJsonResource.file.readText()
    private val exampleDocumentReferenceXml = docRefXmlResource.file.readText()
    private val examplePatientJson = patientJsonResource.file.readText()
    private val examplePatientXml = patientXmlResource.file.readText()

    @Autowired
    private lateinit var controller: DomainResourceUploadController

    @Test
    fun `context loads`() {
        assertThat(controller).isNotNull
    }

    @Nested
    @DisplayName("DomainResourceUploadController json tests")
    inner class JsonTests {

        @Test
        fun `uploading a valid document reference to a valid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_JSON
                content = exampleDocumentReferenceJson
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a valid non document reference domain resource to a valid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_JSON
                content = examplePatientJson
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a domain resource that does not match any profiles to a valid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_JSON
                content = examplePatientJson
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }

        @Test
        fun `uploading a valid domain resource to an invalid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_JSON
                content = examplePatientJson
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }

        @Test
        fun `uploading non fhir results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_JSON
                content = "{}"
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a non domain resource results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_JSON
                content = """
                    {
                        "resourceType": "Bundle",
                        "type": "transaction"
                    }
                """.trimIndent()
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }
    }

    @Nested
    @DisplayName("DomainResourceUploadController xml tests")
    inner class XmlTest {

        @Test
        fun `uploading a valid document reference to a valid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_XML
                content = exampleDocumentReferenceXml
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a valid non document reference domain resource to a valid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_XML
                content = examplePatientXml
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a domain resource that does not match any profiles to a valid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_XML
                content = examplePatientXml
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }

        @Test
        fun `uploading a valid domain resource to an invalid case id results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_XML
                content = examplePatientXml
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }

        @Test
        fun `uploading non fhir results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_XML
                content = "<Xml/>"
            }.andExpect {
                status { isGone() }
            }
        }

        @Test
        fun `uploading a non domain resource results in 410`() {
            mockMvc.post("/v2/his/patients/123/cases/caseId1/fhir/stu3/DomainResource") {
                contentType = FHIR_XML
                content = """
                    <Bundle xmlns="http://hl7.org/fhir">
                        <type value="transaction"></type>
                    </Bundle>
                """.trimIndent()
            }.andExpect {
                status { isGone() }
                jsonPath("$.message") { isString() }
            }
        }
    }
}
