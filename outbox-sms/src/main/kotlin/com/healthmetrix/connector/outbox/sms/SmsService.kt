package com.healthmetrix.connector.outbox.sms

import com.github.michaelbull.result.Result

interface SmsService {
    fun sendSms(sms: Sms): Result<Unit, Exception>
}

data class Sms(
    val srcName: String,
    val destNumber: String,
    val text: String,
)
