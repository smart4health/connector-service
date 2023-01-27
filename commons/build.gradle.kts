@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.org.jetbrains.kotlinx.kotlinx.serialization.json)

    implementation(libs.com.fasterxml.jackson.core.jackson.annotations)

    api(libs.org.json)

    api(libs.org.slf4j.slf4j.api)

    // structured logging
    api(libs.net.logstash.logback.logstash.logback.encoder)
    implementation(libs.com.fasterxml.jackson.module.jackson.module.jaxb.annotations)

    api(libs.com.michael.bull.kotlin.result)

    testImplementation(libs.bundles.testing.base)
}
