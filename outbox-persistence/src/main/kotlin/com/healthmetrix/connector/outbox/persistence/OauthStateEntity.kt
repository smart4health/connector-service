package com.healthmetrix.connector.outbox.persistence

import com.healthmetrix.connector.commons.InternalCaseId
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "oauth_states")
data class OauthStateEntity(
    @Id
    val internalCaseId: InternalCaseId,
    val state: String,
)

interface OauthStateCrudRepository : CrudRepository<OauthStateEntity, InternalCaseId> {
    @Query("SELECT o FROM OauthStateEntity o WHERE state = :state")
    fun findByState(state: String): List<OauthStateEntity>
}

@Service
class OauthStateRepository(private val oauthStateCrudRepository: OauthStateCrudRepository) {
    fun findById(internalCaseId: InternalCaseId): OauthStateEntity? =
        oauthStateCrudRepository.findByIdOrNull(internalCaseId)

    fun findByState(state: String): List<OauthStateEntity> =
        oauthStateCrudRepository.findByState(state)

    fun save(oauthStateEntity: OauthStateEntity): OauthStateEntity =
        oauthStateCrudRepository.save(oauthStateEntity)

    fun delete(oauthStateEntity: OauthStateEntity) =
        oauthStateCrudRepository.delete(oauthStateEntity)
}
