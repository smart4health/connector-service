package com.healthmetrix.connector.inbox.api.upload.v2

import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.inbox.api.DecommissionedApiResponse
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController(value = "stu3DomainResourceUploadController")
class DomainResourceUploadController {

    @PostMapping(
        "/v2/his/patients/{_patientId}/cases/{_externalCaseId}/fhir/stu3/DomainResource",
        consumes = ["application/fhir+json", "application/fhir+xml"],
    )
    fun cacheDocument(
        @PathVariable
        _patientId: String,
        @PathVariable
        _externalCaseId: ExternalCaseId,
    ): ResponseEntity<DecommissionedApiResponse> = DecommissionedApiResponse.asEntity()
}
