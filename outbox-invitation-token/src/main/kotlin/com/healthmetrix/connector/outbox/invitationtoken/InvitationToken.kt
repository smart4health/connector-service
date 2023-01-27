package com.healthmetrix.connector.outbox.invitationtoken

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.LocaleSerializer
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.encodeBase64
import com.healthmetrix.connector.outbox.invitationtoken.totp.TotpKey
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.time.Duration
import java.util.UUID

@Serializable
data class InvitationToken internal constructor(
    @Serializable(with = InternalCaseIdSerializer::class)
    val caseId: InternalCaseId,
    val phone: String, // TODO validated phone number should be a type
    internal val totpKey: TotpKey,
    val nonce: Int,
    @Serializable(with = LocaleSerializer::class)
    val locale: Bcp47LanguageTag,
) {

    constructor(caseId: InternalCaseId, phone: String, locale: Bcp47LanguageTag) : this(
        caseId,
        phone,
        TotpKey.random(),
        SecureRandom().nextInt(),
        locale,
    )

    fun pins(timeFrame: TimeFrame, now: Long = System.currentTimeMillis(), len: Int = 6) =
        timeFrame.windows(now).map { totpKey.pin(it, len) }

    companion object {
        fun encrypt(invitationToken: InvitationToken, aesKey: AesKey) = serialize(invitationToken)
            .toByteArray(Charsets.UTF_8)
            .let(aesKey::encrypt)
            .encodeBase64()

        fun decrypt(encryptedToken: B64String, aesKey: AesKey) = encryptedToken.decode()
            ?.let(aesKey::decrypt)
            ?.toString(Charsets.UTF_8)
            ?.let(this::deserialize)

        @Suppress("EXPERIMENTAL_API_USAGE")
        private fun serialize(invitationToken: InvitationToken) = Json.encodeToString(serializer(), invitationToken)

        @Suppress("EXPERIMENTAL_API_USAGE")
        private fun deserialize(json: String) = Json.decodeFromString(serializer(), json)
    }
}

data class TimeFrame(
    val duration: Duration = Duration.ofSeconds(30),
    val num: Int = 2,
) {
    fun windows(now: Long): List<Long> = (0 until num).map {
        (now - (it * duration.toMillis())) - (now % duration.toMillis())
    }
}

@OptIn(ExperimentalSerializationApi::class)
@Serializer(forClass = InternalCaseId::class)
object InternalCaseIdSerializer : KSerializer<InternalCaseId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("InternalCaseId", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): InternalCaseId {
        return UUID.fromString(decoder.decodeString())
    }

    override fun serialize(encoder: Encoder, value: InternalCaseId) {
        encoder.encodeString(value.toString())
    }
}
