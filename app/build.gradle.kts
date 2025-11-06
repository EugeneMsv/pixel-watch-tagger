plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.diffplug.spotless")
    id("jacoco")
}

android {
    namespace = "com.example.pixelwatchtagger"
    compileSdk = findProperty("compileSdkVersion").toString().toInt()

    defaultConfig {
        applicationId = "com.example.pixelwatchtagger"
        minSdk = findProperty("minSdkVersion").toString().toInt() // Wear OS 4+
        targetSdk = findProperty("targetSdkVersion").toString().toInt()
        versionCode = findProperty("appVersionCode").toString().toInt()
        versionName = findProperty("appVersionName").toString()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
            all {
                it.useJUnitPlatform()
            }
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
        enable +=
            setOf(
                "ComposableNaming",
                "CompositionLocalNaming"
            )

        // Fail build on errors
        abortOnError = true

        // Generate reports
        htmlReport = true
        xmlReport = true
    }
}

dependencies {
    val wearComposeVersion = findProperty("wearComposeVersion").toString()
    val composeUiVersion = findProperty("composeUiVersion").toString()
    val activityComposeVersion = findProperty("activityComposeVersion").toString()
    val wearVersion = findProperty("wearVersion").toString()
    val junitVersion = findProperty("junitVersion").toString()
    val androidXTestVersion = findProperty("androidXTestVersion").toString()

    // Wear OS Compose
    implementation("androidx.wear.compose:compose-material:$wearComposeVersion")
    implementation("androidx.wear.compose:compose-foundation:$wearComposeVersion")

    // Compose UI
    implementation("androidx.compose.ui:ui:$composeUiVersion")
    implementation("androidx.activity:activity-compose:$activityComposeVersion")

    // Wear OS libraries
    implementation("androidx.wear:wear:$wearVersion")

    // Testing dependencies - JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")

    // Android testing
    androidTestImplementation("androidx.test.ext:junit:$androidXTestVersion")
    androidTestImplementation("androidx.test:runner:$androidXTestVersion")
    androidTestImplementation("androidx.test:rules:$androidXTestVersion")

    // Compose UI testing
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeUiVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeUiVersion")
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
                    "ktlint_standard_trailing-comma-on-declaration-site" to "disabled",
                    "ktlint_standard_function-naming" to "disabled"
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

// JaCoCo configuration
jacoco {
    toolVersion = findProperty("jacocoVersion").toString()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")
    mustRunAfter(
        "generateDebugAndroidTestResValues",
        "checkDebugAndroidTestAarMetadata",
        "mergeDebugAndroidTestAssets",
        "jacocoDebug",
        "compressDebugAssets",
        "mergeReleaseResources",
        "generateReleaseResValues",
        "checkReleaseAarMetadata",
        "generateDebugAndroidTestLintModel",
        "mergeReleaseAssets"
    )

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*",
            "**/*\$ViewInjector*.*",
            "**/*\$ViewBinder*.*",
            "**/Lambda$*.class",
            "**/Lambda.class",
            "**/*Lambda.class",
            "**/*Lambda*.class",
            "**/*_MembersInjector.class",
            "**/Dagger*Component*.*",
            "**/*Module_*Factory.class",
            "**/di/module/*",
            "**/*_Factory*.*",
            "**/*Module*.*",
            "**/*Dagger*.*",
            "**/*Hilt*.*"
        )

    val debugTree =
        fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(project.layout.buildDirectory.get()) {
            include("**/*.exec", "**/*.ec")
        }
    )
}

tasks.register<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn("jacocoTestReport")

    val minimumCoverage = findProperty("minimumCoverageRequired").toString().toBigDecimal()

    violationRules {
        rule {
            limit {
                minimum = minimumCoverage
            }
        }
    }

    val fileFilter =
        listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*",
            "android/**/*.*"
        )

    val debugTree =
        fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }

    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(
        fileTree(project.layout.buildDirectory.get()) {
            include("**/*.exec", "**/*.ec")
        }
    )
}

// Custom task: styleCheck - runs code formatting and linting
tasks.register("styleCheck") {
    description = "Runs code formatting and linting checks"
    group = "verification"

    dependsOn("spotlessApply", "lint")

    doLast {
        println("✓ Code style check completed")
    }
}

// Configure existing test task to run style check and all variants
tasks.register("testUnit") {
    description = "Runs all unit tests across all build variants"
    group = "verification"
    dependsOn("styleCheck", "testDebugUnitTest", "testReleaseUnitTest")

    doLast {
        println("✓ Unit tests completed")
    }
}

// Wrapper task for instrumented tests
tasks.register("androidTest") {
    description = "Runs instrumented tests on connected device/emulator"
    group = "verification"
    dependsOn("connectedDebugAndroidTest")

    doLast {
        println("✓ Instrumented tests completed")
    }
}

// Test with coverage and verification
tasks.register("testWithCoverage") {
    description = "Runs unit tests with coverage report and verification"
    group = "verification"
    dependsOn("testUnit", "jacocoTestReport", "jacocoTestCoverageVerification")

    doLast {
        println("✓ Tests completed with coverage verification")
        println("View coverage: app/build/reports/jacoco/jacocoTestReport/html/index.html")
    }
}

// Combined test task (unit + instrumented with coverage)
tasks.register("testAll") {
    description = "Runs all tests (unit + instrumented) with coverage"
    group = "verification"
    dependsOn("testWithCoverage", "androidTest")

    doLast {
        println("✓ All tests completed with coverage")
    }
}
// Test comment to trigger changelog tracker
