plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.kidslearning.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.kidslearning.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
        // Backend base URL and shared token are injected at build time from
        // secrets, never hard-coded here. See documentation/backend.md.
        buildConfigField("String", "BACKEND_BASE_URL", "\"${System.getenv("BACKEND_BASE_URL") ?: ""}\"")
        buildConfigField("String", "APP_SHARED_TOKEN", "\"${System.getenv("APP_SHARED_TOKEN") ?: ""}\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("main") {
        // The example lessons are the shared contract fixtures; bundling the same
        // files keeps a single source of truth between backend tests and the app.
        assets.srcDir(rootProject.projectDir.resolve("../lesson-schema/examples"))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")

    // JSON: kotlinx.serialization mirrors the shared lesson schema.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    // Room for local-first storage.
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Background work (large PDF processing / uploads).
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // On-device OCR (com.google.mlkit:text-recognition) is added when the camera
    // scan feature lands -- its bundled model adds ~30MB, so it doesn't ride along
    // before any code uses it.

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
