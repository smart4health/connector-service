package com.healthmetrix.connector.inbox.pairing

import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshToken
import com.healthmetrix.connector.inbox.persistence.refreshtokens.RefreshTokenRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class QueryStatusUseCase(
    private val caseRepository: CaseRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    @Value("\${query.sql-page-size}")
    private val sqlPageSize: Int,
) {
    operator fun invoke(externalCaseId: ExternalCaseId): Result {
        val internalCaseId = caseRepository.findByExternalCaseId(listOf(externalCaseId)).firstOrNull()?.internalCaseId
            ?: return Result.NO_CASE_ID

        return if (refreshTokenRepository.findByIdOrNull(internalCaseId) == null) {
            Result.NOT_PAIRED
        } else {
            Result.PAIRED
        }
    }

    operator fun invoke(externalCaseIds: List<ExternalCaseId>): Pair<Map<ExternalCaseId, Result>, Map<Result, Int>> {
        val statuses = externalCaseIds.chunked(sqlPageSize).map { chunk ->
            val found = caseRepository.findByExternalCaseId(chunk)
            val noCaseId = chunk - found.map(CaseEntity::externalCaseId)

            val refreshTokens = found
                .map(CaseEntity::internalCaseId)
                .let(refreshTokenRepository::findByIds)
                .map(RefreshToken::internalCaseId)

            val (paired, notPaired) = found.partition {
                it.internalCaseId in refreshTokens
            }

            noCaseId.associateWith { Result.NO_CASE_ID } +
                notPaired.map(CaseEntity::externalCaseId).associateWith { Result.NOT_PAIRED } +
                paired.map(CaseEntity::externalCaseId).associateWith { Result.PAIRED }
        }.fold(mapOf<ExternalCaseId, Result>()) { acc, el -> acc + el }

        val sums = mapOf(
            Result.NO_CASE_ID to statuses.filterValues(Result.NO_CASE_ID::equals).count(),
            Result.NOT_PAIRED to statuses.filterValues(Result.NOT_PAIRED::equals).count(),
            Result.PAIRED to statuses.filterValues(Result.PAIRED::equals).count(),
        )

        return statuses to sums
    }

    enum class Result {
        NO_CASE_ID,
        NOT_PAIRED,
        PAIRED,
    }
}
