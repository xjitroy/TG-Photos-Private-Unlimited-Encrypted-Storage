plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// Defence in depth: even accidental Gradle packaging commands need explicit owner approval.
val packagingApproved = providers.gradleProperty("apkBuildApproved").orNull == "true"
gradle.taskGraph.whenReady {
    if (!packagingApproved && allTasks.any {
        (it.name.startsWith("package") && !it.name.endsWith("Resources") && !it.name.endsWith("Assets")) || it.name.startsWith("assemble") ||
            (it.name.startsWith("bundle") && !it.name.startsWith("bundleLib") && !it.name.endsWith("LocalLintAar")) || it.name.startsWith("install") ||
            it.name.startsWith("publish") || it.name.startsWith("sign")
    }) {
        throw GradleException("APK/AAB packaging requires explicit owner approval. Compile/lint/JVM tests remain available.")
    }
}
