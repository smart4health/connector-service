@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.allopen)
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
}

allOpen {
    annotation("com.healthmetrix.connector.commons.AllOpen")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    implementation(projects.commons)

    // spring
    implementation(libs.org.springframework.spring.context)

    // AWS
    implementation(libs.com.amazonaws.secretsmanager.aws.secretsmanager.caching.java)

    testRuntimeOnly(libs.org.slf4j.slf4j.simple)
    testImplementation(libs.bundles.testing.base)
}
