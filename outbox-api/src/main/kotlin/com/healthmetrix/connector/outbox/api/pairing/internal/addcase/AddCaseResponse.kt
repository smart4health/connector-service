package com.healthmetrix.connector.outbox.api.pairing.internal.addcase

import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpStatus

sealed class AddCaseResponse(status: HttpStatus, hasBody: Boolean = true) : ApiResponse(status, hasBody) {
    object SuccessOverride : AddCaseResponse(HttpStatus.OK, false)

    object SuccessCreated : AddCaseResponse(HttpStatus.CREATED, false)

    data class Error(val internalMessage: String) : AddCaseResponse(HttpStatus.BAD_REQUEST)
}
