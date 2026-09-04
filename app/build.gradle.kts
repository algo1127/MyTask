plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.algo1127.mytask"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.algo1127.mytask"
        minSdk = 28
        targetSdk = 36
        versionCode = 25
        versionName = "Build_2026-09-04A"

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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}



dependencies {
    implementation(libs.androidx.compose.material3.android)
    implementation(libs.androidx.work.runtime.ktx)
    // Room
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.room.ktx)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation("androidx.compose.foundation:foundation:1.6.0")
    implementation("androidx.compose.animation:animation:1.6.8")
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation("com.google.code.gson:gson:2.10.1")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.datastore.core)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ REQUIRED FOR JAVA 8 TIME TYPES ON OLDER ANDROID
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    implementation("com.google.code.gson:gson:2.10.1")

    // ✅ Required for Java 8 time types on older Android
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")


    implementation("androidx.compose.material:material:1.7.x")
    implementation("androidx.compose.material3.adaptive:adaptive:1.0.x") 
}