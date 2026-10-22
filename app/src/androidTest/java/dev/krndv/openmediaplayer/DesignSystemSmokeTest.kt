package dev.krndv.openmediaplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class DesignSystemSmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun designFoundationPreviewIsDisplayed() {
        composeRule.onNodeWithText("Expressive by design").assertIsDisplayed()
        composeRule.onNodeWithText("Shared family foundation").assertIsDisplayed()
        composeRule.onNodeWithText("Dynamic color").assertIsDisplayed()
    }
}
