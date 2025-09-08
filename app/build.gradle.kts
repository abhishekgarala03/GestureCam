plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id ("de.undercouch.download")
}

android {
    namespace = "app.abhishekgarala.gesturecam"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.abhishekgarala.gesturecam"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

// import DownloadMPTasks task
extra["ASSET_DIR"] = "${projectDir}/src/main/assets"
extra["TEST_ASSETS_DIR"] = "${projectDir}/src/androidTest/assets"

// Apply external Gradle script
apply { from("download_tasks.gradle") }

dependencies {

    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")

    // CameraX
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")
    implementation("androidx.camera:camera-video:1.4.2")

    // MediaPipe
    implementation("com.google.mediapipe:tasks-vision:0.10.14")

    // Coroutines and Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.3")
    implementation ("androidx.lifecycle:lifecycle-extensions:2.2.0")
}