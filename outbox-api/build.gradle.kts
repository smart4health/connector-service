@file:Suppress("UnstableApiUsage")

import com.healthmetrix.connector.buildlogic.conventions.excludeSpringBootStarter
import com.healthmetrix.connector.buildlogic.conventions.registeringExtended
import org.springframework.boot.gradle.tasks.bundling.BootJar

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.allopen)
    alias(libs.plugins.org.springframework.boot)
}

allOpen {
    annotation("com.healthmetrix.connector.commons.AllOpen")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    // healthmetrix
    implementation(projects.commons)
    implementation(projects.commonsSecrets)
    implementation(projects.commonsCrypto)
    implementation(projects.commonsWeb)
    implementation(projects.outbox)

    implementation(libs.com.nimbusds.nimbus.jose.jwt)

    // spring
    implementation(libs.org.springframework.boot.spring.boot.starter.web)
    implementation(libs.org.springframework.boot.spring.boot.starter.actuator)
    implementation(libs.org.springframework.boot.spring.boot.starter.thymeleaf)

    // xml deserialization
    implementation(libs.javax.xml.bind.jaxb.api)
    implementation(libs.com.fasterxml.jackson.dataformat.jackson.dataformat.xml)
    runtimeOnly(libs.com.fasterxml.jackson.module.jackson.module.kotlin)

    testImplementation(libs.org.springframework.boot.spring.boot.starter.test) { excludeSpringBootStarter() }
    testRuntimeOnly(libs.com.h2database.h2)
    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.io.mockk)
    testImplementation(projects.outboxEmail)
    testImplementation(projects.outboxSms)
}

testing {
    suites {
        val test by getting(JvmTestSuite::class)
        val integration by registeringExtended(test, libs.versions.junit.get()) {}
        val controller by registeringExtended(test, libs.versions.junit.get()) {}
    }
}

tasks.withType<BootJar> {
    setProperty("archiveFileName", "${project.name}.jar")
}
