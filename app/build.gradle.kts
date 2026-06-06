plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.milauncher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.milauncher"
        minSdk = 21 // Android 5.0+, suitable for most smart TVs
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    
    // Leanback Library for Android TV
    implementation("androidx.leanback:leanback:1.0.0")
    
    // Coroutines for OTA background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // OkHttp for OTA Download
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    
    // Glide for image loading (Optional, if we want remote app icons later)
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
