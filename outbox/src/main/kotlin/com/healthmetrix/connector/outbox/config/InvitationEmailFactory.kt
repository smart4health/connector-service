package com.healthmetrix.connector.outbox.config

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.encodeURL
import com.healthmetrix.connector.outbox.email.Email
import com.healthmetrix.connector.outbox.email.Template
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Service
import org.springframework.web.util.UriComponentsBuilder

@Service
class InvitationEmailFactory(
    private val config: InvitationEmailConfig,
    @Value("\${portal.host}")
    private val portalHost: String,
    @Value("\${portal.paths.invitation-page}")
    private val invitationPagePath: String,
    @Value("\${invitation-mail.registration-url}")
    private val registrationUrl: String,
) {
    fun make(destAddress: String, encryptedToken: B64String, locale: Bcp47LanguageTag): Email {
        val invitationUrl = UriComponentsBuilder.fromHttpUrl(portalHost).apply {
            path(locale.language)
            path(invitationPagePath)
            queryParam("lang", locale.encodeURL())
            queryParam("token", encryptedToken.string.encodeURL())
        }
            .build(true) // indicates that queryParams are encoded manually because nothing but hate for spring
            .toUriString()

        return Email(
            srcAddress = config.srcAddress,
            srcName = config.srcName,
            destAddress = destAddress,
            destName = null,
            subject = config.subject,
            template = Template(
                id = config.templates.getValue(locale.language),
                vars = mapOf(
                    "redirect_url" to invitationUrl,
                    "registration_url" to registrationUrl,
                ),
            ),
        )
    }
}

@ConfigurationProperties("invitation-mail")
@ConstructorBinding
data class InvitationEmailConfig(
    val srcAddress: String,
    val srcName: String,
    val subject: String,
    val templates: Map<String, Int>,
)
