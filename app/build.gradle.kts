plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.shahar.quickcontacts"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.shahar.quickcontacts"
        minSdk = 26
        targetSdk = 35
        versionCode = 6
        versionName = "1.5.0"
    }

    buildFeatures {
        viewBinding = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
