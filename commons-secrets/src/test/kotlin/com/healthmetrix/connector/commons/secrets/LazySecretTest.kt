package com.healthmetrix.connector.commons.secrets

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LazySecretTest {

    @Test
    fun `value should return secretString`() {
        val secretString = "test"

        val result = LazySecret("id", { it }) { secretString }.value

        assertThat(result).isEqualTo(secretString)
    }

    @Test
    fun `value should return null when secret could not be retrieved`() {
        val result = LazySecret("id", { it }) { null }.value

        assertThat(result).isNull()
    }

    @Test
    fun `value should return deserialized value`() {
        val deserialized = "deserialized"
        val deserializer: (String) -> String? = { deserialized }

        val result = LazySecret("id", deserializer) { "something" }.value

        assertThat(result).isEqualTo(deserialized)
    }

    @Test
    fun `value should return null when secret could not be deserialized`() {
        val secretString = "test"
        val deserializer: (String) -> String? = { null }

        val result = LazySecret("id", deserializer) { secretString }.value

        assertThat(result).isNull()
    }

    @Test
    fun `requiredValue should return secretString`() {
        val secretString = "test"

        val result = LazySecret("id", { it }) { secretString }.requiredValue

        assertThat(result).isEqualTo(secretString)
    }

    @Test
    fun `requiredValue should return null when secret could not be retrieved`() {
        assertThrows<SecretNotFoundException> { LazySecret("id", { it }) { null }.requiredValue }
    }

    @Test
    fun `requiredValue should return deserialized value`() {
        val deserialized = "deserialized"
        val deserializer: (String) -> String? = { deserialized }

        val result = LazySecret("id", deserializer) { "something" }.requiredValue

        assertThat(result).isEqualTo(deserialized)
    }

    @Test
    fun `requiredValue should throw SecretNotFoundException when secret could not be deserialized`() {
        val deserializer: (String) -> String? = { null }

        assertThrows<SecretNotFoundException> { LazySecret("id", deserializer) { "test" }.requiredValue }
    }
}
