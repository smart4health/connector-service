package com.healthmetrix.connector.outbox.persistence

import com.healthmetrix.connector.commons.InternalCaseId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "case_nonces")
data class CaseNonceEntity(
    @Id
    val internalCaseId: InternalCaseId,

    val nonce: Int,
)

interface CaseNonceCrudRepository : CrudRepository<CaseNonceEntity, InternalCaseId> {
    @Query("Select c from CaseNonceEntity c WHERE internalCaseId = :internalCaseId and nonce = :nonce")
    fun findByIdAndNonce(internalCaseId: InternalCaseId, nonce: Int): CaseNonceEntity?
}

@Service
class CaseNonceRepository(private val caseNonceCrudRepository: CaseNonceCrudRepository) {
    fun save(caseNonce: CaseNonceEntity): CaseNonceEntity = caseNonceCrudRepository.save(caseNonce)

    fun findByIdAndNonce(internalCaseId: InternalCaseId, nonce: Int): CaseNonceEntity? = caseNonceCrudRepository.findByIdAndNonce(internalCaseId, nonce)
}
