package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.outbox.oauth.OauthClient
import com.healthmetrix.connector.outbox.persistence.CaseEntity
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.persistence.OauthStateRepository
import com.healthmetrix.connector.outbox.persistence.RefreshToken
import com.healthmetrix.connector.outbox.persistence.RefreshTokenRepository
import org.springframework.stereotype.Component

@Component
class OauthSuccessUseCase(
    private val caseRepository: CaseRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val oauthStateRepository: OauthStateRepository,
    private val oauthClient: OauthClient,
) {
    operator fun invoke(state: String, authCode: String): Result {
        val oauthState = oauthStateRepository.findByState(state).firstOrNull() ?: return Result.OauthStateNotFound

        val case = caseRepository.findById(oauthState.internalCaseId)
        if (case?.status != CaseEntity.Status.PIN_SUCCEEDED) {
            return Result.InvalidCaseStatus
        }

        val refreshToken = oauthClient.getRefreshToken(authCode)
            ?: return Result.OauthRefreshFailed

        refreshTokenRepository.save(RefreshToken(oauthState.internalCaseId, refreshToken))

        caseRepository.save(case.copy(status = CaseEntity.Status.OAUTH_SUCCEEDED))

        return Result.Success(case.lang)
    }

    sealed class Result {
        object OauthStateNotFound : Result()

        object InvalidCaseStatus : Result()

        data class Success(val lang: Bcp47LanguageTag) : Result()

        object OauthRefreshFailed : Result()
    }
}
