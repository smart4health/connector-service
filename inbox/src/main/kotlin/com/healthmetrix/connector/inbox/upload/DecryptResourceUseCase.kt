package com.healthmetrix.connector.inbox.upload

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.binding
import com.github.michaelbull.result.toResultOr
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class DecryptResourceUseCase(
    private val domainResourceRepository: DomainResourceRepository,
    @Qualifier("databaseEncryptionKey")
    private val encryptionKey: LazySecret<AesKey>,
) {
    operator fun invoke(internalResourceId: UUID): Result<String, Error> = binding {
        val entity = domainResourceRepository.findById(internalResourceId)
            .toResultOr { Error.NotFound }
            .bind()

        entity.encryptedJson.decode()
            ?.let(encryptionKey.requiredValue::decrypt)
            ?.toString(Charsets.UTF_8)
            .toResultOr { Error.DecryptionFailure }
            .bind()
    }

    sealed class Error {
        object NotFound : Error()

        object DecryptionFailure : Error()
    }
}
