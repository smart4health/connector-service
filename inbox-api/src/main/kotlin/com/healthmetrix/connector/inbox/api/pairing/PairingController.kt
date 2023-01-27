package com.healthmetrix.connector.inbox.api.pairing

import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.inbox.api.DecommissionedApiResponse
import com.healthmetrix.connector.inbox.api.pairing.addcase.AddCaseResponse
import com.healthmetrix.connector.inbox.outbox.AddCaseResult.Error
import com.healthmetrix.connector.inbox.outbox.AddCaseResult.SuccessCreated
import com.healthmetrix.connector.inbox.outbox.AddCaseResult.SuccessOverridden
import com.healthmetrix.connector.inbox.pairing.AddCaseUseCase
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletRequest

@RestController
class PairingController(
    private val addCaseUseCase: AddCaseUseCase,
) {

    @PutMapping(
        path = [
            "/v1/his/patients/{_patientId}/cases/{_externalCaseId}",
            "/v2/his/patients/{_patientId}/cases/{_externalCaseId}",
        ],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, "application/json+encrypted", "application/xml+encrypted"],
    )
    fun oldAddCase(
        @PathVariable _patientId: String,
        @PathVariable _externalCaseId: ExternalCaseId,
        @RequestBody _addCaseRequestContent: ByteArray,
    ): ResponseEntity<DecommissionedApiResponse> = DecommissionedApiResponse.asEntity()

    @PutMapping(
        path = ["/v3/his/patients/{patientId}/cases/{externalCaseId}"],
        consumes = [MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE, "application/json+encrypted", "application/xml+encrypted"],
    )
    fun addCase(
        @PathVariable patientId: String, // ignore patient id for now
        @PathVariable externalCaseId: ExternalCaseId,
        @RequestBody addCaseRequestContent: ByteArray,
        httpServletRequest: HttpServletRequest,
    ): ResponseEntity<AddCaseResponse> {
        logger.debug("addCase for {} {}", kv("patientId", patientId), kv("externalCaseId", externalCaseId))

        val contentType = httpServletRequest.contentType

        val response = addCaseUseCase(
            externalCaseId,
            addCaseRequestContent,
            contentType,
        )

        if (response is Error) {
            logger.info("addCase not successful {}", kv("error", response.toString()))
        }

        return when (response) {
            SuccessCreated -> AddCaseResponse.SuccessCreated
            SuccessOverridden -> AddCaseResponse.SuccessOverridden
            is Error -> AddCaseResponse.Error(response.internalMessage)
        }.asEntity()
    }
}
