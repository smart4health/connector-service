package com.healthmetrix.connector.outbox.api.pairing

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import org.springframework.web.util.UriComponentsBuilder

fun successUrl(host: String, language: Bcp47LanguageTag, pagePath: String, kind: SuccessKind? = null): String {
    val url = UriComponentsBuilder.fromHttpUrl(host)
        .path(language.language)
        .path(pagePath)
        .queryParam("lang", language.encodeURL())

    kind?.let { url.queryParam("kind", kind.toString()) }

    return url.build(true)
        .toUriString()
}

enum class SuccessKind {
    ALREADY_PAIRED,
    ;

    override fun toString() = when (this) {
        ALREADY_PAIRED -> "already_paired"
    }
}
