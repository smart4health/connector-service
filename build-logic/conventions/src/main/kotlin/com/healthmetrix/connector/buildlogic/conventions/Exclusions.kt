package com.healthmetrix.connector.buildlogic.conventions

import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.kotlin.dsl.exclude

fun ExternalModuleDependency.exclusionsTestRuntime() {
    exclude(group = "junit", module = "junit")
}

fun ExternalModuleDependency.exclusionsSpringTestRuntime() {
    exclude(group = "junit", module = "junit")
}

fun ExternalModuleDependency.exclusionsSpringTestImplementation() {
    exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    exclude(group = "com.vaadin.external.google", module = "android-json")
    exclude(module = "junit")
    exclude(module = "mockito-core")
}

fun ExternalModuleDependency.excludeReflect() {
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
}

fun ExternalModuleDependency.excludeSpringBootStarter() {
    // Clashes with com.nhaarman.mockitokotlin2.verifyZeroInteractions
    exclude(group = "org.mockito", module = "mockito-core")
    // clashes with org.json.JSONObject
    exclude(group = "com.vaadin.external.google", module = "android-json")
}
