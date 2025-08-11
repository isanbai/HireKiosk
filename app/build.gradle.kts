plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "id.hirejob.kiosk"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.hirejob.kiosk"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // HTTP trigger default port
        buildConfigField("int", "DEFAULT_HTTP_PORT", "8765")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin { jvmToolchain(17) }

    // View Binding simple
    buildFeatures { 
        viewBinding = true
        buildConfig = true 
    }
}

dependencies {
    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")

    // Glide (GIF support)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // Preference UI
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Embedded HTTP server
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Core, appcompat, material
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}
