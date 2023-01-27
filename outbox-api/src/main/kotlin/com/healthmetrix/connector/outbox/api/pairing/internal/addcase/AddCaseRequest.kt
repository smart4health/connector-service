package com.healthmetrix.connector.outbox.api.pairing.internal.addcase

import com.fasterxml.jackson.annotation.JsonProperty
import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.NoArg
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

data class AddCaseRequest(
    val email: String,
    val mobileNumber: String,
    val publicKey: B64String,
    val lang: String? = null,
)

data class AddCaseRequestWrapper(
    @JsonProperty(value = "content", required = true)
    val content: ByteArray,

    @JsonProperty(value = "contentType", required = true)
    val contentType: String,

    @JsonProperty(value = "publicKey", required = true)
    val publicKey: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AddCaseRequestWrapper

        if (!content.contentEquals(other.content)) return false
        if (contentType != other.contentType) return false
        if (publicKey != other.publicKey) return false

        return true
    }

    override fun hashCode(): Int {
        var result = content.contentHashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + publicKey.hashCode()
        return result
    }
}

@XmlRootElement(name = "AddCaseRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArg
data class AddCaseRequestContent(
    @XmlElement(name = "email", required = true)
    @JsonProperty(value = "email", required = true)
    val email: String,

    @XmlElement(name = "mobileNumber", required = true)
    @JsonProperty(value = "mobileNumber", required = true)
    val mobileNumber: String,

    @XmlElement(name = "lang", required = false)
    @JsonProperty(value = "lang", required = false)
    val lang: String? = null,
)
