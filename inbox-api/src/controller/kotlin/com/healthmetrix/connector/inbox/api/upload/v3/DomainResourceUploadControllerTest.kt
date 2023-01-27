package com.healthmetrix.connector.inbox.api.upload.v3

import com.healthmetrix.connector.inbox.api.util.FakeSecretsConfig
import com.healthmetrix.connector.inbox.api.util.InboxControllerTestApplication
import com.healthmetrix.connector.inbox.cache.CacheResourceUseCase
import com.healthmetrix.connector.inbox.upload.FhirConfig
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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
        JsonDomainResourceHttpMessageConverter::class,
        XmlDomainResourceHttpMessageConverter::class,
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

    @MockBean
    private lateinit var mockCacheResourceUseCase: CacheResourceUseCase

    @Test
    fun `context loads`() {
        assertThat(controller).isNotNull
    }

    @Nested
    @DisplayName("DomainResourceUploadController json tests")
    inner class JsonTests {

        @Test
        fun `uploading a valid document reference to a valid case id results in 201`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn CacheResourceUseCase.Result.Success

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_JSON
                content = exampleDocumentReferenceJson
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `uploading a valid non document reference domain resource to a valid case id results in 201`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn CacheResourceUseCase.Result.Success

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_JSON
                content = examplePatientJson
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `uploading a domain resource that does not match any profiles to a valid case id results in 422 unprocessable entity`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn
                CacheResourceUseCase.Result.InvalidDomainResource("{}")

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_JSON
                content = examplePatientJson
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.outcome") { isMap() }
            }
        }

        @Test
        fun `uploading a valid domain resource to an invalid case id results in 404`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn
                CacheResourceUseCase.Result.NoCaseId

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_JSON
                content = examplePatientJson
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.message") {
                    value("Case id not found")
                }
            }
        }

        @Test
        fun `uploading non fhir results in 400`() {
            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_JSON
                content = "{}"
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `uploading a non domain resource results in 404`() {
            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_JSON
                content = """
                    {
                        "resourceType": "Bundle",
                        "type": "transaction"
                    }
                """.trimIndent()
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.message") {
                    value("Resource type is not supported")
                }
            }
        }
    }

    @Nested
    @DisplayName("DomainResourceUploadController xml tests")
    inner class XmlTest {

        @Test
        fun `uploading a valid document reference to a valid case id results in 201`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn CacheResourceUseCase.Result.Success

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_XML
                content = exampleDocumentReferenceXml
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `uploading a valid non document reference domain resource to a valid case id results in 201`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn CacheResourceUseCase.Result.Success

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_XML
                content = examplePatientXml
            }.andExpect {
                status { isCreated() }
            }
        }

        @Test
        fun `uploading a domain resource that does not match any profiles to a valid case id results in 422 unprocessable entity`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn
                CacheResourceUseCase.Result.InvalidDomainResource("{}")

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_XML
                content = examplePatientXml
            }.andExpect {
                status { isUnprocessableEntity() }
                jsonPath("$.outcome") { isMap() }
            }
        }

        @Test
        fun `uploading a valid domain resource to an invalid case id results in 404`() {
            whenever(mockCacheResourceUseCase.invoke(any(), any())) doReturn
                CacheResourceUseCase.Result.NoCaseId

            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_XML
                content = examplePatientXml
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.message") {
                    value("Case id not found")
                }
            }
        }

        @Test
        fun `uploading non fhir results in 400`() {
            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_XML
                content = "<Xml/>"
            }.andExpect {
                status { isBadRequest() }
            }
        }

        @Test
        fun `uploading a non domain resource results in 404`() {
            mockMvc.post("/v3/his/patients/123/cases/caseId1/fhir/r4/DomainResource") {
                contentType = FHIR_XML
                content = """
                    <Bundle xmlns="http://hl7.org/fhir">
                        <type value="transaction"></type>
                    </Bundle>
                """.trimIndent()
            }.andExpect {
                status { isNotFound() }
                jsonPath("$.message") {
                    value("Resource type is not supported")
                }
            }
        }
    }
}
