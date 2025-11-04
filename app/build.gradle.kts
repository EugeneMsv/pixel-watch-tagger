plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.diffplug.spotless")
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

    lint {
        // Enable Compose-specific lint checks
        enable += setOf(
            "ComposeUnstableCollections",
            "ComposableNaming",
            "ComposeModifierMissing",
            "ComposeModifierReused",
            "ComposeRememberMissing",
            "CompositionLocalNaming",
            "ComposeParameterOrder",
            "ComposeViewModelInjection"
        )

        // Fail build on errors
        abortOnError = true

        // Generate reports
        htmlReport = true
        xmlReport = true

        // Baseline for existing issues
        baseline = file("lint-baseline.xml")
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

spotless {
    kotlin {
        target("src/**/*.kt")
        targetExclude("**/build/**/*.kt")

        // Use ktlint for Kotlin formatting
        ktlint("1.0.1")
            .editorConfigOverride(
                mapOf(
                    "indent_size" to "4",
                    "indent_style" to "space",
                    "max_line_length" to "100",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_trailing-comma-on-call-site" to "disabled",
                    "ktlint_standard_trailing-comma-on-declaration-site" to "disabled"
                )
            )

        // License header (optional)
        // licenseHeaderFile(rootProject.file("spotless/copyright.txt"))

        // Trim trailing whitespace
        trimTrailingWhitespace()

        // End files with newline
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint("1.0.1")
    }

    format("xml") {
        target("src/**/*.xml")
        targetExclude("**/build/**/*.xml")
        indentWithSpaces(4)
        trimTrailingWhitespace()
        endWithNewline()
    }
}
