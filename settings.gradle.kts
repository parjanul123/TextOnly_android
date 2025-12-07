pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // ✅ JitPack — necesar pentru WebRTC
        maven { url = uri("https://jitpack.io") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // ✅ aici din nou JitPack
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "TextOnly"
include(":app")
