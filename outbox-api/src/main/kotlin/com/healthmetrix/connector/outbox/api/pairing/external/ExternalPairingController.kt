package com.healthmetrix.connector.outbox.api.pairing.external

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.logger
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.web.asEntity
import com.healthmetrix.connector.outbox.api.pairing.UnrecoverableErrorKind
import com.healthmetrix.connector.outbox.api.pairing.external.addcase.AddCaseEncryptionKeyResponse
import com.healthmetrix.connector.outbox.api.pairing.external.oauthsuccess.OauthSuccessResponse
import com.healthmetrix.connector.outbox.api.pairing.successUrl
import com.healthmetrix.connector.outbox.api.pairing.unrecoverableErrorUrl
import com.healthmetrix.connector.outbox.usecases.OauthErrorUseCase
import com.healthmetrix.connector.outbox.usecases.OauthSuccessUseCase
import com.nimbusds.jose.jwk.RSAKey
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class ExternalPairingController(
    private val oauthSuccessUseCase: OauthSuccessUseCase,
    @Qualifier("addCaseEncryptionKey")
    private val addCaseRequestEncryptionKey: LazySecret<RSAKey>,
    private val oauthErrorUseCase: OauthErrorUseCase,
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
) {
    @GetMapping(value = ["/v1/api/pairing/success"], params = ["code"])
    fun oauthSuccess(
        @RequestParam state: String,
        @RequestParam code: String,
    ): ResponseEntity<OauthSuccessResponse> {
        logger.debug("oauthSuccess for {}", kv("state", state))
        return when (val result = oauthSuccessUseCase(state, code)) {
            is OauthSuccessUseCase.Result.Success -> successUrl(
                host = portalPageHost,
                language = result.lang,
                pagePath = pairingSuccessUrl,
            )
            else -> unrecoverableErrorUrl(
                host = portalPageHost,
                errorPath = errorUrl,
                emailContact = emailContact,
                lang = Bcp47LanguageTag(defaultLocale),
                unrecoverableErrorKind = when (result) {
                    OauthSuccessUseCase.Result.InvalidCaseStatus -> UnrecoverableErrorKind.OAUTH_INVALID_STATUS
                    OauthSuccessUseCase.Result.OauthRefreshFailed -> UnrecoverableErrorKind.OAUTH_REFRESH_FAILED
                    OauthSuccessUseCase.Result.OauthStateNotFound -> UnrecoverableErrorKind.OAUTH_STATE_NOT_FOUND
                    else -> UnrecoverableErrorKind.OTHER
                },
            )
        }.let(::OauthSuccessResponse).asEntity()
    }

    @GetMapping(value = ["/v1/api/pairing/success"], params = ["error"])
    fun oauthError(
        @RequestParam state: String,
        @RequestParam error: String,
    ): ResponseEntity<OauthSuccessResponse> = unrecoverableErrorUrl(
        host = portalPageHost,
        errorPath = errorUrl,
        emailContact = emailContact,
        lang = when (val result = oauthErrorUseCase(state)) {
            is OauthErrorUseCase.Result.Success -> result.lang
            else -> Bcp47LanguageTag(defaultLocale)
        },
        unrecoverableErrorKind = UnrecoverableErrorKind.OAUTH_ERROR,
    ).also {
        logger.debug("oauthError for {} with error {}", kv("state", state), kv("error", error))
    }.let(::OauthSuccessResponse).asEntity()

    @GetMapping("/v1/api/pairing/publickey")
    fun getAddCaseEncryptionKey(): ResponseEntity<AddCaseEncryptionKeyResponse> =
        addCaseRequestEncryptionKey.value.toApiResponse().asEntity()

    private fun RSAKey?.toApiResponse(): AddCaseEncryptionKeyResponse = this
        ?.let(AddCaseEncryptionKeyResponse::Success)
        ?: AddCaseEncryptionKeyResponse.Error
}
