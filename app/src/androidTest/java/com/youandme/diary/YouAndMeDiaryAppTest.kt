package com.youandme.diary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

class YouAndMeDiaryAppTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun homeRecordGeneratingResultFlowIsClickable() {
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
        composeRule.onNodeWithText("打开纪念册").performClick()
        composeRule.onNodeWithTag("memory-screen").assertIsDisplayed()
        composeRule.onNodeWithText("小小胎动").assertIsDisplayed()
    }

    @Test
    fun editedNoteTextSurvivesResultReopen() {
        val editedText = "测试：这一段解释已经被改写并保存。"

        composeRule.onNodeWithTag("record-button").performClick()
        composeRule.onNodeWithTag("generate-button").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("已自动记入时间线").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note-edit-toggle").performClick()
        composeRule.onNodeWithTag("note-editor").performTextClearance()
        composeRule.onNodeWithTag("note-editor").performTextInput(editedText)
        composeRule.onNodeWithTag("note-edit-toggle").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText(editedText).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("返回").performClick()

        composeRule.onNodeWithText("打开时间线").performClick()
        composeRule.onNodeWithText("打开这一天").performClick()

        composeRule.onNodeWithText(editedText).assertIsDisplayed()
    }
}
