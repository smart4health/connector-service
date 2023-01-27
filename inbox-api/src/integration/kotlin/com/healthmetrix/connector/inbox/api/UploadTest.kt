package com.healthmetrix.connector.inbox.api

import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.inbox.api.sync.SyncRefreshTokensComponent
import com.healthmetrix.connector.inbox.api.upload.CacheUploadComponent
import com.healthmetrix.connector.inbox.sync.SyncRefreshTokensUseCase
import com.healthmetrix.connector.inbox.upload.ResourceUploader
import com.healthmetrix.connector.inbox.upload.UploadDocumentsUseCase
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.core.io.Resource
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put
import java.util.UUID

@SpringBootTest(
    classes = [InboxApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [],
)
@AutoConfigureMockMvc
@DirtiesContext
class UploadTest(
    @Value("classpath:test-data/r4/minimal_document_reference.json")
    docRefJsonResource: Resource,
    @Value("classpath:test-data/r4/minimal_document_reference.xml")
    docRefXmlResource: Resource,
    @Value("classpath:test-data/minimal_patient.json")
    patientJsonResource: Resource,
) {

    @Autowired
    private lateinit var mockMvc: MockMvc

    // mock the schedulers to disable them
    @Suppress("unused")
    @MockBean
    private lateinit var _mockCacheUploadComponent: CacheUploadComponent

    @Suppress("unused")
    @MockBean
    private lateinit var _mockSyncRefreshTokensComponent: SyncRefreshTokensComponent

    private val docRefJson = docRefJsonResource.file.readText()
    private val docRefXml = docRefXmlResource.file.readText()
    private val patientJson = patientJsonResource.file.readText()

    @Autowired
    private lateinit var syncRefreshTokensUseCase: SyncRefreshTokensUseCase

    @Autowired
    private lateinit var uploadDocumentsUseCase: UploadDocumentsUseCase

    @MockBean
    private lateinit var mockUploader: ResourceUploader

    @Test
    fun `a basic upload flow using v1 and a document reference results in no uploaded resources`() {
        val patientId = "Patient1"
        val externalCaseId = UUID.randomUUID()

        // 1. Old endpoint is Gone
        mockMvc.post("/v1/his/patients/$patientId/cases/$externalCaseId/fhir/stu3/documentreferences") {
            contentType = MediaType("application", "fhir+json")
            content = docRefJson
        }.andExpect {
            status { isGone() }
        }

        // 2. add case gone
        mockMvc.put("/v1/his/patients/$patientId/cases/$externalCaseId") {
            contentType = MediaType("application", "json")
            content = "{}" // only used by outbox, which is mocked
        }.andExpect {
            status { isGone() }
        }

        // 3. old endpoint still gone
        mockMvc.post("/v1/his/patients/$patientId/cases/$externalCaseId/fhir/stu3/documentreferences") {
            contentType = MediaType("application", "fhir+xml")
            content = docRefXml
        }.andExpect {
            status { isGone() }
        }

        // 4. sync and upload
        syncRefreshTokensUseCase()

        uploadDocumentsUseCase()

        verify(mockUploader, mode = never()).invoke(any(), any(), any(), any())
    }

    @Test
    fun `a basic upload flow using v2 and a domain resource results in no uploaded resources`() {
        val patientId = "Patient2"
        val externalCaseId = UUID.randomUUID()

        // 1. fail to upload due to Gone endpoint
        mockMvc.post("/v2/his/patients/$patientId/cases/$externalCaseId/fhir/stu3/DomainResource") {
            contentType = MediaType("application", "fhir+json")
            content = docRefJson
        }.andExpect {
            status { isGone() }
        }

        // 2. add case gone
        mockMvc.put("/v2/his/patients/$patientId/cases/$externalCaseId") {
            contentType = MediaType("application", "json")
            content = "{}" // only used by outbox, which is mocked
        }.andExpect {
            status { isGone() }
        }

        // 3. uploading is still Gone
        mockMvc.post("/v2/his/patients/$patientId/cases/$externalCaseId/fhir/stu3/DomainResource") {
            contentType = MediaType("application", "fhir+xml")
            content = docRefXml
        }.andExpect {
            status { isGone() }
        }

        // 4. sync and upload
        syncRefreshTokensUseCase()

        uploadDocumentsUseCase()

        verify(mockUploader, mode = never()).invoke(any(), any(), any(), any())
    }

    @Test
    fun `a basic upload flow using v3 and a domain resource results in 1 uploaded resource`() {
        val patientId = "Patient2"
        val externalCaseId = UUID.randomUUID()

        // 1. fail to upload due to no case
        mockMvc.post("/v3/his/patients/$patientId/cases/$externalCaseId/fhir/r4/DomainResource") {
            contentType = MediaType("application", "fhir+json")
            content = docRefJson
        }.andExpect {
            status { isNotFound() }
        }

        // 2. add case still works
        mockMvc.put("/v3/his/patients/$patientId/cases/$externalCaseId") {
            contentType = MediaType("application", "json")
            content = "{}" // only used by outbox, which is mocked
        }.andExpect {
            status { isCreated() }
        }

        // 3. uploading succeeds
        mockMvc.post("/v3/his/patients/$patientId/cases/$externalCaseId/fhir/r4/DomainResource") {
            contentType = MediaType("application", "fhir+xml")
            content = docRefXml
        }.andExpect {
            status { isCreated() }
        }

        // 4. sync and upload
        syncRefreshTokensUseCase()

        whenever(mockUploader.invoke(any(), any(), any(), any())).thenAnswer {
            Ok((it.getArgument(3) as UUID))
        }

        uploadDocumentsUseCase()

        verify(mockUploader, times(2)).invoke(any(), any(), any(), any())
    }

    @Test
    fun `uploading a fhir resource outside of the d4l profile results in 422`() {
        val patientId = "Patient2"
        val externalCaseId = UUID.randomUUID()

        // 1. add case
        mockMvc.put("/v3/his/patients/$patientId/cases/$externalCaseId") {
            contentType = MediaType("application", "json")
            content = "{}" // only used by outbox, which is mocked
        }.andExpect {
            status { isCreated() }
        }

        // 2. fail to upload
        mockMvc.post("/v3/his/patients/$patientId/cases/$externalCaseId/fhir/r4/DomainResource") {
            contentType = MediaType("application", "fhir+json")
            content = patientJson
        }.andExpect {
            status { isUnprocessableEntity() }
        }
    }
}
