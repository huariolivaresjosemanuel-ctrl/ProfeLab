package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Uses an empty search only; it creates no student, session or other personal record. */
@RunWith(AndroidJUnit4::class)
class Education32NavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enterExistingApp() { compose.dismissWelcomeIfPresent() }

    @Test fun sessionsStudentShortcutPreservesFiltersAndReturnsToOrigin() {
        compose.onNodeWithTag("nav_sessions").performClick()
        compose.onNodeWithText("Buscar sesiones").performTextReplacement("consulta-QA-contextual")
        compose.onNodeWithText("Mis alumnos · consultar seguimiento").performScrollTo().performClick()
        compose.onNodeWithText("Mis aulas").assertIsDisplayed()
        for(destination in listOf("home","taller","sessions","notebook","library")) {
            compose.onNodeWithTag("nav_$destination").assertExists()
        }
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Nueva sesión").assertIsDisplayed()
        compose.onNodeWithText("consulta-QA-contextual").assertExists()
        compose.onNodeWithText("Buscar sesiones").performTextReplacement("")
    }
}
