package com.healthmetrix.connector.outbox.api.pairing.internal.checkpin

import com.fasterxml.jackson.annotation.JsonIgnore
import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpStatus

// TODO message bodies or all distinct return codes?
sealed class CheckPinResponse(status: HttpStatus) : ApiResponse(status) {
    object InvalidToken : CheckPinResponse(HttpStatus.BAD_REQUEST) {
        @Suppress("MayBeConstant")
        val internalMessage = "Invalid token"
    }

    object InvalidCaseId : CheckPinResponse(HttpStatus.INTERNAL_SERVER_ERROR)
    object PinNotSent : CheckPinResponse(HttpStatus.BAD_REQUEST) {
        @Suppress("MayBeConstant")
        val internalMessage = "Invalid case status"
    }

    object InvalidPin : CheckPinResponse(HttpStatus.FORBIDDEN)
    data class Valid(val location: String, @JsonIgnore val basicAuthToken: String) : CheckPinResponse(HttpStatus.OK)
}
