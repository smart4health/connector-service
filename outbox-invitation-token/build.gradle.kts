@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization)
}

dependencies {
    implementation(projects.commons)
    implementation(projects.commonsCrypto)
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.org.jetbrains.kotlinx.kotlinx.serialization.json)

    testImplementation(libs.bundles.testing.base)
    testImplementation(libs.io.mockk)
}
