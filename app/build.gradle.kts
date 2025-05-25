plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("kotlin-kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.purrytify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.purrytify"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.datastore.core.android)
    implementation(libs.androidx.datastore.preferences.core.android)
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation(libs.volley)
    implementation(libs.espresso.core)
    implementation(libs.androidx.recyclerview.v130)

    val composeBom = platform("androidx.compose:compose-bom:2024.02.02")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx.v1120)
    implementation(libs.androidx.lifecycle.runtime.ktx.v270)
    implementation(libs.androidx.activity.compose.v182)

    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.material.icons.extended)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v115)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(libs.ui.test.junit4)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Retrofit & GSON
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    // OkHttp
    implementation(libs.okhttp)
    implementation(libs.logging.interceptor)

    // Encrypted SharedPreferences
    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.datastore.preferences)

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Room dependencies
    val roomVersion = "2.5.2"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // Image loading
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("io.coil-kt:coil-svg:2.4.0")
    implementation("io.coil-kt:coil-video:2.4.0")
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Background services dan notifications
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation ("androidx.media:media:1.7.0")
    implementation ("androidx.core:core:1.12.1")

    implementation ("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.15.1")

    implementation ("androidx.legacy:legacy-support-v4:1.0.0")

    implementation ("androidx.activity:activity-ktx:1.8.2")
    implementation ("androidx.fragment:fragment-ktx:1.6.2")

    // QR Code Generation
    implementation ("com.google.zxing:core:3.5.1")
    implementation ("com.journeyapps:zxing-android-embedded:4.3.0")

    // Camera X untuk QR Scanner
    implementation ("androidx.camera:camera-core:1.3.0")
    implementation ("androidx.camera:camera-camera2:1.3.0")
    implementation ("androidx.camera:camera-lifecycle:1.3.0")
    implementation ("androidx.camera:camera-view:1.3.0")

    // ML Kit untuk barcode scanning
    implementation ("com.google.mlkit:barcode-scanning:17.2.0")


    // LiveData, StateFlow, dan SharedFlow
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.25.1")

    // Media
    implementation("androidx.media:media:1.6.0")

    // Location services (Google Play Services)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Permissions handling
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // File utilities
    implementation("commons-io:commons-io:2.11.0")

    // Multipart request untuk Retrofit
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("com.google.android.libraries.places:places:3.3.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
}