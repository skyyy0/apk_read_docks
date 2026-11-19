package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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
    fun `manifest resolves external pdf view and share intents`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pm = context.packageManager

        val viewPdfIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://com.whatsapp.provider.media/item/report.pdf"), "application/pdf")
        }
        val viewMatches = pm.queryIntentActivities(viewPdfIntent, 0)
        assertTrue(viewMatches.any { it.activityInfo.packageName == context.packageName })

        val octetPdfIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse("content://com.whatsapp.provider.media/item/invoice.pdf"), "application/octet-stream")
        }
        val octetMatches = pm.queryIntentActivities(octetPdfIntent, 0)
        assertTrue(octetMatches.any { it.activityInfo.packageName == context.packageName })

        val sendPdfIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, Uri.parse("content://com.whatsapp.provider.media/item/shared.pdf"))
        }
        val sendMatches = pm.queryIntentActivities(sendPdfIntent, 0)
        assertTrue(sendMatches.any { it.activityInfo.packageName == context.packageName })
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
