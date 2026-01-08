plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.textonly"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.textonly"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ✅ Activitate & rezultate
    implementation("androidx.activity:activity-ktx:1.9.3")

    // ✅ Autentificare biometrică
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // ✅ ML Kit & CameraX
    implementation("com.google.mlkit:barcode-scanning:17.2.0")
    implementation("androidx.camera:camera-core:1.3.2")
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")

    // ✅ Guava
    implementation("com.google.guava:guava:31.1-android")
    // ✅ ZXing pentru scanare QR
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    // ✅ Java-WebSocket pentru comunicare WebSocket
    implementation("org.java-websocket:Java-WebSocket:1.5.4")
    // ✅ STOMP pentru mesagerie în timp real
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    // ✅ RxJava și RxAndroid pentru STOMP
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")

    // ✅ Networking (OkHttp)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // ✅ Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    // ✅ WebRTC (folosește versiunea stabilă)
    implementation("io.github.webrtc-sdk:android:114.5735.01")

    // ✅ Teste
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
