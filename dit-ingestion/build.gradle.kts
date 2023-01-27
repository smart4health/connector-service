@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id("com.healthmetrix.kotlin.conventions")
}

dependencies {
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)

    api(files("libs/dil-v4.2.0.jar"))

    // dependencies from the gradle module metadata provided by D4L
    // and manually checked against dependencies already imported
    // $ cat <file>  | jq '.variants | map(select(.name | contains("runtimeElements")))[0] | .dependencies | map("\(.group):\(.module):\(.version.requires)") | .[]' -r
    // org.jetbrains.kotlin:kotlin-stdlib:1.4.32
    // ca.uhn.hapi.fhir:hapi-fhir-structures-r5:5.3.0
    // ca.uhn.hapi.fhir:hapi-fhir-structures-r4:5.3.0
    // ca.uhn.hapi.fhir:hapi-fhir-structures-dstu3:5.3.0
    // ca.uhn.hapi.fhir:hapi-fhir:5.3.0
    // ca.uhn.hapi.fhir:hapi-fhir-validation:5.3.0
    // ca.uhn.hapi.fhir:hapi-fhir-validation-resources-dstu3:5.3.0
    // ca.uhn.hapi.fhir:hapi-fhir-validation-resources-r4:5.3.0
    // ca.uhn.hapi.fhir:org.hl7.fhir.utilities:5.3.0
    // org.slf4j:slf4j-api:1.7.30
    // org.bouncycastle:bcprov-jdk15on:1.64
    // org.threeten:threetenbp:1.4.4
    // care.data4life.hc-fhir-sdk-java:fhir-java:1.4.0
    // care.data4life.hc-sdk-kmp:sdk-ingestion:1.11.0

    api(libs.care.data4life.hc.sdk.kmp.sdk.core)

    runtimeOnly(libs.ca.uhn.hapi.fhir.org.hl7.fhir.utilities)
    runtimeOnly(libs.org.threeten.threetenbp)
    runtimeOnly(libs.care.data4life.hc.fhir.sdk.java.fhir.java)
    runtimeOnly(libs.care.data4life.hc.sdk.kmp.sdk.ingestion)
    runtimeOnly(libs.care.data4life.hc.util.sdk.kmp.util)
    runtimeOnly(libs.care.data4life.hc.result.sdk.kmp.error)
}
