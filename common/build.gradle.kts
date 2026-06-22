plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.localkart.common"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    api("androidx.compose.ui:ui")
    api("androidx.compose.material3:material3")
    api("androidx.compose.material:material-icons-extended")
    api("androidx.compose.ui:ui-tooling-preview")
    api("androidx.navigation:navigation-compose:2.7.7")
    api("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.2")
    api("io.coil-kt:coil-compose:2.6.0")

    // Firebase
    api(platform("com.google.firebase:firebase-bom:33.1.1"))
    api("com.google.firebase:firebase-auth-ktx")
    api("com.google.firebase:firebase-firestore-ktx")
    api("com.google.firebase:firebase-storage-ktx")
    api("com.google.firebase:firebase-messaging-ktx")

    // Google Sign-In
    api("com.google.android.gms:play-services-auth:21.2.0")

    // QR generation
    api("com.google.zxing:core:3.5.3")
}
