// 🔹 build.gradle.kts — la nivelul proiectului (root)
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false // ✅ necesar pt Firebase
}
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}

