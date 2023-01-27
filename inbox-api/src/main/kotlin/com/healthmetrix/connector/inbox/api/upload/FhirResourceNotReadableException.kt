package com.healthmetrix.connector.inbox.api.upload

import ca.uhn.fhir.parser.DataFormatException
import org.springframework.http.HttpInputMessage
import org.springframework.http.converter.HttpMessageNotReadableException

class FhirResourceNotReadableException(
    val dfe: DataFormatException,
    inputMessage: HttpInputMessage,
) : HttpMessageNotReadableException("Invalid FHIR document reference", inputMessage)
