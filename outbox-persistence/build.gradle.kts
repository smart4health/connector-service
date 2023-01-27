@file:Suppress("UnstableApiUsage")

import com.healthmetrix.connector.buildlogic.conventions.excludeSpringBootStarter
import com.healthmetrix.connector.buildlogic.conventions.registeringExtended

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.jpa)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.org.jetbrains.kotlin.kotlin.reflect)

    implementation(projects.commons)
    implementation(projects.commonsSecrets)
    implementation(projects.commonsCrypto)

    // liquibase
    runtimeOnly(libs.org.liquibase.liquibase.core)
    runtimeOnly(libs.jakarta.xml.bind.jakarta.xml.bind.api)

    implementation(libs.org.springframework.boot.spring.boot.starter.data.jpa)

    runtimeOnly(libs.org.postgresql)
    runtimeOnly(libs.com.h2database.h2)

    testImplementation(libs.org.springframework.boot.spring.boot.starter.test) { excludeSpringBootStarter() }
    testRuntimeOnly(libs.com.h2database.h2)
    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.io.mockk)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val database by registeringExtended(test, libs.versions.junit.get()) {}
    }
}
