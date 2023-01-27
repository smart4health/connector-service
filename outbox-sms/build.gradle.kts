@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring)
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    implementation(projects.commonsSecrets)
    implementation(projects.commons)

    // Mailjet
    implementation(libs.com.mailjet.mailjet.client)

    // spring
    implementation(libs.org.springframework.spring.context)

    testImplementation(libs.bundles.testing.base)
}
