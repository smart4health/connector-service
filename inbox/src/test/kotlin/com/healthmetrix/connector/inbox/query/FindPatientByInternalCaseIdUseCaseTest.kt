package com.healthmetrix.connector.inbox.query

import com.github.michaelbull.result.get
import com.github.michaelbull.result.getError
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.persistence.cases.CaseEntity
import com.healthmetrix.connector.inbox.persistence.cases.CaseRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Optional

class FindPatientByInternalCaseIdUseCaseTest {
    private val mockCaseRepository: CaseRepository = mockk()

    private val underTest = FindPatientByInternalCaseIdUseCase(
        caseRepository = mockCaseRepository,
    )

    @Test
    fun `finding patient with non uuid returns a format error`() {
        every { mockCaseRepository.findByInternalCaseId(any()) } returns Optional.empty()

        val res = underTest("not a uuid")

        assertThat(res.getError()).isInstanceOf(FindPatientByInternalCaseIdUseCase.Error.Format::class.java)
    }

    @Test
    fun `finding a patient that does not exist returns a not found error`() {
        every { mockCaseRepository.findByInternalCaseId(any()) } returns Optional.empty()

        val res = underTest(InternalCaseId.randomUUID().toString())

        assertThat(res.getError()).isEqualTo(FindPatientByInternalCaseIdUseCase.Error.NotFound)
    }

    @Test
    fun `finding a patient that exists returns the patient's external case id`() {
        val fakeCaseEntity = CaseEntity(
            InternalCaseId.randomUUID(),
            "external case id",
            B64String("do not use"),
        )
        every { mockCaseRepository.findByInternalCaseId(any()) } returns Optional.of(fakeCaseEntity)

        val res = underTest(fakeCaseEntity.internalCaseId.toString())

        assertThat(res.get()).isEqualTo(fakeCaseEntity.externalCaseId)
    }
}
