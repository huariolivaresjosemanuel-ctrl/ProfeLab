package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/** Exercises genuine welcome controls, including the optional first Taller visit, then returns Home. */
internal fun ComposeTestRule.dismissWelcomeIfPresent(){
    waitUntil(15_000){onAllNodesWithTag("welcome-screen").fetchSemanticsNodes().isNotEmpty() || onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty()}
    val tag=when {
        onAllNodesWithTag("welcome-skip").fetchSemanticsNodes().isNotEmpty()->"welcome-skip"
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
    onNodeWithTag("nav_taller").performClick()
    waitUntil(15000){onAllNodesWithTag("workshop-catalog").fetchSemanticsNodes().isNotEmpty()}
    val preferences=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.preferences
    val current=runBlocking {preferences.preferences.first()}
    if(current.mascotVisible && current.personalizedGreetings && !current.tallerWelcomeSeen){
        waitUntil(15000){onAllNodesWithTag("taller-welcome-skip").fetchSemanticsNodes().isNotEmpty() || runBlocking {preferences.preferences.first().tallerWelcomeSeen}}
        if(onAllNodesWithTag("taller-welcome-skip").fetchSemanticsNodes().isNotEmpty())onNodeWithTag("taller-welcome-skip").performClick()
        waitUntil(15000){runBlocking {preferences.preferences.first().tallerWelcomeSeen}}
    }
    waitForIdle()
    onAllNodesWithTag("taller-welcome").assertCountEquals(0)
    onNodeWithTag("nav_home").performClick();waitForIdle()
}
