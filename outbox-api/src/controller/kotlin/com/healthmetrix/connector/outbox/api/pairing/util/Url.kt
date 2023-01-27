package com.healthmetrix.connector.outbox.api.pairing.util

import java.net.URI

fun withQueryParams(redirectUrl: String, block: (Map<String, String>) -> Unit) {
    block(URI(redirectUrl).queryParams())
}

private fun URI.queryParams(): Map<String, String> = this.query.split("&").associate {
    val (key, value) = it.split("=")
    key to value
}
