package com.healthmetrix.connector.commons

import java.util.UUID

typealias ExternalCaseId = String

typealias InternalCaseId = UUID

data class B64String(val string: String) {
    fun decode(): ByteArray? = string.decodeBase64()
}
