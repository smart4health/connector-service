package com.healthmetrix.connector.outbox.api.pairing.frontend

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.outbox.api.pairing.IframeRoutes
import com.healthmetrix.connector.outbox.api.pairing.SuccessKind
import com.healthmetrix.connector.outbox.api.pairing.UnrecoverableErrorKind
import com.healthmetrix.connector.outbox.api.pairing.successUrl
import com.healthmetrix.connector.outbox.api.pairing.unrecoverableErrorUrl
import com.healthmetrix.connector.outbox.api.pairing.validation.CheckPinAttributes
import com.healthmetrix.connector.outbox.usecases.CheckPinUseCase
import com.healthmetrix.connector.outbox.usecases.SendPinUseCase
import com.healthmetrix.connector.outbox.usecases.SendSmsUseCase
import net.logstash.logback.argument.StructuredArguments
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * Handles routes that render templates inside the iframe that is embedded (internally within the Hospital's domain)
 * in a hospital's website. The iframe takes the user through the flow to confirm their PIN via SMS and then redirects
 * them to a success page or displays an error message with the possibility for the user to re-enter their PIN.
 */
@Controller
class FrontendPairingController(
    private val sendSmsUseCase: SendSmsUseCase,
    private val checkPinUseCase: CheckPinUseCase,
    private val sendPinUseCase: SendPinUseCase,
    @Value("\${contacts.support-email}")
    private val emailContact: String,
    @Value("\${portal.host}")
    private val portalPageHost: String,
    @Value("\${portal.paths.pairing-success}")
    private val pairingSuccessUrl: String,
    @Value("\${portal.paths.error}")
    private val errorUrl: String,
) {
    @GetMapping("/frontend/send-sms")
    fun sendSms(
        model: Model,
        @RequestParam
        token: B64String,
        @RequestParam
        lang: String,
    ): String {
        model.addAttribute("lang", lang)
        return when (val viewData = sendSmsUseCase(token)) {
            is SendSmsUseCase.Result.Success -> {
                model.addAttribute("token", token.string)
                model.addAttribute("phoneLast4", viewData.phoneLastFour)
                IframeRoutes.SEND_SMS_ROUTE
            }
            SendSmsUseCase.Result.InvalidToken -> {
                val url = unrecoverableErrorUrl(
                    host = portalPageHost,
                    errorPath = errorUrl,
                    lang = Bcp47LanguageTag(lang),
                    emailContact = emailContact,
                    // sendSmsUseCase only decrypts, does not check nonces
                    unrecoverableErrorKind = UnrecoverableErrorKind.MALFORMED_TOKEN,
                )

                model.addAttribute("redirectUrl", url)
                IframeRoutes.UNRECOVERABLE_ERROR_ROUTE
            }
        }
    }

    @GetMapping("/frontend/pairing")
    fun sendPin(
        @RequestParam
        token: String,
        @RequestParam
        resend: Boolean = false,
        @RequestParam
        lang: String,
        model: Model,
    ): String {
        when (val result = sendPinUseCase(B64String(token))) {
            is Ok -> {
                if (resend) model.addAttribute("resend", true)
                model.addAttribute("checkPin", CheckPinAttributes(pin = "", token = token, lang = lang))
                model.addAttribute("token", token)
                return IframeRoutes.PIN_CHECK_ROUTE
            }
            is Err -> {
                val bcp47LanguageTag = Bcp47LanguageTag(lang)

                val unrecoverableErrorKind = when (result.error) {
                    SendPinUseCase.SendPinError.MALFORMED_INVITATION_TOKEN -> UnrecoverableErrorKind.MALFORMED_TOKEN
                    SendPinUseCase.SendPinError.EXPIRED_INVITATION_TOKEN -> UnrecoverableErrorKind.EXPIRED_TOKEN
                    SendPinUseCase.SendPinError.INVALID_STATUS -> UnrecoverableErrorKind.SEND_PIN_INVALID_STATUS
                    SendPinUseCase.SendPinError.INVALID_CASE_ID -> UnrecoverableErrorKind.INVALID_CASE_ID
                    SendPinUseCase.SendPinError.SMS_ERROR -> UnrecoverableErrorKind.SMS
                    SendPinUseCase.SendPinError.ALREADY_PAIRED -> {
                        model.addAttribute(
                            "redirectUrl",
                            successUrl(
                                host = portalPageHost,
                                pagePath = pairingSuccessUrl,
                                language = bcp47LanguageTag,
                                kind = SuccessKind.ALREADY_PAIRED,
                            ),
                        )
                        return IframeRoutes.SUCCESS_ROUTE
                    }
                }
                logger.info("sendPin not successful {}", StructuredArguments.kv("error", result.error.toString()))

                val url = unrecoverableErrorUrl(
                    host = portalPageHost,
                    errorPath = errorUrl,
                    lang = bcp47LanguageTag,
                    emailContact = emailContact,
                    unrecoverableErrorKind = unrecoverableErrorKind,
                )

                model.addAttribute("redirectUrl", url)
                return IframeRoutes.UNRECOVERABLE_ERROR_ROUTE
            }
        }
    }

    @GetMapping("/frontend/pin-check")
    fun pinPage(
        model: Model,
        @RequestParam token: String,
        @RequestParam resend: Boolean = false,
        @RequestParam lang: String,
    ): String {
        model.addAttribute("checkPin", CheckPinAttributes(pin = "", token = token, lang = lang))
        if (resend) {
            model.addAttribute("resend", true)
        }
        return IframeRoutes.PIN_CHECK_ROUTE
    }

    @PostMapping("/frontend/pin-check")
    fun checkPin(
        @ModelAttribute("checkPin")
        checkPinAttributes: CheckPinAttributes,
        model: Model,
    ): String = when (val result = checkPinUseCase(B64String(checkPinAttributes.token), checkPinAttributes.pin)) {
        is Ok -> {
            model.addAttribute("redirectUrl", result.value)
            IframeRoutes.SUCCESS_ROUTE
        }
        is Err -> {
            logger.info("checkPin not successful {}", StructuredArguments.kv("error", result.error))

            when (result.error) {
                CheckPinUseCase.CheckPinError.MALFORMED_INVITATION_TOKEN,
                CheckPinUseCase.CheckPinError.INVALID_CASE_ID,
                CheckPinUseCase.CheckPinError.EXPIRED_INVITATION_TOKEN,
                -> {
                    // TODO actually unrecoverable?
                    model.addAttribute("checkPin", checkPinAttributes)
                    model.addAttribute("tokenError", true)
                    IframeRoutes.PIN_CHECK_ROUTE
                }
                CheckPinUseCase.CheckPinError.PIN_NOT_SENT -> {
                    val url = unrecoverableErrorUrl(
                        host = portalPageHost,
                        errorPath = errorUrl,
                        emailContact = emailContact,
                        lang = Bcp47LanguageTag(checkPinAttributes.lang),
                        unrecoverableErrorKind = UnrecoverableErrorKind.PIN_NOT_SENT,
                    )

                    model.addAttribute("redirectUrl", url)
                    IframeRoutes.UNRECOVERABLE_ERROR_ROUTE
                }
                CheckPinUseCase.CheckPinError.INVALID_PIN -> {
                    model.addAttribute("checkPin", checkPinAttributes)
                    model.addAttribute("pinError", true)
                    IframeRoutes.PIN_CHECK_ROUTE
                }
            }
        }
    }
}
