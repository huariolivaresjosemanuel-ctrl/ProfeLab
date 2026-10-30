package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enterExistingApp(){compose.dismissWelcomeIfPresent()}

    @Test fun allFiveTabsReachRealDestinations() {
        compose.onNodeWithTag("nav_taller").performClick()
        compose.onNodeWithText("Mi taller").assertIsDisplayed()
        compose.onNodeWithTag("nav_sessions").performClick()
        compose.onNodeWithText("Nueva sesión").assertIsDisplayed()
        compose.onNodeWithTag("nav_notebook").performClick()
        compose.onNodeWithText("Nueva nota").assertIsDisplayed()
        compose.onNodeWithTag("nav_library").performClick()
        compose.onNodeWithText("Importar PDF").assertIsDisplayed()
        compose.onNodeWithTag("nav_home").performClick()
        compose.onNodeWithText("Tu avance esta semana").assertIsDisplayed()
    }
    @Test fun taskEditorSurvivesActivityRecreation() {
        compose.onNodeWithContentDescription("Avisos locales").performClick()
        compose.onNodeWithText("Añadir tarea").performClick()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Título").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Título").performTextReplacement("Prueba de rotación")
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Prueba de rotación").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Prueba de rotación").assertExists()
    }
}
