// Build configuration for the app module itself.
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.elendheim.pictureeditor"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elendheim.pictureeditor"
        minSdk = 26          // Android 8.0 and up -> covers the vast majority of devices.
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        vectorDrawables { useSupportLibrary = true }
    }

    // Release is signed with the debug key on purpose. That keeps the APK
    // installable straight from the release page without juggling secrets.
    // Swap in a real keystore later if the app ever goes to the Play Store.
    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
        buildConfig = true   // exposes BuildConfig.VERSION_NAME to the About screen.
    }
    composeOptions {
        // Must line up with the Kotlin version above.
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core Android + lifecycle helpers.
    implementation("androidx.core:core-ktx:1.13.1")
    // Reads the rotation flag baked into photos so portraits load upright.
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    // Jetpack Compose. The BOM pins every Compose library to one tested set.
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")

    // Reading and writing the custom filter packs as JSON.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
}
