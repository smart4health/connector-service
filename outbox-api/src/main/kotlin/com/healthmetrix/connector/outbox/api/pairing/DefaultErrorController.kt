package com.healthmetrix.connector.outbox.api.pairing

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.logger
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.servlet.error.ErrorController
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import javax.servlet.http.HttpServletResponse

@RestController
class DefaultErrorController(
    @Value("\${portal.host}")
    private val portalPageHost: String,
    @Value("\${portal.paths.pairing-success}")
    private val pairingSuccessUrl: String,
    @Value("\${portal.paths.error}")
    private val errorUrl: String,
    @Value("\${default-locale}")
    private val defaultLocale: String,
    @Value("\${contacts.support-email}")
    private val emailContact: String,
) : ErrorController {

    @GetMapping("/error")
    fun redirectToErrorPage(servletResponse: HttpServletResponse) = unrecoverableErrorUrl(
        host = portalPageHost,
        errorPath = errorUrl,
        lang = Bcp47LanguageTag(defaultLocale),
        emailContact = emailContact,
        unrecoverableErrorKind = UnrecoverableErrorKind.OTHER,
    ).let(servletResponse::sendRedirect).also {
        logger.info("Error occurred: {}", kv("errorType", "spring-default-error"))
    }
}
