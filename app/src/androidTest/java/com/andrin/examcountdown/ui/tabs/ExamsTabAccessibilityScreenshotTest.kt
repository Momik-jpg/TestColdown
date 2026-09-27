package com.andrin.examcountdown.ui.tabs

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.andrin.examcountdown.model.Exam
import com.andrin.examcountdown.ui.buildExamPresentation
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExamsTabAccessibilityScreenshotTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun noResultsState_exposesRecoveryAction() {
        composeRule.setContent {
            MaterialTheme {
                NoExamResultsCard(onClearFilters = {})
            }
        }

        composeRule.onNodeWithTag("exam-no-results").assertIsDisplayed()
        composeRule.onNodeWithText("Keine Prüfungen für diesen Filter.").assertIsDisplayed()
        composeRule.onNodeWithText("Filter zurücksetzen").assertHasClickAction()
    }

    @Test
    fun examDetails_exposesCoreInformation_andRendersScreenshot() {
        val exam = Exam(
            title = "Mathematik Prüfung",
            subject = "MAT",
            location = "Raum 3",
            startsAtEpochMillis = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        )

        composeRule.setContent {
            MaterialTheme {
                ExamDetailsDialog(
                    exam = exam,
                    presentation = buildExamPresentation(exam),
                    collisions = emptyList(),
                    onDismiss = {},
                    onPlanStudy = {}
                )
            }
        }

        composeRule.onNodeWithTag("exam-details-dialog").assertIsDisplayed()
        composeRule.onNodeWithText("Prüfungsdetails", useUnmergedTree = true)
            .assert(hasAccessibilityHeading())
        composeRule.onNodeWithText("Fach").assertIsDisplayed()
        composeRule.onNodeWithText("Lern-Sessions planen").assertHasClickAction()

        val screenshot = composeRule
            .onNodeWithTag("exam-details-dialog")
            .captureToImage()
        assertTrue(
            "Die Detailansicht muss einen sichtbaren Screenshot liefern.",
            screenshot.width > 0 && screenshot.height > 0
        )
    }

    @Test
    fun longExamTitle_remainsFullyVisible() {
        val longTitle = "Mathematik Prüfung mit einer langen vollständig lesbaren Bezeichnung"
        val exam = Exam(
            title = longTitle,
            subject = "MAT",
            location = "Raum 3",
            startsAtEpochMillis = System.currentTimeMillis() + 24L * 60L * 60L * 1000L
        )

        composeRule.setContent {
            MaterialTheme {
                ExamDetailsDialog(
                    exam = exam,
                    presentation = buildExamPresentation(exam),
                    collisions = emptyList(),
                    onDismiss = {},
                    onPlanStudy = {}
                )
            }
        }

        composeRule.onNodeWithText(longTitle, useUnmergedTree = true).assertIsDisplayed()
    }
}

private fun hasAccessibilityHeading() = SemanticsMatcher("is an accessibility heading") { node ->
    node.config.contains(SemanticsProperties.Heading)
}
