package com.example.pixelwatchtagger

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for MainActivity
 *
 * These are simple unit tests that verify basic functionality without requiring
 * the Android framework. For UI testing with Compose, see MainActivityInstrumentedTest.
 */
class MainActivityTest {

    @Test
    fun testActivityClassExists() {
        // Verify the MainActivity class can be referenced
        val activityClass = MainActivity::class.java
        assertNotNull(activityClass)
        assertEquals("MainActivity", activityClass.simpleName)
    }

    @Test
    fun testPackageName() {
        // Verify the package name is correct
        val activityClass = MainActivity::class.java
        assertEquals("com.example.pixelwatchtagger", activityClass.`package`?.name)
    }

    @Test
    fun testActivityIsComponentActivity() {
        // Verify MainActivity extends ComponentActivity
        val activityClass = MainActivity::class.java
        val superClass = activityClass.superclass
        assertNotNull(superClass)
        assertEquals("ComponentActivity", superClass.simpleName)
    }
}
