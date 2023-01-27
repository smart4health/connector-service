package com.healthmetrix.connector.outbox.api.pairing.external.oauthsuccess

import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus

class OauthSuccessResponse(url: String) : ApiResponse(
    HttpStatus.MOVED_PERMANENTLY,
    hasBody = false,
    headers = HttpHeaders().apply {
        add(HttpHeaders.LOCATION, url)
    },
)
