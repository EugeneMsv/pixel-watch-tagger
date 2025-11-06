package com.example.pixelwatchtagger

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for MainActivity
 *
 * These tests run on an Android device or emulator and verify the Compose UI behavior.
 * They require the Android framework to run.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityInstrumentedTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testAppDisplaysTitle() {
        // Verify that the app title is displayed
        composeTestRule
            .onNodeWithText("Pixel Watch", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun testAppDisplaysSubtitle() {
        // Verify that "Tagger" text is displayed
        composeTestRule
            .onNodeWithText("Tagger", substring = true)
            .assertIsDisplayed()
    }
}
