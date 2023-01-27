@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
    alias(libs.plugins.org.jetbrains.kotlin.plugin.allopen)
}

allOpen {
    annotation("com.healthmetrix.connector.commons.AllOpen")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    implementation(projects.commons)
    implementation(projects.commonsSecrets)

    implementation(libs.org.bouncycastle.bcprov.jdk15on)

    testImplementation(libs.bundles.testing.base)
}
