package com.healthmetrix.connector.outbox.api.pairing.internal

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.outbox.api.pairing.internal.addcase.AddCaseRequestDeserializer
import com.healthmetrix.connector.outbox.api.pairing.internal.addcase.AddCaseRequestWrapper
import com.healthmetrix.connector.outbox.api.pairing.internal.addcase.AddCaseResponse
import com.healthmetrix.connector.outbox.api.pairing.internal.deleterefreshtoken.DeleteRefreshTokenResponse
import com.healthmetrix.connector.outbox.usecases.AddCaseUseCase
import com.healthmetrix.connector.outbox.usecases.DeleteRefreshTokenUseCase
import com.healthmetrix.connector.outbox.usecases.GetRefreshTokensUseCase
import com.healthmetrix.connector.outbox.usecases.RefreshToken
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class InternalPairingController(
    private val addCaseUseCase: AddCaseUseCase,
    private val getRefreshTokensUseCase: GetRefreshTokensUseCase,
    private val deleteRefreshTokenUseCase: DeleteRefreshTokenUseCase,
    private val addCaseRequestDeserializer: AddCaseRequestDeserializer,
) {

    @PutMapping(
        path = ["/v1/internal/cases/{internalCaseIdParam}"],
        consumes = [
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            "application/json+encrypted",
            "application/xml+encrypted",
        ],
    )
    fun addCase(
        @PathVariable internalCaseIdParam: String,
        @RequestBody requestWrapper: AddCaseRequestWrapper,
    ): ResponseEntity<AddCaseResponse> {
        logger.debug("addCase for {}", kv("internalCaseId", internalCaseIdParam))

        val addCaseRequest = addCaseRequestDeserializer(requestWrapper)

        val result = addCaseUseCase(
            internalCaseId = InternalCaseId.fromString(internalCaseIdParam),
            emailAddress = addCaseRequest.email,
            mobileNumber = addCaseRequest.mobileNumber,
            publicKey = addCaseRequest.publicKey,
            lang = addCaseRequest.lang,
        )

        if (result != AddCaseUseCase.Result.SUCCESS_OVERRIDDEN && result != AddCaseUseCase.Result.SUCCESS_CREATED) {
            logger.info("addCase not successful {}", kv("result", result.toString()))
        }

        return when (result) {
            AddCaseUseCase.Result.SUCCESS_CREATED -> AddCaseResponse.SuccessCreated
            AddCaseUseCase.Result.SUCCESS_OVERRIDDEN -> AddCaseResponse.SuccessOverride
            AddCaseUseCase.Result.INVALID_EMAIL -> AddCaseResponse.Error("Failed to send email")
            AddCaseUseCase.Result.INVALID_PHONE -> AddCaseResponse.Error("Invalid mobile number")
            AddCaseUseCase.Result.INVALID_STATUS -> AddCaseResponse.Error("Invalid case status")
        }.asEntity()
    }

    @GetMapping("/v1/internal/pairing/refreshtokens")
    fun getRefreshTokens(): List<RefreshToken> = getRefreshTokensUseCase()

    @DeleteMapping("/v1/internal/pairing/refreshtokens/{id}")
    fun deleteRefreshTokens(@PathVariable id: InternalCaseId): ResponseEntity<DeleteRefreshTokenResponse> {
        deleteRefreshTokenUseCase(id)
        return DeleteRefreshTokenResponse.asEntity()
    }
}
