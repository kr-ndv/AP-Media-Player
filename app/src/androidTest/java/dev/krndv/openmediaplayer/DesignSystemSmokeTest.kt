package dev.krndv.openmediaplayer

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class MediaLibrarySmokeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun libraryHomeIsDisplayed() {
        composeRule.onNodeWithText("Your media, your way.").assertIsDisplayed()
        composeRule.onNodeWithText("Your library starts here").assertIsDisplayed()
        composeRule.onNodeWithText("Audio").assertIsDisplayed()
        composeRule.onNodeWithText("Video").assertIsDisplayed()
    }

    @Test
    fun networkDestinationExplainsPlannedSources() {
        composeRule.onNodeWithText("Network").performClick()
        composeRule.onNodeWithText("Play from anywhere.").assertIsDisplayed()
        composeRule.onNodeWithText("Network shares").assertIsDisplayed()
    }
}
