package com.healthmetrix.connector.outbox.api.pairing.internal.deleterefreshtoken

import com.healthmetrix.connector.commons.web.ApiResponse
import org.springframework.http.HttpStatus

// could be expanded in the future if our ORM ever reports failed deletions
object DeleteRefreshTokenResponse : ApiResponse(HttpStatus.OK, hasBody = false)
