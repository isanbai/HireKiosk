plugins {
    id("com.android.application")
    kotlin("android")
    // tambahkan kapt/ksp hanya kalau memang ada dependency yang butuh (mis Glide compiler, Room, dsb)
}

android {
    namespace = "id.hirejob.kiosk" // samakan dengan package di AndroidManifest
    compileSdk = 34

    defaultConfig {
        applicationId = "id.hirejob.kiosk"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
}
