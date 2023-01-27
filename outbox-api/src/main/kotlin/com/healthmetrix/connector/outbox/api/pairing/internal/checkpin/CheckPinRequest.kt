package com.healthmetrix.connector.outbox.api.pairing.internal.checkpin

data class CheckPinRequest(val token: String, val pin: String)
