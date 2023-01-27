package com.healthmetrix.connector.outbox.usecases

import com.healthmetrix.connector.commons.B64String
import com.healthmetrix.connector.commons.crypto.AesKey
import com.healthmetrix.connector.commons.secrets.LazySecret
import com.healthmetrix.connector.outbox.invitationtoken.InvitationToken
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class SendSmsUseCase(
    @Qualifier("invitationTokenEncryptionKey") private val invitationTokenEncryptionKey: LazySecret<AesKey>,
) {
    operator fun invoke(invitationToken: B64String): Result {
        val token = InvitationToken.decrypt(invitationToken, invitationTokenEncryptionKey.requiredValue)
            ?: return Result.InvalidToken

        return Result.Success(token.phone.takeLast(4))
    }

    sealed class Result {
        object InvalidToken : Result()

        data class Success(val phoneLastFour: String) : Result()
    }
}
