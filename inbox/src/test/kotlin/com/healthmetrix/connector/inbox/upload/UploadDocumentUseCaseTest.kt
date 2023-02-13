package com.healthmetrix.connector.inbox.upload

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.inbox.persistence.domainresources.DomainResourceRepository
import com.healthmetrix.connector.inbox.persistence.domainresources.UploadableResource
import com.healthmetrix.connector.inbox.persistence.uploadattempts.UploadAttemptRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class UploadDocumentUseCaseTest {

    private val document1 =
        UploadableResource(
            internalCaseId = InternalCaseId.randomUUID(),
            internalResourceId = UUID.randomUUID(),
            refreshToken = "refresh token",
            privateKeyPemString = "very secret, much private",
        )

    private val document2 =
        UploadableResource(
            internalCaseId = document1.internalCaseId,
            internalResourceId = UUID.randomUUID(),
            refreshToken = "refresh token",
            privateKeyPemString = "very secret, much private",
        )

    private val mockDomainResourceRepository: DomainResourceRepository = mockk {
        every { deleteById(any()) } returns Unit
        every { getResourcesWithRefreshTokens() } returns
            listOf(document1, document2)
    }
    private val mockUploadAttemptRepository: UploadAttemptRepository = mockk {
        every { getLastAttemptAndCountById(any()) } returns null
        every { save(any()) } answers { arg(0) }
    }

    private val backoffFilter: BackoffPredicate = object : BackoffPredicate {
        override fun invoke(ignored: UploadableResource) = true
    }

    private val accessTokenFetcher: AccessTokenFetcher = object : AccessTokenFetcher {
        override fun invoke(entry: Map.Entry<UploadDocumentsUseCase.GroupedCaseInfo, List<UploadDocumentsUseCase.GroupedResource>>) =
            entry.component2().map {
                AccessTokenMapper.ResourceWithAccessToken(it.internalResourceId, "token", "pem")
            }
    }

    private val documentUploader: ResourceUploader = mockk()

    private val decryptResourceUseCase: DecryptResourceUseCase = mockk {
        every { this@mockk.invoke(any()) } returns Ok("jason")
    }

    private val underTest = UploadDocumentsUseCase(
        mockDomainResourceRepository,
        mockUploadAttemptRepository,
        backoffFilter,
        accessTokenFetcher,
        documentUploader,
        decryptResourceUseCase,
        50,
    )

    @Test
    fun `successful upload results in deleted document references`() {
        every { documentUploader.invoke(any(), any(), any(), any()) } answers {
            Ok(arg(3) as UUID)
        }

        underTest()

        verify {
            mockDomainResourceRepository.deleteById(document1.internalResourceId)
            mockDomainResourceRepository.deleteById(document2.internalResourceId)
        }
    }

    @Test
    fun `failed uploads result in upload attempts being recorded`() {
        every { documentUploader.invoke(any(), any(), any(), any()) } answers {
            Err(arg(3) as UUID to Exception())
        }

        underTest()

        verify {
            mockUploadAttemptRepository.save(
                match {
                    it.internalResourceId == document1.internalResourceId
                },
            )

            mockUploadAttemptRepository.save(
                match {
                    it.internalResourceId == document2.internalResourceId
                },
            )
        }
    }
}
