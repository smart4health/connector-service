package com.healthmetrix.connector.outbox.config

import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.commons.secrets.OUTBOX_INVITATION_TOKEN_ENCRYPTION_KEY_SECRET
import com.healthmetrix.connector.commons.secrets.Secrets
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InvitationTokenKeyConfig {

    @Bean("invitationTokenEncryptionKey")
    fun retrieveInvitationTokenEncryptionKey(secrets: Secrets): LazySecret<AesKey> =
        secrets.lazyGet(OUTBOX_INVITATION_TOKEN_ENCRYPTION_KEY_SECRET, AesKey.Companion::buildFromB64String)
}
