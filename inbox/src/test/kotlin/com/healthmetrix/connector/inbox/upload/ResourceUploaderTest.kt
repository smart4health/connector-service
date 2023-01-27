package com.healthmetrix.connector.inbox.upload

import ca.uhn.fhir.context.FhirContext
import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Ok
import com.healthmetrix.connector.inbox.d4l.DomainResourceUploader
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.UUID

class ResourceUploaderTest {
    private val bundle = """
        {
          "resourceType": "Bundle",
          "type": "transaction"
        }
    """.trimIndent()

    private val patient = """
        {
            "resourceType": "Patient"
        }
    """.trimIndent()

    private val fakeResourceWithAccessToken = AccessTokenMapper.ResourceWithAccessToken(
        internalResourceId = UUID.randomUUID(),
        accessToken = "accessToken",
        privateKeyPemString = "privateKeyPemString",
    )

    private val fhirContext = FhirContext.forR4()

    private val mockDomainResourceUploader: DomainResourceUploader = mock()

    private val underTest = ResourceUploader(fhirContext, mockDomainResourceUploader)

    @Test
    fun `uploading non-json results in a Failure`() {
        val result = underTest(
            json = "garbage",
            accessToken = fakeResourceWithAccessToken.accessToken,
            privateKeyPemString = fakeResourceWithAccessToken.privateKeyPemString,
            internalResourceId = fakeResourceWithAccessToken.internalResourceId,
        )

        assertThat(result).isInstanceOf(Err::class.java)
    }

    @Test
    fun `uploading a non domain resource fhir resource results in a Failure`() {
        val result = underTest(
            json = bundle,
            accessToken = fakeResourceWithAccessToken.accessToken,
            privateKeyPemString = fakeResourceWithAccessToken.privateKeyPemString,
            internalResourceId = fakeResourceWithAccessToken.internalResourceId,
        )

        assertThat(result).isInstanceOf(Err::class.java)
    }

    @Test
    fun `uploading a domain resource results in Success`() {
        val result = underTest(
            json = patient,
            accessToken = fakeResourceWithAccessToken.accessToken,
            privateKeyPemString = fakeResourceWithAccessToken.privateKeyPemString,
            internalResourceId = fakeResourceWithAccessToken.internalResourceId,
        )

        assertThat(result).isInstanceOf(Ok::class.java)
    }
}
