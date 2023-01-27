package com.healthmetrix.connector.outbox.config

import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.outbox.sms.Sms
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("sms")
@ConstructorBinding
class SmsFactory(
    private val srcName: String,
    private val templates: Map<String, String>,
) {
    fun make(locale: Bcp47LanguageTag, destNumber: String, pin: String) = Sms(
        srcName = srcName,
        destNumber = destNumber,
        text = templates.getValue(locale.toString()).format(pin),
    )
}
