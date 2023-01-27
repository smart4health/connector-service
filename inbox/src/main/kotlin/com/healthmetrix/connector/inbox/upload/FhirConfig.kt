package com.healthmetrix.connector.inbox.upload

import ca.uhn.fhir.context.FhirContext
import care.data4life.ingestion.r4.FhirIngestionEngine
import care.data4life.ingestion.r4.Platform
import care.data4life.sdk.network.Environment
import com.healthmetrix.connector.commons.secrets.LazySecret
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FhirConfig {
    @Bean
    fun provideR4Context(): FhirContext = FhirContext.forR4()

    @Bean
    fun provideIngestionEngine(
        @Value("\${d4l.environment}")
        d4lEnvironment: String,
        @Value("\${d4l.fhir-package-directory}")
        fhirPackageDirectory: String,
        @Qualifier("oauthClientId")
        oauthClientId: LazySecret<String>,
    ): FhirIngestionEngine = FhirIngestionEngine.fromFhirPackageDirectory(
        deploymentEnvironment = Environment.fromName(d4lEnvironment),
        deploymentPlatform = Platform.SMART4HEALTH,
        dirPath = fhirPackageDirectory,
        clientId = oauthClientId.requiredValue,
    )
}
