package com.healthmetrix.connector.outbox.email

import com.healthmetrix.connector.commons.logger

class MockEmailService : EmailService {
    override fun sendEmail(email: Email): Boolean {
        logger.info("Sending email $email")
        return true
    }
}
