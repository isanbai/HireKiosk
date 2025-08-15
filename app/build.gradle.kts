plugins {
    id("com.android.application")
    kotlin("android")
    // id("org.jlleitschuh.gradle.ktlint")
    id("io.gitlab.arturbosch.detekt")
    // kapt kalau mau pakai Glide compiler (opsional)
    // kotlin("kapt")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    autoCorrect = true
    // opsional: tulis baseline agar warning lama di-skip
    // baseline = file("$projectDir/detekt-baseline.xml")
    reports {
        html.required.set(true) // laporan di build/reports/detekt/detekt.html
        xml.required.set(true)
        txt.required.set(false)
        sarif.required.set(false)
    }
}

android {
    namespace = "id.hirejob.kiosk"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.hirejob.kiosk"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources.excludes +=
            setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
            )
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity-ktx:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // Media3 (ExoPlayer)
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")

    // Glide (GIF/image)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    // kapt("com.github.bumptech.glide:compiler:4.16.0") // kalau mau annotation processor

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // HTTP server ringan
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
