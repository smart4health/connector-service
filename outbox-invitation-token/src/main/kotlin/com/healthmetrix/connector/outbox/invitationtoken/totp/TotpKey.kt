package com.healthmetrix.connector.outbox.invitationtoken.totp

import com.healthmetrix.connector.commons.decodeHex
import com.healthmetrix.connector.commons.encodeHex
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.Serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.nio.ByteBuffer
import java.security.Key
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal const val ALGORITHM = "HmacSHA1"

// 10^len but with ints
private fun divisor(len: Int) = List(len) { 10 }.reduce(Int::times)

@Serializable
internal class TotpKey internal constructor(private val key: Key) {

    fun pin(t: Long, len: Int = 8): String {
        val mac = Mac.getInstance(ALGORITHM)
        val buf = ByteBuffer.allocate(mac.macLength).apply {
            putLong(t)
        }.array()

        mac.run {
            init(key)
            update(buf, 0, Long.SIZE_BYTES)
            doFinal(buf, 0)
        }

        val ints = buf.map(Byte::toInt)
        val offset: Int = ints.last() and 0x0f

        // code similar to the IETF TOTP Appendix A and other libraries
        val intermediate: Int = (
            ((ints[offset] and 0x7f) shl 24) or
                ((ints[offset + 1] and 0xff) shl 16) or
                ((ints[offset + 2] and 0xff) shl 8) or
                (ints[offset + 3] and 0xff)
            ) % divisor(
            len,
        )

        return intermediate.toString().padStart(len, '0')
    }

    override fun toString(): String = key.encoded.encodeHex()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TotpKey

        if (key != other.key) return false

        return true
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

    @OptIn(ExperimentalSerializationApi::class)
    @Serializer(forClass = TotpKey::class)
    companion object : KSerializer<TotpKey> {
        fun random() = TotpKey(
            KeyGenerator.getInstance(ALGORITHM).run {
                init(512) // 512 bits for SHA1
                generateKey()
            },
        )

        fun from(string: String) = TotpKey(SecretKeySpec(string.decodeHex(), ALGORITHM))

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TotpKey", PrimitiveKind.STRING)

        override fun deserialize(decoder: Decoder) =
            from(decoder.decodeString())

        override fun serialize(encoder: Encoder, value: TotpKey) {
            encoder.encodeString(value.toString())
        }
    }
}
