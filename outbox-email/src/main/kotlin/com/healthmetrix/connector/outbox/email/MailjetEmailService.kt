package com.healthmetrix.connector.outbox.email

import com.healthmetrix.connector.commons.array
import com.healthmetrix.connector.commons.logger
import com.mailjet.client.MailjetClient
import com.mailjet.client.MailjetRequest
import com.mailjet.client.resource.Emailv31
import com.mailjet.client.resource.Emailv31.Message

class MailjetEmailService(
    private val mailjetClient: MailjetClient,
) : EmailService {

    override fun sendEmail(email: Email): Boolean {
        val request = MailjetRequest(Emailv31.resource).property(
            Emailv31.MESSAGES,
            array {
                json {
                    Message.FROM to json {
                        Message.EMAIL to email.srcAddress
                        Message.NAME to email.srcName
                    }

                    Message.TO to array {
                        json {
                            Message.EMAIL to email.destAddress
                            if (email.destName == null) {
                                Message.NAME to email.destName
                            }
                        }
                    }

                    Message.SUBJECT to email.subject
                    Message.TEMPLATEID to email.template.id
                    Message.TEMPLATELANGUAGE to true
                    Message.VARIABLES to email.template.asJson()
                }
            },
        )

        logger.debug("Sending email: $request")

        return try {
            val response = mailjetClient.post(request)
            if (response.status != 200) {
                logger.warn("Received non-200 mailjet email response $response")
            }
            response.status == 200
        } catch (e: Exception) {
            logger.warn("Received mailjet email exception", e)
            false
        }
    }
}
