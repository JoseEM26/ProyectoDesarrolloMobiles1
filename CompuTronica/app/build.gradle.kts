plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    alias(libs.plugins.googleServices)
}

android {
    namespace = "com.example.computronica"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.computronica"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        viewBinding = true
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Agregar la dependencia de RoundedImageView
    implementation("com.makeramen:roundedimageview:2.3.0")

    // Firebase: BoM + módulos KTX
    implementation(platform("com.google.firebase:firebase-bom:${libs.versions.firebaseBom.get()}"))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
// Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // ✅ NUEVAS DEPENDENCIAS PARA EL CHAT:
    implementation("com.google.firebase:firebase-database-ktx")  // Realtime Database
    implementation("com.google.firebase:firebase-storage-ktx")   // Storage (para imágenes)

}
