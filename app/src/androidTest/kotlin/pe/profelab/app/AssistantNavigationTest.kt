package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantNavigationTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}
    @Test fun assistantOpensRealWorkshopInventory(){
        compose.onNodeWithTag("nav_taller").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-catalog").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("assistant-open").performClick()
        captureQaWindow("10_ASISTENTE_TALLER")
        val action=if(compose.onAllNodesWithTag("assistant.taller.inventory").fetchSemanticsNodes().isNotEmpty())"assistant.taller.inventory"else "assistant.global.no_inventory"
        compose.onNodeWithTag(action).performScrollTo().performClick()
        compose.waitUntil(15000){compose.onAllNodesWithText("Mi inventario").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Mi inventario").assertExists()
    }
    @Test fun assistantTeacherActionOpensExistingTeachingMode(){
        compose.onNodeWithTag("nav_taller").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-open:mat-001").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("workshop-open:mat-001").performClick()
        compose.onNodeWithTag("assistant-open").performClick()
        compose.onNodeWithTag("assistant.taller.teacher").performScrollTo().performClick()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Docente · Cómo enseñarlo"))
        compose.onNodeWithText("Docente · Cómo enseñarlo").assertExists()
    }
}
