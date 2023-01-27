package com.healthmetrix.connector.inbox.persistence.cases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.ExternalCaseId
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.persistence.B64StringConverter
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import java.util.Optional
import javax.persistence.Convert
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "cases")
data class CaseEntity(
    @Id
    val internalCaseId: InternalCaseId,
    val externalCaseId: ExternalCaseId,
    @Convert(converter = B64StringConverter::class)
    val privateKey: B64String,
)

interface CaseCrudRepository : CrudRepository<CaseEntity, InternalCaseId> {
    @Query("SELECT c FROM CaseEntity c WHERE externalCaseId IN :externalCaseIds")
    fun findByExternalCaseId(externalCaseIds: List<ExternalCaseId>): List<CaseEntity>
}

@Service
class CaseRepository(private val caseCrudRepository: CaseCrudRepository) {
    fun save(caseEntity: CaseEntity): CaseEntity = caseCrudRepository.save(caseEntity)

    fun findByExternalCaseId(externalCaseIds: List<ExternalCaseId>) = caseCrudRepository.findByExternalCaseId(externalCaseIds)

    fun findByInternalCaseId(internalCaseId: InternalCaseId): Optional<CaseEntity> = caseCrudRepository.findById(internalCaseId)
}
