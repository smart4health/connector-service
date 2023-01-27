package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.outbox.persistence.CaseRepository
import com.healthmetrix.connector.outbox.persistence.OauthStateRepository
import org.springframework.stereotype.Component

@Component
class OauthErrorUseCase(
    private val caseRepository: CaseRepository,
    private val oauthStateRepository: OauthStateRepository,
) {
    operator fun invoke(state: String): Result {
        val oauthState = oauthStateRepository.findByState(state).firstOrNull() ?: return Result.OauthStateNotFound
        val case = caseRepository.findById(oauthState.internalCaseId) ?: return Result.CaseNotFound
        return Result.Success(case.lang)
    }

    sealed class Result {
        object CaseNotFound : Result()
        object OauthStateNotFound : Result()
        data class Success(val lang: Bcp47LanguageTag) : Result()
    }
}
