package com.healthmetrix.connector.inbox.api.pairing.addcase

import com.fasterxml.jackson.annotation.JsonProperty
import com.healthmetrix.connector.commons.NoArg
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "AddCaseRequest")
@XmlAccessorType(XmlAccessType.FIELD)
@NoArg
data class AddCaseRequest(
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
