package com.healthmetrix.connector.inbox.api.sync

import com.healthmetrix.connector.inbox.sync.RenewRefreshTokensUseCase
import com.healthmetrix.connector.inbox.sync.SyncRefreshTokensUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class SyncRefreshTokensComponent(
    private val syncRefreshTokensUseCase: SyncRefreshTokensUseCase,
    private val renewRefreshTokensUseCase: RenewRefreshTokensUseCase,
) {
    @Scheduled(fixedRateString = "\${sync.fixed-rate}", initialDelayString = "\${sync.initial-delay}")
    fun syncKeys() {
        syncRefreshTokensUseCase()
        renewRefreshTokensUseCase()
    }
}
