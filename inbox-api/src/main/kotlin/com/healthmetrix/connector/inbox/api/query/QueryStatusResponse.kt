package com.healthmetrix.connector.inbox.api.query

import com.fasterxml.jackson.annotation.JsonProperty
import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpStatus

sealed class QueryStatusResponse(status: HttpStatus, hasBody: Boolean) : ApiResponse(status, hasBody) {
    data class CaseFound(
        @JsonProperty("pairing_completed")
        val pairingComplete: Boolean,
    ) : QueryStatusResponse(HttpStatus.OK, true)

    object CaseNotFound : QueryStatusResponse(HttpStatus.NOT_FOUND, false)
}
