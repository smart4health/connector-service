package com.healthmetrix.connector.inbox.api.upload

import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException
import java.lang.ClassCastException

class FhirResourceTypeMismatchException(
    val classCastException: ClassCastException,
    inputMessage: HttpInputMessage,
) : HttpMessageNotReadableException("FHIR resource does not match expected type", inputMessage)
