package com.healthmetrix.connector.outbox.config

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.outbox.email.Email
import com.healthmetrix.connector.outbox.email.Template
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class InvitationEmailFactoryTest {
    private val portalHost = "http://portal.fake"
    private val invitationPagePath = "/invite"
    private val registrationUrl = "http://fake4life/register"

    private val srcAddress = "sender@fake.de"
    private val srcName = "fake sender"
    private val subject = "fake"
    private val germanLang = "de-DE"
    private val templateId = 123456
    private val templates = mapOf("de" to templateId)
    private val config = InvitationEmailConfig(
        srcAddress,
        srcName,
        subject,
        templates,
    )

    private val encryptedToken = B64String("iAmAToken")
    private val germanLocale = Bcp47LanguageTag(germanLang)
    private val destAddress = "person@fake.de"

    private val invitationEmailFactory =
        InvitationEmailFactory(
            config,
            portalHost,
            invitationPagePath,
            registrationUrl,
        )

    @Test
    fun `make should create correct email`() {
        val expectedMail = Email(
            srcAddress = srcAddress,
            srcName = srcName,
            subject = subject,
            destAddress = destAddress,
            destName = null,
            template = Template(
                id = templateId,
                vars = mapOf(
                    "redirect_url" to "http://portal.fake/de/invite?lang=de-DE&token=iAmAToken",
                    "registration_url" to registrationUrl,
                ),
            ),
        )

        val res = invitationEmailFactory.make(destAddress, encryptedToken, germanLocale)

        assertThat(res).isEqualTo(expectedMail)
    }
}
