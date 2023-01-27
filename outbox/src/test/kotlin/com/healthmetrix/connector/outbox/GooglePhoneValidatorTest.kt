package com.healthmetrix.connector.outbox

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.Locale
import java.util.UUID

class GooglePhoneValidatorTest {

    private val phoneValidator = GooglePhoneValidator()
    private val internalCaseId = UUID.randomUUID()

    // German numbers
    @Test
    fun `german mobile phone number with country code should be returned correctly`() {
        assertThat(phoneValidator(internalCaseId, "+491704467860", Bcp47LanguageTag(Locale.GERMANY))).isEqualTo("+491704467860")
    }

    @Test
    fun `german mobile phone number with country code and zero in between should be returned correctly`() {
        assertThat(phoneValidator(internalCaseId, "+4901704467860", Bcp47LanguageTag(Locale.GERMANY))).isEqualTo("+491704467860")
    }

    @Test
    fun `german mobile phone number without country code should be returned correctly with country code`() {
        assertThat(phoneValidator(internalCaseId, "01704467860", Bcp47LanguageTag(Locale.GERMANY))).isEqualTo("+491704467860")
    }

    // American numbers
    @Test
    fun `american mobile phone number with country code should be returned correctly`() {
        assertThat(phoneValidator(internalCaseId, "+14437988637", Bcp47LanguageTag(Locale.US))).isEqualTo("+14437988637")
    }

    @Test
    fun `american mobile phone number without country code should be returned correctly with country code`() {
        assertThat(phoneValidator(internalCaseId, "4437988637", Bcp47LanguageTag(Locale.US))).isEqualTo("+14437988637")
    }

    // Portuguese numbers
    @Test
    fun `portuguese mobile phone number with country code should be returned correctly`() {
        assertThat(phoneValidator(internalCaseId, "+351960291276", Bcp47LanguageTag("pt-PT"))).isEqualTo("+351960291276")
    }

    @Test
    fun `portuguese mobile phone number without country code should be returned correctly with country code`() {
        assertThat(phoneValidator(internalCaseId, "960291276", Bcp47LanguageTag("pt-PT"))).isEqualTo("+351960291276")
    }

    // Error cases
    @Test
    fun `invalid phone number should return null`() {
        assertThat(phoneValidator(internalCaseId, "1234567890", Bcp47LanguageTag(Locale.GERMANY))).isNull()
    }

    @Test
    fun `characters should return null`() {
        assertThat(phoneValidator(internalCaseId, "test", Bcp47LanguageTag(Locale.GERMANY))).isNull()
    }

    @Test
    fun `invalid country code should return null`() {
        assertThat(phoneValidator(internalCaseId, "+11704467860", Bcp47LanguageTag(Locale.GERMANY))).isNull()
    }

    @Test
    fun `line number should return null`() {
        assertThat(phoneValidator(internalCaseId, "+493082007546", Bcp47LanguageTag(Locale.GERMANY))).isNull()
    }
}
