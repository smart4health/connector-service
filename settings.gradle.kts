@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
}

val secrets = java.util.Properties().apply {
    with(file("secrets.properties")) {
        if (exists()) {
            inputStream().let(this@apply::load)
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()

        maven {
            url = uri("https://maven.pkg.github.com/d4l-data4life/hc-util-sdk-kmp")
            credentials {
                username = secrets.getProperty("gpr.user")
                    ?: System.getenv("PACKAGE_REGISTRY_USERNAME")
                password = secrets.getProperty("gpr.token")
                    ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
            }
        }

        maven {
            url = uri("https://maven.pkg.github.com/d4l-data4life/hc-fhir-sdk-java")
            credentials {
                username = secrets.getProperty("gpr.user")
                    ?: System.getenv("PACKAGE_REGISTRY_USERNAME")
                password = secrets.getProperty("gpr.token")
                    ?: System.getenv("PACKAGE_REGISTRY_TOKEN")
            }
        }
    }

    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
}

rootProject.name = "connector"
include("commons")
include("commons-secrets")
include("commons-crypto")
include("commons-web")
include("outbox")
include("outbox-api")
include("outbox-email")
include("outbox-invitation-token")
include("outbox-persistence")
include("inbox-api")
include("inbox")
include("inbox-persistence")
include("outbox-sms")
include("dit-ingestion")
