package com.healthmetrix.connector.inbox.api.upload.v3

import com.fasterxml.jackson.annotation.JsonRawValue
import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.web.ApiResponse
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.inbox.api.upload.FhirResourceNotReadableException
import com.healthmetrix.connector.inbox.api.upload.FhirResourceTypeMismatchException
import com.healthmetrix.connector.inbox.cache.CacheResourceUseCase
import org.hl7.fhir.r4.model.DomainResource
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController(value = "r4DomainResourceUploadController")
class DomainResourceUploadController(
    private val cacheResourceUseCase: CacheResourceUseCase,
) {

    @PostMapping(
        "/v3/his/patients/{patientId}/cases/{externalCaseId}/fhir/r4/DomainResource",
        consumes = ["application/fhir+json", "application/fhir+xml"],
    )
    fun cacheDocument(
        @PathVariable
        patientId: String,
        @PathVariable
        externalCaseId: ExternalCaseId,
        @RequestBody
        domainResource: DomainResource,
    ): ResponseEntity<CacheDocumentResponse> = when (val r = cacheResourceUseCase(domainResource, externalCaseId)) {
        CacheResourceUseCase.Result.Success -> CacheDocumentResponse.Created
        CacheResourceUseCase.Result.NoCaseId -> CacheDocumentResponse.NoCaseId
        is CacheResourceUseCase.Result.InvalidDomainResource -> CacheDocumentResponse.ValidationFailed(r.renderedOperationOutcome)
    }.asEntity()

    @ExceptionHandler(FhirResourceNotReadableException::class)
    fun handleInvalidFormat(ex: FhirResourceNotReadableException): ResponseEntity<InvalidFormat> =
        InvalidFormat(ex.dfe.message ?: "DataFormatException").asEntity()

    @ExceptionHandler(FhirResourceTypeMismatchException::class)
    fun handleInvalidResourceType(ex: FhirResourceTypeMismatchException): ResponseEntity<ApiResponse> =
        object : ApiResponse(HttpStatus.NOT_FOUND, true) {
            val message = "Resource type is not supported"
        }.asEntity()

    data class InvalidFormat(
        val message: String,
    ) : ApiResponse(HttpStatus.BAD_REQUEST, true)

    sealed class CacheDocumentResponse(status: HttpStatus, hasBody: Boolean) : ApiResponse(status, hasBody) {
        object Created : CacheDocumentResponse(HttpStatus.CREATED, false)

        data class ValidationFailed(
            @JsonRawValue
            val outcome: String,
        ) : CacheDocumentResponse(HttpStatus.UNPROCESSABLE_ENTITY, true)

        object NoCaseId : CacheDocumentResponse(HttpStatus.NOT_FOUND, true) {
            val message = "Case id not found"
        }
    }
}
