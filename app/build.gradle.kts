plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.miaolik.sitehub"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.miaolik.sitehub"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.3.8"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    jvmToolchain(17)
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull

android.signingConfigs {
    create("release") {
        if (releaseKeystorePath != null) {
            storeFile = file(releaseKeystorePath)
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
        }
    }
}

android.buildTypes {
    getByName("release") {
        isMinifyEnabled = false
        if (releaseKeystorePath != null) {
            signingConfig = android.signingConfigs.getByName("release")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
