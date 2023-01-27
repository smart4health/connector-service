package com.healthmetrix.connector.inbox.query

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.flatMap
import com.github.michaelbull.result.map
import com.github.michaelbull.result.mapError
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import org.springframework.stereotype.Component
import java.util.UUID
import com.github.michaelbull.result.runCatching as catch

/**
 * Recommend renaming to be more generic as more use cases pop up
 */
@Component
class FindPatientByInternalCaseIdUseCase(
    private val caseRepository: CaseRepository,
) {
    operator fun invoke(internalCaseId: String): Result<ExternalCaseId, Error> =
        catch { UUID.fromString(internalCaseId) }
            .mapError(Error::Format)
            .map(caseRepository::findByInternalCaseId)
            .map { it.orElse(null) }
            .flatMap { it.toResultOr { Error.NotFound } }
            .map(CaseEntity::externalCaseId)

    sealed class Error {
        data class Format(val t: Throwable) : Error()

        object NotFound : Error()
    }
}
