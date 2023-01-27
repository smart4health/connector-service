package com.healthmetrix.connector.outbox.api.pairing.external.addcase

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.healthmetrix.connector.commons.web.ApiResponse
import com.nimbusds.jose.jwk.RSAKey
import org.springframework.http.HttpStatus

sealed class AddCaseEncryptionKeyResponse(status: HttpStatus) : ApiResponse(status) {
    @JsonSerialize(using = RsaKeySerializer::class)
    data class Success(val rsaJwk: RSAKey) : AddCaseEncryptionKeyResponse(HttpStatus.OK)

    object Error : AddCaseEncryptionKeyResponse(HttpStatus.INTERNAL_SERVER_ERROR) {
        val internalMessage: String = "An internal server error occurred trying to retrieve the addCase encryption key"
    }
}

class RsaKeySerializer : JsonSerializer<AddCaseEncryptionKeyResponse.Success>() {
    override fun serialize(
        value: AddCaseEncryptionKeyResponse.Success?,
        gen: JsonGenerator,
        serializers: SerializerProvider?,
    ) = gen.writeRawValue(value?.rsaJwk?.toJSONString())
}
