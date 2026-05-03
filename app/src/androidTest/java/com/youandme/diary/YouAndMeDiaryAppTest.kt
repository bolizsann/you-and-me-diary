package com.youandme.diary

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.closeSoftKeyboard
import java.time.LocalDate
import java.time.YearMonth
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
        resetLocalDataFromHome()

        composeRule.onNodeWithTag("memory-button").performClick()
        composeRule.onNodeWithTag("memory-screen").assertIsDisplayed()
        composeRule.onNodeWithText("小小胎动").assertIsDisplayed()
    }

    @Test
    fun editedNoteTextSurvivesResultReopen() {
        val editedText = "测试：这一段解释已经被改写并保存。"

        resetLocalDataFromHome()
        composeRule.onNodeWithTag("record-button").performClick()
        composeRule.onNodeWithTag("generate-button").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("已自动记入时间线").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("note-edit-toggle").performClick()
        composeRule.onNodeWithTag("note-editor").performTextClearance()
        composeRule.onNodeWithTag("note-editor").performTextInput(editedText)
        closeSoftKeyboard()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("note-edit-toggle").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText(editedText).fetchSemanticsNodes().isNotEmpty()
        }
        returnHomeFromPage()

        composeRule.onNodeWithTag("timeline-button").performScrollTo().performClick()
        composeRule.onNodeWithText("打开这一天").performClick()
        composeRule.onNodeWithTag("result-screen").assertIsDisplayed()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText(editedText).fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithText(editedText).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun clearLocalDataResetsSettingsAndSeedData() {
        composeRule.onNodeWithTag("settings-button").performClick()
        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("settings-username-input").performTextClearance()
        composeRule.onNodeWithTag("settings-username-input").performTextInput("小雨")
        composeRule.onNodeWithTag("settings-due-date-input").performTextInput("2026-11-08")
        composeRule.onNodeWithTag("clear-local-data-button").performClick()

        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("你和小小的 ta。").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("memory-button").performClick()
        composeRule.onNodeWithText("小小胎动").assertIsDisplayed()
    }

    @Test
    fun newRecordAppearsAfterActivityRecreate() {
        resetLocalDataFromHome()

        composeRule.onNodeWithTag("record-button").performClick()
        composeRule.onNodeWithTag("generate-button").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("已自动记入时间线").fetchSemanticsNodes().isNotEmpty()
        }
        returnHomeFromPage()

        composeRule.activityRule.scenario.recreate()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithTag("timeline-button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("timeline-button").performScrollTo().performClick()

        val today = LocalDate.now()
        composeRule.onNodeWithText("${today.monthValue} 月 ${today.dayOfMonth} 日").assertIsDisplayed()
        composeRule.onNodeWithText("一次小小胎动").assertIsDisplayed()
    }

    @Test
    fun timelineMonthAndYearCanSwitch() {
        resetLocalDataFromHome()

        val initialMonth = YearMonth.of(2026, 4)

        composeRule.onNodeWithTag("timeline-button").performScrollTo().performClick()
        composeRule.onNodeWithTag("timeline-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("timeline-current-month")
            .assertTextEquals("${initialMonth.year} 年 ${initialMonth.monthValue} 月")

        composeRule.onNodeWithTag("timeline-previous-month").performClick()
        val previousMonth = initialMonth.minusMonths(1)
        composeRule.onNodeWithTag("timeline-current-month")
            .assertTextEquals("${previousMonth.year} 年 ${previousMonth.monthValue} 月")

        repeat(12) {
            composeRule.onNodeWithTag("timeline-next-month").performClick()
        }
        val nextYearMonth = previousMonth.plusMonths(12)
        composeRule.onNodeWithTag("timeline-current-month")
            .assertTextEquals("${nextYearMonth.year} 年 ${nextYearMonth.monthValue} 月")
    }

    private fun resetLocalDataFromHome() {
        composeRule.onNodeWithTag("settings-button").performClick()
        composeRule.onNodeWithTag("settings-screen").assertIsDisplayed()
        composeRule.onNodeWithTag("clear-local-data-button").performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("你和小小的 ta。").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun returnHomeFromPage() {
        composeRule.onNodeWithTag("page-back-button").performScrollTo().performClick()
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithTag("timeline-button").fetchSemanticsNodes().isNotEmpty()
        }
    }
}
