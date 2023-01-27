package com.healthmetrix.connector.inbox.api

import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpStatus

object DecommissionedApiResponse : ApiResponse(HttpStatus.GONE) {
    val message = "/v1 and /v2 endpoints have been removed, please use /v3 with FHIR R4"
}
