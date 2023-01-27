package com.healthmetrix.connector.outbox.persistence

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import javax.persistence.AttributeConverter
import javax.persistence.Convert
import javax.persistence.Converter
import javax.persistence.Entity
import javax.persistence.Id
import javax.persistence.Table

@Entity
@Table(name = "cases")
data class CaseEntity(
    @Id
    val internalCaseId: InternalCaseId,
    val status: Status,
    @Convert(converter = B64StringConverter::class)
    val publicKey: B64String,
    @Convert(converter = LocaleConverter::class)
    val lang: Bcp47LanguageTag,
) {

    constructor(internalCaseId: InternalCaseId, publicKey: B64String, lang: Bcp47LanguageTag) : this(
        internalCaseId,
        Status.UNPAIRED,
        publicKey,
        lang,
    )

    enum class Status {
        UNPAIRED,
        INVITATION_SENT,
        PIN_SENT,
        PIN_SUCCEEDED,
        OAUTH_SUCCEEDED,
    }
}

interface CaseCrudRepository : CrudRepository<CaseEntity, InternalCaseId>

@Service
class CaseRepository(private val caseCrudRepository: CaseCrudRepository) {
    fun findById(internalCaseId: InternalCaseId): CaseEntity? = caseCrudRepository.findByIdOrNull(internalCaseId)

    fun save(caseEntity: CaseEntity): CaseEntity = caseCrudRepository.save(caseEntity)

    fun delete(caseEntity: CaseEntity) = caseCrudRepository.delete(caseEntity)
}

@Converter
class LocaleConverter : AttributeConverter<Bcp47LanguageTag, String> {
    override fun convertToDatabaseColumn(attribute: Bcp47LanguageTag): String = attribute.toString()

    override fun convertToEntityAttribute(dbData: String): Bcp47LanguageTag = Bcp47LanguageTag(dbData)
}
