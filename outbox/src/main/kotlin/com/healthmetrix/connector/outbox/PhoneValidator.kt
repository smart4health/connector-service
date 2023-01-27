package com.healthmetrix.connector.outbox

import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.healthmetrix.connector.commons.Bcp47LanguageTag
import com.healthmetrix.connector.commons.InternalCaseId
import com.healthmetrix.connector.commons.logger
import net.logstash.logback.argument.StructuredArguments.kv
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service

interface PhoneValidator {
    operator fun invoke(internalCaseId: InternalCaseId, mobileNumber: String, lang: Bcp47LanguageTag): String?
}

@Service
@Profile("phonevalidator")
class GooglePhoneValidator : PhoneValidator {

    private val validNumberTypes =
        listOf(PhoneNumberUtil.PhoneNumberType.MOBILE, PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE)

    override operator fun invoke(
        internalCaseId: InternalCaseId,
        mobileNumber: String,
        lang: Bcp47LanguageTag,
    ): String? {
        try {
            val util = PhoneNumberUtil.getInstance()
            val parsedNumber = util.parse(mobileNumber, lang.country)

            if (!util.isValidNumber(parsedNumber)) {
                logger.info(
                    "Mobile number for case {} is not valid for the given region",
                    kv("internalCaseId", internalCaseId),
                )
                return null
            }

            val numberType = util.getNumberType(parsedNumber)
            if (!validNumberTypes.contains(numberType)) {
                logger.info(
                    "Mobile number for case {} is not a mobile number: {}",
                    kv("internalCaseId", internalCaseId),
                    numberType,
                )
                return null
            }

            // format to be called from DE
            return util.format(parsedNumber, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (ex: NumberParseException) {
            logger.info(
                "Error parsing mobile number for case {}. Error type is {}",
                kv("internalCaseId", internalCaseId),
                ex.errorType,
            )
            return null
        }
    }
}

@Service
@Profile("!phonevalidator")
class MockPhoneValidator : PhoneValidator {
    override fun invoke(internalCaseId: InternalCaseId, mobileNumber: String, lang: Bcp47LanguageTag) = mobileNumber
}
