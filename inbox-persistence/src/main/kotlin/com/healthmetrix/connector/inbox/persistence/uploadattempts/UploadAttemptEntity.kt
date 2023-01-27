package com.healthmetrix.connector.inbox.persistence.uploadattempts

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.util.UUID
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "upload_attempts")
data class UploadAttemptEntity(
    @Id
    val attemptId: UUID,
    val internalResourceId: UUID,
    val attemptedAt: Timestamp,
)

interface UploadAttemptCrudRepository : CrudRepository<UploadAttemptEntity, UUID> {

    @Query("SELECT a FROM UploadAttemptEntity a WHERE a.internalResourceId = :internalResourceId ORDER BY a.attemptedAt DESC")
    fun getAttemptsById(internalResourceId: UUID): List<UploadAttemptEntity>

    @Query("SELECT count(a) FROM UploadAttemptEntity a WHERE a.internalResourceId = :internalResourceId")
    fun countAttemptsById(internalResourceId: UUID): Int
}

@Repository
class UploadAttemptRepository(private val uploadAttemptCrudRepository: UploadAttemptCrudRepository) {
    private fun getLastAttemptById(internalResourceId: UUID): Timestamp? =
        uploadAttemptCrudRepository.getAttemptsById(internalResourceId).firstOrNull()?.attemptedAt

    private fun countAttemptsById(internalResourceId: UUID): Int =
        uploadAttemptCrudRepository.countAttemptsById(internalResourceId)

    @Transactional
    fun getLastAttemptAndCountById(internalResourceId: UUID): Pair<Timestamp, Int>? =
        getLastAttemptById(internalResourceId)?.let {
            it to countAttemptsById(internalResourceId)
        }

    fun save(uploadAttemptEntity: UploadAttemptEntity): UploadAttemptEntity =
        uploadAttemptCrudRepository.save(uploadAttemptEntity)
}
