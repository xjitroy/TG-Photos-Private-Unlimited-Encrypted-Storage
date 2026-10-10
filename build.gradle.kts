import groovy.json.JsonOutput
import java.security.MessageDigest
import org.gradle.api.artifacts.component.ModuleComponentIdentifier

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// Review evidence only: resolved public package coordinates/hashes, no private configuration or APK.
tasks.register("writeResolvedDependencyInventory") {
    group = "verification"
    description = "Record app and draft security-module dependencies for license/privacy/advisory review"
    doLast {
        val scopes = linkedMapOf(
            ":app" to listOf("debugRuntimeClasspath", "releaseRuntimeClasspath"),
            ":core:crypto" to listOf("runtimeClasspath"),
            ":core:telegram" to listOf("runtimeClasspath"),
            ":core:cache" to listOf("runtimeClasspath"),
            ":core:backup" to listOf("debugRuntimeClasspath"),
            ":core:media" to listOf("debugRuntimeClasspath"),
        )
        val hashes = mutableMapOf<File, String>()
        val reports = mutableListOf<Map<String, Any>>()
        scopes.forEach { (module, names) ->
            names.forEach { name ->
                val configuration = project(module).configurations.getByName(name)
                val components = configuration.incoming.resolutionResult.allComponents.mapNotNull { component ->
                    (component.id as? ModuleComponentIdentifier)?.let { "${it.group}:${it.module}:${it.version}" }
                }.sorted()
                val external = configuration.incoming.artifactView {
                    componentFilter { it is ModuleComponentIdentifier }
                }.artifacts.artifacts
                val artifacts = external.map { artifact ->
                    val component = artifact.id.componentIdentifier as ModuleComponentIdentifier
                    val file = artifact.file
                    val sha = hashes.getOrPut(file) {
                        val digest = MessageDigest.getInstance("SHA-256")
                        file.inputStream().use { input ->
                            val buffer = ByteArray(65536)
                            while (true) {
                                val count = input.read(buffer)
                                if (count < 0) break
                                digest.update(buffer, 0, count)
                            }
                        }
                        digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
                    }
                    mapOf("coordinate" to "${component.group}:${component.module}:${component.version}", "artifact" to file.name,
                        "sha256" to sha, "bytes" to file.length(), "licenseReview" to "pending")
                }.sortedBy { it["coordinate"].toString() + it["artifact"].toString() }
                reports += mapOf("module" to module, "configuration" to name,
                    "resolvedComponents" to components, "artifacts" to artifacts)
            }
        }
        val report = mapOf("project" to "TG Photos", "formatVersion" to 1,
            "scope" to "Resolved JVM/Android app and draft-module inventory; not a complete native SBOM, license/advisory/privacy audit",
            "configurations" to reports)
        val output = layout.projectDirectory.file(".toolchain/dependency-evidence/resolved-runtime.json").asFile
        output.parentFile.mkdirs()
        output.writeText(JsonOutput.prettyPrint(JsonOutput.toJson(report)) + "\n")
        println("Resolved dependency inventory: ${reports.size} configurations, ${hashes.size} artifact hashes; review pending")
    }
}

// Defence in depth: even accidental Gradle packaging commands need explicit owner approval.
val packagingApproved = providers.gradleProperty("apkBuildApproved").orNull == "true"
gradle.taskGraph.whenReady {
    val blockedTasks = allTasks.filter {
        (it.name.startsWith("package") && !it.name.endsWith("Resources") && !it.name.endsWith("Assets")) || it.name.startsWith("assemble") ||
            (it.name.startsWith("bundle") && !it.name.startsWith("bundleLib") &&
                !it.name.endsWith("LocalLintAar") && !it.name.endsWith("ClassesToCompileJar") &&
                !it.name.endsWith("ClassesToRuntimeJar")) || it.name.startsWith("install") ||
            it.name.startsWith("publish") || it.name.startsWith("sign")
    }
    if (!packagingApproved && blockedTasks.isNotEmpty()) {
        throw GradleException("APK/AAB packaging requires explicit owner approval. Blocked tasks: " +
            blockedTasks.joinToString { it.path } + ". Compile/lint/JVM tests remain available.")
    }
}
