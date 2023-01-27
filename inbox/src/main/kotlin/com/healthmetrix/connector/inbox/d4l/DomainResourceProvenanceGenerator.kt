package com.healthmetrix.connector.inbox.d4l

import care.data4life.ingestion.r4.FhirIngestionEngine
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.Provenance
import org.springframework.stereotype.Component

interface DomainResourceProvenanceGenerator {
    fun generate(domainResource: DomainResource): Pair<DomainResource, Provenance>
}

@Component
class D4LDomainResourceProvenanceGenerator(
    private val ingestionEngine: FhirIngestionEngine,
) : DomainResourceProvenanceGenerator {
    override fun generate(domainResource: DomainResource): Pair<DomainResource, Provenance> =
        ingestionEngine.generateAndLinkIngestionProvenanceForResource(domainResource)
}
