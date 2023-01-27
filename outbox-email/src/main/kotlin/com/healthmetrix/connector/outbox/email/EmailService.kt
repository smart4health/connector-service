package com.healthmetrix.connector.outbox.email

import com.healthmetrix.connector.commons.json

interface EmailService {
    fun sendEmail(email: Email): Boolean
}

data class Email(
    val srcAddress: String,
    val srcName: String,
    val destAddress: String,
    val destName: String?,
    val subject: String,
    val template: Template,
)

data class Template(val id: Int, private val vars: Map<String, Any>? = null) {
    fun asJson() = json {
        vars?.forEach { (k, v) ->
            k to v
        }
    }
}
