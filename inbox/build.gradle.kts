@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.noarg)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
}

noArg {
    annotation("com.healthmetrix.connector.commons.NoArg")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    implementation(projects.commons)
    implementation(projects.commonsSecrets)
    implementation(projects.commonsCrypto)
    implementation(projects.commonsWeb)
    implementation(projects.inboxPersistence)
    implementation(projects.ditIngestion)

    // HAPI FHIR
    implementation(libs.ca.uhn.hapi.fhir.hapi.fhir.base)
    implementation(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.r4)
    implementation(libs.ca.uhn.hapi.fhir.hapi.fhir.validation)
    implementation(libs.ca.uhn.hapi.fhir.hapi.fhir.validation.resources.r4)

    implementation(libs.org.springframework.spring.context)
    implementation(libs.com.fasterxml.jackson.core.jackson.annotations)

    // Webflux
    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.reactor)
    implementation(libs.org.springframework.spring.webflux)
    implementation(libs.io.projectreactor.netty.reactor.netty)

    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.io.mockk)
}
