package com.healthmetrix.connector.outbox.api.pairing.validation

data class CheckPinAttributes(
    var pin: String,
    var token: String,
    val lang: String,
)
