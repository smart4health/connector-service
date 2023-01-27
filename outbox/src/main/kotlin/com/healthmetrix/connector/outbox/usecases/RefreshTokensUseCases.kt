package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.outbox.persistence.RefreshTokenRepository
import org.springframework.stereotype.Component

@Component
class GetRefreshTokensUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    operator fun invoke(): List<RefreshToken> = refreshTokenRepository.findAll().map {
        RefreshToken(it.internalCaseId, it.value)
    }
}

@Component
class DeleteRefreshTokenUseCase(
    private val refreshTokenRepository: RefreshTokenRepository,
) {
    operator fun invoke(internalCaseId: InternalCaseId): Boolean {
        refreshTokenRepository.deleteById(internalCaseId)
        return true
    }
}

data class RefreshToken(
    val internalCaseId: InternalCaseId,
    val refreshToken: String,
)
