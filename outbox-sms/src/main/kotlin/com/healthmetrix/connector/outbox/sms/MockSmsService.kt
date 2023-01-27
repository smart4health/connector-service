package com.healthmetrix.connector.outbox.sms

import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.connector.commons.logger

class MockSmsService : SmsService {
    override fun sendSms(sms: Sms): Result<Unit, Exception> {
        logger.info("Sending sms $sms")
        return Ok(Unit)
    }
}
