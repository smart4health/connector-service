package com.healthmetrix.connector.commons

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class Bcp47LanguageTagTest {
    @Test
    fun `represents a IETF BCP 47 compliant language tag`() {
        assertThat(Bcp47LanguageTag("en-US").toString()).isEqualTo("en-US")
    }

    @Test
    fun `converts ISO lang tag to IETF BCP`() {
        assertThat(Bcp47LanguageTag("en_US").toString()).isEqualTo("en-US")
    }

    @Test
    fun `can extract lang from locale`() {
        assertThat(Bcp47LanguageTag("en-US").language).isEqualTo("en")
    }
}
