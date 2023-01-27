@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    implementation(projects.commons)

    implementation(libs.org.springframework.spring.web)
    implementation(libs.com.fasterxml.jackson.core.jackson.annotations)
}
