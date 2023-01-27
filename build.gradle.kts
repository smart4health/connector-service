import com.github.benmanes.gradle.versions.reporter.result.Result
import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    idea
    alias(libs.plugins.com.github.ben.manes.versions)

    alias(libs.plugins.org.springframework.boot) apply false

    alias(libs.plugins.org.jetbrains.kotlin.plugin.allopen) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.jpa) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.noarg) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.serialization) apply false
    alias(libs.plugins.org.jetbrains.kotlin.plugin.spring) apply false
    // https://youtrack.jetbrains.com/issue/KT-30276
    alias(libs.plugins.org.jetbrains.kotlin.jvm) apply false
}

allprojects {
    group = "com.healthmetrix"
    version = "1.0.0"
}

tasks.withType<DependencyUpdatesTask> {
    outputFormatter = closureOf<Result> {
        val sb = StringBuilder()
        outdated.dependencies.forEach { dep ->
            sb.append("${dep.group}:${dep.name} ${dep.version} -> ${dep.available.release ?: dep.available.milestone}\n")
        }
        if (sb.isNotBlank()) {
            rootProject.file("build/dependencyUpdates/outdated-dependencies").apply {
                parentFile.mkdirs()
                println(sb.toString())
                writeText(sb.toString())
            }
        } else {
            println("Up to date!")
        }
    }

    // no alphas, betas, milestones, release candidates
    // or whatever the heck jaxb-api is using
    rejectVersionIf {
        candidate.isSlf4jSpringBoot2Incompatible or
            candidate.version.contains("alpha", ignoreCase = true) or
            candidate.version.contains("beta", ignoreCase = true) or
            candidate.version.contains(Regex("M[0-9]*$")) or
            candidate.version.contains("RC", ignoreCase = true) or
            candidate.version.contains(Regex("b[0-9]+\\.[0-9]+$")) or
            candidate.version.contains("eap", ignoreCase = true)
    }
}

// Spring Boot does not yet support SLF4J 2.0.0 as spring-boot-starter-logging requires StaticLoggerBinder: https://github.com/spring-projects/spring-boot/issues/12649
val ModuleComponentIdentifier.isSlf4jSpringBoot2Incompatible: Boolean
    get() = (group == "org.slf4j") and (version.replace(".", "").toInt() >= 200)
