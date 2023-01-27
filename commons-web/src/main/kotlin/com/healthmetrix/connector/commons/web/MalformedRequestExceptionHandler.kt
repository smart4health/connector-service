package com.healthmetrix.connector.commons.web

import com.healthmetrix.connector.commons.logger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageConversionException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler

@ControllerAdvice
class GlobalExceptionHandler {
    /**
     * Possible the only global exception, the malformed request body
     *
     * TODO used to only handle HttpMessageNotReadableException, but
     *   after some jackson updates it throws a conversion exception instead.
     *   Could be fixed in the future
     *   see https://github.com/spring-projects/spring-framework/issues/24610
     */
    @ExceptionHandler(HttpMessageConversionException::class)
    fun onException(exception: HttpMessageConversionException): ResponseEntity<MalformedRequestError> {
        logger.warn("Received invalid request body", exception)
        return MalformedRequestError.asEntity()
    }
}

object MalformedRequestError : ApiResponse(HttpStatus.BAD_REQUEST) {
    val internalMessage = "Malformed request body"
}
