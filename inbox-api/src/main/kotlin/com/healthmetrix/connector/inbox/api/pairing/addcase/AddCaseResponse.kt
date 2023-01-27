package com.healthmetrix.connector.inbox.api.pairing.addcase

import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpStatus

sealed class AddCaseResponse(status: HttpStatus, hasBody: Boolean = true) : ApiResponse(status, hasBody) {
    object SuccessOverridden : AddCaseResponse(HttpStatus.OK, false)

    object SuccessCreated : AddCaseResponse(HttpStatus.CREATED, false)

    data class Error(val internalMessage: String) : AddCaseResponse(HttpStatus.BAD_REQUEST)
}
