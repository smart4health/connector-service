package com.healthmetrix.connector.inbox.upload

import ca.uhn.fhir.context.FhirContext
import ca.uhn.fhir.parser.IParser
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.inbox.cache.CacheResourceUseCase
import com.healthmetrix.connector.inbox.d4l.DomainResourceProvenanceGenerator
import com.healthmetrix.connector.inbox.d4l.DomainResourceValidator
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argForWhich
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.hl7.fhir.r4.model.DocumentReference
import org.hl7.fhir.r4.model.DomainResource
import org.hl7.fhir.r4.model.OperationOutcome
import org.hl7.fhir.r4.model.Provenance
import org.junit.jupiter.api.Test
import java.util.UUID

class CacheResourceUseCaseTest {

    private val mockIParser: IParser = mock {
        on { encodeResourceToString(any()) } doReturn "fake json"
    }
    private val mockFhirContext: FhirContext = mock {
        on { newJsonParser() } doReturn mockIParser
    }

    private val mockDomainResourceValidator: DomainResourceValidator = mock()

    private val mockDomainResourceRepository: DomainResourceRepository = mock()
    private val mockCaseRepository: CaseRepository = mock()

    private val mockDomainResourceProvenanceGenerator = object : DomainResourceProvenanceGenerator {
        override fun generate(domainResource: DomainResource) =
            domainResource to Provenance()
    }

    private val mockDocumentReference: DocumentReference = mock()
    private val fakeCaseEntity = CaseEntity(
        UUID.randomUUID(),
        "whatever",
        B64String("private"),
    )

    private val underTest = CacheResourceUseCase(
        mockFhirContext,
        mockDomainResourceValidator,
        mockDomainResourceRepository,
        mockCaseRepository,
        mockDomainResourceProvenanceGenerator,
    )

    @Test
    fun `caching a document with validation errors results in INVALID_DOCUMENT`() {
        val outcome = OperationOutcome()
        whenever(mockDomainResourceValidator.validate(any())) doReturn (false to outcome)

        assertThat(underTest(mockDocumentReference, "externalCaseId"))
            .isInstanceOf(CacheResourceUseCase.Result.InvalidDomainResource::class.java)
    }

    @Test
    fun `caching a valid document before adding the case results in NO_CASE_ID`() {
        whenever(mockDomainResourceValidator.validate(any())) doReturn (true to OperationOutcome())
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf()

        assertThat(underTest(mockDocumentReference, "externalCaseId"))
            .isEqualTo(CacheResourceUseCase.Result.NoCaseId)
    }

    @Test
    fun `caching a valid document after adding the case results in SUCCESS`() {
        whenever(mockDomainResourceValidator.validate(any())) doReturn (true to OperationOutcome())
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf(fakeCaseEntity)

        assertThat(underTest(mockDocumentReference, "externalCaseId"))
            .isEqualTo(CacheResourceUseCase.Result.Success)
    }

    @Test
    fun `caching a valid document after adding the case saves an entity as a side effect`() {
        whenever(mockDomainResourceValidator.validate(any())) doReturn (true to OperationOutcome())
        whenever(mockCaseRepository.findByExternalCaseId(any())) doReturn listOf(fakeCaseEntity)

        underTest(mockDocumentReference, "externalCaseId")

        verify(mockDomainResourceRepository, times(2)).save(
            argForWhich {
                externalCaseId == "externalCaseId" && json == "fake json"
            },
        )
    }
}
