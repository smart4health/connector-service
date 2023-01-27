package com.healthmetrix.connector.outbox.api.pairing

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.encodeURL
import com.healthmetrix.connector.commons.logger
import net.logstash.logback.argument.StructuredArguments
import org.springframework.web.util.UriComponentsBuilder

fun unrecoverableErrorUrl(
    host: String,
    errorPath: String,
    lang: Bcp47LanguageTag,
    emailContact: String,
    unrecoverableErrorKind: UnrecoverableErrorKind,
): String {
    val correlationId: CorrelationId = CorrelationId.randomUUID()

    correlationId.logger.warn("Unrecoverable Pairing Error: {}", StructuredArguments.kv("correlationId", correlationId))

    return UriComponentsBuilder.fromHttpUrl(host).apply {
        path(lang.language)
        path(errorPath)
        queryParam("lang", lang.encodeURL())
        queryParam("correlationId", correlationId)
        queryParam("emailContact", emailContact.encodeURL())
        queryParam("kind", unrecoverableErrorKind.toString())
    }.build(true).toUriString()
}

enum class UnrecoverableErrorKind {
    MALFORMED_TOKEN,
    EXPIRED_TOKEN,
    SMS,
    SEND_PIN_INVALID_STATUS,
    INVALID_CASE_ID,
    OAUTH_INVALID_STATUS,
    OAUTH_REFRESH_FAILED,
    OAUTH_STATE_NOT_FOUND,
    PIN_NOT_SENT,
    OAUTH_ERROR,
    OTHER,
    ;

    override fun toString() = when (this) {
        MALFORMED_TOKEN -> "malformed_token"
        EXPIRED_TOKEN -> "expired_token"
        SMS -> "sms"
        SEND_PIN_INVALID_STATUS -> "send_pin_invalid_status"
        INVALID_CASE_ID -> "invalid_case_id"
        OAUTH_INVALID_STATUS -> "oauth_invalid_status"
        OAUTH_REFRESH_FAILED -> "oauth_refresh_failed"
        OAUTH_STATE_NOT_FOUND -> "oauth_state_not_found"
        PIN_NOT_SENT -> "pin_not_sent"
        OAUTH_ERROR -> "oauth_error"
        OTHER -> "other"
    }
}
