package com.youandme.diary

import androidx.compose.ui.test.assertIsDisplayed
import com.youandme.diary.app.YouAndMeDiaryApp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class YouAndMeDiaryAppTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeRecordGeneratingResultFlowIsClickable() {
        composeRule.setContent {
            YouAndMeDiaryApp()
        }

        composeRule.onNodeWithTag("home-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("record-button").performClick()
        composeRule.onNodeWithTag("record-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("generate-button").performClick()
        composeRule.onNodeWithTag("generating-screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("已自动记入时间线").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("result-screen").assertIsDisplayed()
    }

    @Test
    fun favoriteSlideAppearsInMemoryBook() {
        composeRule.setContent {
            YouAndMeDiaryApp()
        }

        composeRule.onNodeWithText("打开纪念册").performClick()
        composeRule.onNodeWithTag("memory-screen").assertIsDisplayed()
        composeRule.onNodeWithText("小小胎动").assertIsDisplayed()
    }
}
