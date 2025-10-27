plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.example.pixelwatchtagger"
    compileSdk = findProperty("compileSdkVersion").toString().toInt()

    defaultConfig {
        applicationId = "com.example.pixelwatchtagger"
        minSdk = findProperty("minSdkVersion").toString().toInt()  // Wear OS 4+
        targetSdk = findProperty("targetSdkVersion").toString().toInt()
        versionCode = findProperty("appVersionCode").toString().toInt()
        versionName = findProperty("appVersionName").toString()
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
        val javaVer = JavaVersion.toVersion(findProperty("javaVersion").toString().toInt())
        sourceCompatibility = javaVer
        targetCompatibility = javaVer
    }

    kotlinOptions {
        jvmTarget = findProperty("jvmTargetVersion").toString()
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    val wearComposeVersion = findProperty("wearComposeVersion").toString()
    val composeUiVersion = findProperty("composeUiVersion").toString()
    val activityComposeVersion = findProperty("activityComposeVersion").toString()
    val wearVersion = findProperty("wearVersion").toString()

    // Wear OS Compose
    implementation("androidx.wear.compose:compose-material:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-foundation:$wearComposeVersion")

    // Compose UI
    implementation("androidx.compose.ui:ui:$composeUiVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")

    // Wear OS libraries
    implementation("androidx.wear:wear:$wearVersion")
}
