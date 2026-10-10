plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "app.tgphotos.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "app.tgphotos.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true }
}
kotlin { jvmToolchain(17) }
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:photos"))
    implementation(project(":feature:albums"))
    implementation(project(":feature:files"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:settings"))
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.core.splashscreen)
    implementation(libs.lifecycle.runtime)
    // The manifest's reviewed local initializer policy needs its class on compile and runtime paths.
    implementation(libs.startup.runtime)
    debugImplementation(libs.compose.tooling)
}
