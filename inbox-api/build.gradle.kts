@file:Suppress("UnstableApiUsage")

import com.healthmetrix.connector.buildlogic.conventions.excludeReflect
import com.healthmetrix.connector.buildlogic.conventions.excludeSpringBootStarter
import com.healthmetrix.connector.buildlogic.conventions.registeringExtended
import org.springframework.boot.gradle.tasks.bundling.BootJar

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.noarg)
    alias(libs.plugins.org.springframework.boot)
}

noArg {
    annotation("com.healthmetrix.connector.commons.NoArg")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.org.jetbrains.kotlin.kotlin.reflect)

    // healthmetrix
    implementation(projects.commons)
    implementation(projects.commonsWeb)
    implementation(projects.commonsSecrets)
    implementation(projects.inbox)

    implementation(libs.javax.xml.bind.jaxb.api)
    runtimeOnly(libs.com.fasterxml.jackson.module.jackson.module.kotlin)

    // spring
    implementation(libs.org.springframework.boot.spring.boot.starter.web)
    implementation(libs.org.springframework.boot.spring.boot.starter.actuator)

    // HAPI FHIR
    implementation(libs.ca.uhn.hapi.fhir.hapi.fhir.base)
    implementation(libs.ca.uhn.hapi.fhir.hapi.fhir.structures.r4)

    // Swagger
    implementation(libs.org.springdoc.springdoc.openapi.kotlin) { excludeReflect() }
    implementation(libs.org.springdoc.springdoc.openapi.ui)

    testImplementation(libs.org.springframework.boot.spring.boot.starter.test) { excludeSpringBootStarter() }
    testRuntimeOnly(libs.com.h2database.h2)
    testImplementation(libs.bundles.testing.base)
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
