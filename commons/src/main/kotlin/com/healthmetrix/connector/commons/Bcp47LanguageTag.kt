package com.healthmetrix.connector.commons

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.Locale

/**
 * Wrapper around Java Locale's class because the Locale class is not IETF BCP 47 Compliant.
 * As an example, the following language tag is not IETF BCP 47 compliant, but is how the Java
 * `Locale` class would represent it when `.toString()` is called: `en_US`. We ensure
 * that all locales are converted to hyphenated language tags as strings, while being able to understand
 * non-compliant inputs such as `en_US` when instantiating the class. The main advantage is that the
 * `Bcp47LanguageTag` can seamlessly transition between string and `Bcp47LanguageTag` while maintaining
 * IETF BCP 47 compliance.
 *
 */
data class Bcp47LanguageTag constructor(val locale: Locale) {
    constructor(lang: String) : this(Locale.forLanguageTag(lang.replace("_", "-")))

    val language: String = locale.language

    val country: String = locale.country

    override fun toString(): String = locale.toLanguageTag()

    fun encodeURL(): String = toString().encodeURL()
}

/**
 * Moved here because Kotlin 1.8 does not allow external Serializer definitions on data classes.
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = Bcp47LanguageTag::class)
object LocaleSerializer : KSerializer<Bcp47LanguageTag> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Locale", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): Bcp47LanguageTag = Bcp47LanguageTag(decoder.decodeString())

    override fun serialize(encoder: Encoder, value: Bcp47LanguageTag) {
        encoder.encodeString(value.toString())
    }
}
