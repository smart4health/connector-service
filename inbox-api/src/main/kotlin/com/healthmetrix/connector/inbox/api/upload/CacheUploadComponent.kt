package com.healthmetrix.connector.inbox.api.upload

import com.healthmetrix.connector.inbox.cache.CleanCacheUseCase
import com.healthmetrix.connector.inbox.upload.UploadDocumentsUseCase
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class CacheUploadComponent(
    private val uploadDocumentsUseCase: UploadDocumentsUseCase,
    private val cleanCacheUseCase: CleanCacheUseCase,
) {
    @Scheduled(fixedRateString = "\${upload.fixed-rate}", initialDelayString = "\${upload.initial-delay}")
    fun uploadAndCleanCache() {
        uploadDocumentsUseCase()
        cleanCacheUseCase()
    }
}
