@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.noarg)
}

noArg {
    annotation("com.healthmetrix.connector.commons.NoArg")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    implementation(projects.commons)
    implementation(projects.commonsSecrets)
    implementation(projects.commonsCrypto)
    implementation(projects.outboxEmail)
    implementation(projects.outboxInvitationToken)
    implementation(projects.outboxPersistence)
    implementation(projects.outboxSms)

    implementation(libs.org.springframework.boot.spring.boot.starter)
    implementation(libs.com.googlecode.libphonenumber)
    implementation(libs.com.fasterxml.jackson.core.jackson.annotations)

    // Webflux
    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.kotlinx.coroutines.reactor)
    implementation(libs.org.springframework.spring.webflux)
    implementation(libs.io.projectreactor.netty.reactor.netty)

    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.io.mockk)
}
