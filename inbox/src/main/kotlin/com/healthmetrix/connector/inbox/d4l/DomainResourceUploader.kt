package com.healthmetrix.connector.inbox.d4l

import care.data4life.ingestion.r4.FhirIngestionEngine
import care.data4life.ingestion.r4.PemKeyString
import com.healthmetrix.connector.commons.logger
import org.hl7.fhir.r4.model.DomainResource
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

interface DomainResourceUploader {
    fun uploadDocument(domainResource: DomainResource, accessToken: String, privateKey: PemKeyString)
}

@Component
@Profile("upload")
class D4LDomainResourceUploader(private val ingestionEngine: FhirIngestionEngine) : DomainResourceUploader {
    override fun uploadDocument(domainResource: DomainResource, accessToken: String, privateKey: PemKeyString) =
        ingestionEngine.uploadResource(
            domainResource,
            accessToken.toByteArray(charset = Charsets.UTF_8),
            privateKey.toByteArray(charset = Charsets.UTF_8),
        )
}

@Component
@Profile("!upload")
class MockDomainResourceUploader : DomainResourceUploader {
    override fun uploadDocument(domainResource: DomainResource, accessToken: String, privateKey: PemKeyString) =
        logger.info("Pretending to upload document")
}
