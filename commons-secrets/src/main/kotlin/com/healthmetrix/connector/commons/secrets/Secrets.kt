package com.healthmetrix.connector.commons.secrets

import com.fasterxml.jackson.databind.ObjectMapper
import com.healthmetrix.connector.commons.AllOpen
import com.healthmetrix.connector.commons.logger
import java.io.IOException

const val OUTBOX_ADD_CASE_ENCRYPTION_KEY_SECRET = "outbox/encryption-keys/add-case-request"
const val OUTBOX_INVITATION_TOKEN_ENCRYPTION_KEY_SECRET = "outbox/encryption-keys/invitation-token"
const val OUTBOX_POSTGRES_PASSWORD = "outbox/postgres/application/password"
const val OUTBOX_MAILJET_SMS_TOKEN_SECRET_ID = "outbox/mailjet/sms"
const val OUTBOX_MAILJET_SECRET_ID = "outbox/mailjet/mail"
const val OUTBOX_OAUTH_CLIENT_SECRET_ID = "outbox/oauth-client"
const val OUTBOX_DATABASE_ENCRYPTION_AES_KEY = "outbox/encryption-keys/database"
const val INBOX_DATABASE_ENCRYPTION_AES_KEY = "inbox/encryption-keys/database"
const val INBOX_POSTGRES_PASSWORD = "inbox/postgres/application/password"
const val INBOX_OAUTH_CLIENT_SECRET_ID = "inbox/oauth-client"
const val OUTBOX_GATEWAY_BASIC_AUTH = "shared/gateway/outbox-basic-auth"

class SecretNotFoundException : Exception()

@AllOpen
class LazySecret<T>(val id: String, private val deserialize: (String) -> T?, private val retrieveSecret: () -> String?) {

    val value: T?
        get() = retrieveSecret()?.let(deserialize)

    val requiredValue: T
        get() = value ?: throw SecretNotFoundException()
}

internal typealias SecretFetcher = (String) -> String?

class Secrets(
    private val objectMapper: ObjectMapper,
    private val secretFetcher: SecretFetcher,
) {
    /**
     * Retrieves secret value as String for [secretId] by appending the application's environment as prefix to the [secretId]
     * @return LazySecret that lazily retrieves the secret as a string or null when the secret could not be retrieved
     */
    fun lazyGet(secretId: String) =
        LazySecret(secretId, { it }) { secretFetcher(secretId) }

    /**
     * Retrieves property with [jsonKey] on secret JSON object for [secretId] by appending the application's environment as prefix to the [secretId]
     * @return LazySecret that lazily retrieves the secret as a string or null when the secret could not be retrieved or [jsonKey] is not set
     */
    fun lazyGet(secretId: String, jsonKey: String): LazySecret<String> =
        LazySecret(secretId, { it }) { secretFetcher(secretId)?.jsonValue(jsonKey) }

    /**
     * Retrieves secret value as String for [secretId] by appending the application's environment as prefix to the [secretId]
     * @return LazySecret that lazily retrieves the secret as a string or null when the secret could not be retrieved
     */
    fun <T> lazyGet(secretId: String, deserializer: (String) -> T?): LazySecret<T> =
        LazySecret(secretId, deserializer) { secretFetcher(secretId) }

    /**
     * Retrieves property with [jsonKey] on secret JSON object for [secretId] by appending the application's environment as prefix to the [secretId]
     * @return LazySecret that lazily retrieves the secret as a string or null when the secret could not be retrieved or [jsonKey] is not set
     */
    fun <T> lazyGet(secretId: String, jsonKey: String, deserializer: (String) -> T?): LazySecret<T> =
        LazySecret(secretId, deserializer) { secretFetcher(secretId)?.jsonValue(jsonKey) }

    private fun String.jsonValue(key: String): String? = try {
        objectMapper.readTree(this)
            ?.path(key)
            ?.asText(null)
    } catch (e: IOException) {
        logger.warn("Error occurred reading secret with id", e)
        null
    }
}
