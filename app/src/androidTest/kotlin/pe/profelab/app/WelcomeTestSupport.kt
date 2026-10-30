package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule

/** Exercises the real continue controls; does not prefill preferences or disable the welcome flow. */
internal fun ComposeTestRule.dismissWelcomeIfPresent(){
    waitUntil(15_000){onAllNodesWithTag("welcome-screen").fetchSemanticsNodes().isNotEmpty() || onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()}
    val tag=when {
        onAllNodesWithTag("welcome-skip-registration").fetchSemanticsNodes().isNotEmpty()->"welcome-skip-registration"
        onAllNodesWithTag("welcome-continue").fetchSemanticsNodes().isNotEmpty()->"welcome-continue"
        else->null
    }
    if(tag!=null){
        try {onNodeWithTag(tag).performScrollTo().performClick()}
        catch(error:AssertionError){
            // A returning greeting can expire between lookup and tap. Only accept that exact end state.
            if(onAllNodesWithTag("welcome-screen").fetchSemanticsNodes().isNotEmpty() || onAllNodesWithTag("nav_home").fetchSemanticsNodes().isEmpty())throw error
        }
    }
    waitUntil(15_000){onAllNodesWithTag("welcome-screen").fetchSemanticsNodes().isEmpty() && onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()}
}
