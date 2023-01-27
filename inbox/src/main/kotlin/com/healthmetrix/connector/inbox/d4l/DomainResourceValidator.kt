package com.healthmetrix.connector.inbox.d4l

import care.data4life.ingestion.r4.FhirIngestionEngine
import care.data4life.ingestion.r4.containsSeriousIssue
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.OperationOutcome
import org.springframework.stereotype.Component

interface DomainResourceValidator {
    /**
     * @return true if valid, else false, plus the OperationOutcome
     */
    fun validate(domainResource: DomainResource): Pair<Boolean, OperationOutcome>
}

@Component
class D4LDomainResourceValidator(
    private val ingestionEngine: FhirIngestionEngine,
) : DomainResourceValidator {

    override fun validate(domainResource: DomainResource): Pair<Boolean, OperationOutcome> =
        ingestionEngine.validate(domainResource).let { outcome ->
            !outcome.let(::containsSeriousIssue) to outcome
        }
}
