package com.healthmetrix.connector.inbox.api.upload.v1

import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.inbox.api.DecommissionedApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DocumentReferenceUploadController {
    @Deprecated("This endpoint was originally implemented as a PUT but should be implemented as a POST")
    @PutMapping(
        "/v1/his/patients/{_patientId}/cases/{_externalCaseId}/fhir/stu3/documentreference/{_documentId}",
        consumes = ["application/fhir+json", "application/fhir+xml"],
    )
    fun uploadDocument(
        @PathVariable _patientId: String,
        @PathVariable _externalCaseId: ExternalCaseId,
        @PathVariable _documentId: String,
    ): ResponseEntity<DecommissionedApiResponse> = DecommissionedApiResponse.asEntity()

    // NOTE: This POST endpoint uses the plural version of the above-deprecated PUT version
    @Deprecated("Please use version 3 of the inbox api")
    @PostMapping(
        "/v1/his/patients/{_patientId}/cases/{_externalCaseId}/fhir/stu3/documentreferences",
        consumes = ["application/fhir+json", "application/fhir+xml"],
    )
    fun createDocument(
        @PathVariable _patientId: String,
        @PathVariable _externalCaseId: ExternalCaseId,
    ): ResponseEntity<DecommissionedApiResponse> = DecommissionedApiResponse.asEntity()
}
