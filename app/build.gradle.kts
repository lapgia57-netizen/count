import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val firebaseProperties = Properties().apply {
    rootProject.file("firebase.properties").takeIf { it.isFile }?.inputStream()?.use { load(it) }
}

fun firebaseValue(name: String): String =
    providers.gradleProperty(name)
        .orElse(providers.environmentVariable(name))
        .orElse(firebaseProperties.getProperty(name, ""))
        .getOrElse("")

android {
    namespace = "com.example.venha"
    compileSdk = 35

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    defaultConfig {
        applicationId = "com.example.venha"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "FIREBASE_API_KEY", buildConfigString(firebaseValue("FIREBASE_API_KEY")))
        buildConfigField("String", "FIREBASE_APP_ID", buildConfigString(firebaseValue("FIREBASE_APP_ID")))
        buildConfigField("String", "FIREBASE_PROJECT_ID", buildConfigString(firebaseValue("FIREBASE_PROJECT_ID")))
        buildConfigField("String", "FIREBASE_DATABASE_URL", buildConfigString(firebaseValue("FIREBASE_DATABASE_URL")))
        buildConfigField("String", "FIREBASE_STORAGE_BUCKET", buildConfigString(firebaseValue("FIREBASE_STORAGE_BUCKET")))
        buildConfigField("String", "FIREBASE_SENDER_ID", buildConfigString(firebaseValue("FIREBASE_SENDER_ID")))
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
