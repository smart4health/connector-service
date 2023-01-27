package com.healthmetrix.connector.outbox.sms

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.github.michaelbull.result.Result
import com.healthmetrix.connector.commons.logger
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.errors.MailjetClientRequestException
import com.mailjet.client.errors.MailjetException
import com.mailjet.client.resource.sms.SmsSend
import net.logstash.logback.argument.StructuredArguments

class MailjetSmsService(private val mailjetClient: MailjetClient) : SmsService {
    override fun sendSms(sms: Sms): Result<Unit, MailjetException> {
        val request = MailjetRequest(SmsSend.resource).apply {
            property(SmsSend.FROM, sms.srcName)
            property(SmsSend.TO, sms.destNumber)
            property(SmsSend.TEXT, sms.text)
        }

        return try {
            mailjetClient.post(request)

            Ok(Unit)
        } catch (mailjetException: MailjetException) {
            when (mailjetException) {
                is MailjetClientRequestException -> logger.error(
                    "Send sms bad request {} {}",
                    StructuredArguments.kv("responseBody", mailjetException.message),
                    StructuredArguments.kv("statusCode", mailjetException.statusCode),
                )
                else -> logger.error(
                    "Failed to send sms {}",
                    StructuredArguments.kv("class", mailjetException::class.java),
                )
            }

            Err(mailjetException)
        }
    }
}
