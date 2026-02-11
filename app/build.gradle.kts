plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)

    id("com.google.devtools.ksp") version "2.0.0-1.0.24"
}


android {
    namespace = "com.example.stepforge"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.stepforge"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }


    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // --- Compose: hepsini 1.7.5 setine çektik ---
    implementation("androidx.compose.ui:ui:1.7.5")
    implementation("androidx.compose.material:material:1.7.5")
    implementation("androidx.compose.material:material-icons-extended:1.7.5")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.foundation:foundation:1.7.5")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.remote.creation.core)
    implementation(libs.androidx.compose.foundation)
    debugImplementation("androidx.compose.ui:ui-tooling:1.7.5")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.34.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")

    // Diğer Compose foundation dependency'leri (lib'lerden gelen)
    implementation(libs.androidx.compose.foundation.foundation)
    implementation(libs.androidx.compose.foundation.foundation2)

    // Firebase Crashlytics (libs üzerinden)
    implementation(libs.firebase.crashlytics)

    // Biometric
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Android core + datastore + room
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Health Connect
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")

    // Confetti
    implementation("nl.dionsegijn:konfetti-compose:2.0.5")

    // Coil
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Test libs
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Google Play services Auth
    implementation("com.google.android.gms:play-services-auth:21.1.0")

    // Firebase Auth + Firestore
    implementation("com.google.firebase:firebase-auth-ktx:23.0.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.0.0")
}