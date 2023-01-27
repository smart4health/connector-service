package com.healthmetrix.connector.commons.secrets

import com.amazonaws.secretsmanager.caching.SecretCache
import com.amazonaws.services.secretsmanager.model.ResourceNotFoundException
import com.healthmetrix.connector.commons.logger

class AwsSecrets(
    client: String,
    stage: String,
    private val secretCache: SecretCache = SecretCache(),
) : SecretFetcher {

    private val prefix = "${client.trim('/')}/${stage.trim('/')}"

    override fun invoke(secretId: String): String? = try {
        val secret: String? = "$prefix/${secretId.trimStart('/')}".let { fullId ->
            logger.debug("Retrieving secret with id $fullId")
            secretCache.getSecretString(fullId)
        }

        if (secret == null) {
            logger.warn("Retrieved null or empty secret for prefix $prefix and id $secretId")
        }

        secret
    } catch (e: ResourceNotFoundException) {
        logger.warn("Could not retrieve secret for prefix $prefix and id $secretId")
        null
    }
}
