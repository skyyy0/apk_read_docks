package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Lumina Docs", appName)
    }

    @Test
    fun `navigation across library create lab and settings works cleanly`() {
        composeRule.onNodeWithText("Lumina Docs").assertIsDisplayed()
        composeRule.onNodeWithTag("library_fab_new").assertIsDisplayed().performClick()

        composeRule.onNodeWithText("Create & Open").assertIsDisplayed()
        composeRule.onNodeWithTag("create_hub_list")
            .performScrollToNode(hasTestTag("create_blank_md"))
        composeRule.onNodeWithTag("create_blank_md").performClick()
        composeRule.onNodeWithTag("confirm_create_blank").performClick()

        composeRule.waitUntil(timeoutMillis = 5000) {
            composeRule.onAllNodes(hasTestTag("text_editor_body")).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("text_editor_body").assertIsDisplayed()
        composeRule.onNodeWithTag("editor_back_button").performClick()

        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.onNodeWithTag("theme_mode_dark").assertIsDisplayed().performClick()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasTestTag("toggle_power_saving"))
        composeRule.onNodeWithTag("toggle_power_saving").assertIsDisplayed().performClick()
    }
}
