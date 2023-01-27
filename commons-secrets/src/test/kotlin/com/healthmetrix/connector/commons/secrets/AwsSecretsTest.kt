package com.healthmetrix.connector.commons.secrets

import com.amazonaws.secretsmanager.caching.SecretCache
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

internal class AwsSecretsTest {

    private val client = "test"
    private val stage = "fake"
    private val secretCache: SecretCache = mock()
    private val underTest = AwsSecrets(client, stage, secretCache)

    @Test
    fun `get should return expected value`() {
        val expected = "totally secret"
        whenever(secretCache.getSecretString(any())) doReturn expected

        val result = underTest.invoke("secret/id")

        assertThat(result).isEqualTo(expected)
    }

    @Test
    fun `get should return null when secretCache returns null`() {
        whenever(secretCache.getSecretString(any())) doReturn null

        val result = underTest.invoke("secret/id")

        assertThat(result).isNull()
    }

    @ParameterizedTest
    @MethodSource("provideSecretIds")
    fun `get should append secretId to client and stage`(client: String, stage: String, secretId: String) {
        whenever(secretCache.getSecretString(any())) doReturn "stop warning about nulls thanks"

        val underTest = AwsSecrets(
            client,
            stage,
            secretCache,
        )

        underTest.invoke(secretId)

        val expected = "test/fake/secret/id"
        verify(secretCache).getSecretString(expected)
    }

    companion object {
        @JvmStatic
        fun provideSecretIds() = listOf(
            Arguments.of("test", "fake", "secret/id"),
            Arguments.of("test/", "fake", "secret/id"),
            Arguments.of("/test", "fake", "secret/id"),
            Arguments.of("/test/", "fake", "secret/id"),
            Arguments.of("test", "fake", "/secret/id"),
            Arguments.of("test/", "fake", "/secret/id"),
            Arguments.of("/test", "fake", "/secret/id"),
            Arguments.of("/test/", "fake", "/secret/id"),
            Arguments.of("//test//", "fake", "//secret/id"),
        )
    }
}
