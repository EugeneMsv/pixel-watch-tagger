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
        minSdk = findProperty("minSdkVersion").toString().toInt()  // Wear OS 4+
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

    // Testing dependencies
    testImplementation("junit:junit:$junitVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // Android testing
    androidTestImplementation("androidx.test.ext:junit:$androidXTestVersion")
    androidTestImplementation("androidx.test:runner:$androidXTestVersion")
    androidTestImplementation("androidx.test:rules:$androidXTestVersion")
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

// JaCoCo configuration
jacoco {
    toolVersion = findProperty("jacocoVersion").toString()
}

tasks.register<JacocoReport>("jacocoTestReport") {
    dependsOn("testDebugUnitTest")

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(true)
    }

    val fileFilter = listOf(
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

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    val mainSrc = "${project.projectDir}/src/main/java"

    sourceDirectories.setFrom(files(mainSrc))
    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("**/*.exec", "**/*.ec")
    })
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

    val fileFilter = listOf(
        "**/R.class",
        "**/R$*.class",
        "**/BuildConfig.*",
        "**/Manifest*.*",
        "**/*Test*.*",
        "android/**/*.*"
    )

    val debugTree = fileTree("${project.layout.buildDirectory.get()}/tmp/kotlin-classes/debug") {
        exclude(fileFilter)
    }

    classDirectories.setFrom(files(debugTree))
    executionData.setFrom(fileTree(project.layout.buildDirectory.get()) {
        include("**/*.exec", "**/*.ec")
    })
}

tasks.named("check") {
    dependsOn("jacocoTestCoverageVerification")
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

// Custom task: test - runs all tests with coverage
tasks.register("test") {
    description = "Runs unit tests and generates coverage report"
    group = "verification"

    dependsOn("testDebugUnitTest", "jacocoTestReport")

    doLast {
        println("✓ Tests completed with coverage report")
        println("View coverage: app/build/reports/jacoco/jacocoTestReport/html/index.html")
    }
}
