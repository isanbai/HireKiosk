pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // taruh versi plugin DI SINI supaya tidak bentrok di modul
    plugins {
        id("com.android.application") version "8.7.2"
        kotlin("android") version "2.0.0"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "HireKiosk"
include(":app")
