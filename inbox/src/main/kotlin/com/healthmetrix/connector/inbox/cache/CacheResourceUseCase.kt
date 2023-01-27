package com.healthmetrix.connector.inbox.cache

import ca.uhn.fhir.context.FhirContext
import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.inbox.d4l.DomainResourceProvenanceGenerator
import com.healthmetrix.connector.inbox.d4l.DomainResourceValidator
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResource
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

@Component
class CacheResourceUseCase(
    private val fhirContext: FhirContext,
    private val validator: DomainResourceValidator,
    private val domainResourceRepository: DomainResourceRepository,
    private val caseRepository: CaseRepository,
    private val domainResourceProvenanceGenerator: DomainResourceProvenanceGenerator,
) {
    operator fun invoke(domainResource: org.hl7.fhir.r4.model.DomainResource, externalCaseId: ExternalCaseId): Result {
        val (valid, outcome) = validator.validate(domainResource)
        if (!valid) {
            return fhirContext.newJsonParser()
                .encodeResourceToString(outcome)
                .let(Result::InvalidDomainResource)
        }

        if (caseRepository.findByExternalCaseId(listOf(externalCaseId)).isEmpty()) {
            return Result.NoCaseId
        }

        val (domainResourceLinkedToProvenance, provenance) = domainResourceProvenanceGenerator.generate(domainResource)

        domainResourceRepository.save(
            DomainResource(
                internalResourceId = UUID.randomUUID(),
                externalCaseId = externalCaseId,
                insertedAt = Timestamp.from(Instant.now()),
                json = fhirContext.newJsonParser().encodeResourceToString(domainResourceLinkedToProvenance),
            ),
        )

        domainResourceRepository.save(
            DomainResource(
                internalResourceId = UUID.randomUUID(),
                externalCaseId = externalCaseId,
                insertedAt = Timestamp.from(Instant.now()),
                json = fhirContext.newJsonParser().encodeResourceToString(provenance),
            ),
        )

        return Result.Success
    }

    sealed class Result {
        object Success : Result()
        object NoCaseId : Result()
        data class InvalidDomainResource(
            val renderedOperationOutcome: String,
        ) : Result()
    }
}
