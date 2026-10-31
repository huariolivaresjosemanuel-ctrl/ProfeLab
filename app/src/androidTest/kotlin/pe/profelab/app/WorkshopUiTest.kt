package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/** Real Compose interactions; pending execution on emulator/device. */
@RunWith(AndroidJUnit4::class)
class WorkshopUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enterExistingApp(){compose.dismissWelcomeIfPresent()}
    @get:Rule val diagnostics=object:TestWatcher(){
        override fun failed(error:Throwable,description:Description){
            android.util.Log.e("ProfeLab-QA",description.methodName,error)
            runCatching {android.util.Log.e("ProfeLab-QA",description.methodName+"\n"+compose.onRoot(useUnmergedTree=true).printToString())}
        }
    }
    private fun guide(){
        compose.openWorkshopDefinition()
    }
    @Test fun catalogGuideHasStructuredMaterialsAndThreeModes(){
        guide()
        compose.workshopAction("workshop-to-materials")
        compose.onNodeWithText("Materiales y herramientas").assertIsDisplayed()
        compose.onAllNodes(hasText("|",substring=true)).assertCountEquals(0)
        compose.workshopAction("workshop-materials-next")
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Docente"))
        compose.onNodeWithText("Docente").performClick()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Docente · Cómo enseñarlo"))
        compose.onNodeWithText("Docente · Cómo enseñarlo").assertIsDisplayed()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Académico"))
        compose.onNodeWithText("Académico").performClick()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Fundamentación académica"))
        compose.onNodeWithText("Fundamentación académica").assertIsDisplayed()
    }
    @Test fun startingCopyAndCompletingStepSurvivesRecreation(){
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val before=runBlocking {store.observeRecords().first().map {it.id}.toSet()}
        guide()
        compose.workshopAction("workshop-begin")
        compose.waitUntil(15_000){compose.onAllNodesWithTag("workshop-materials").fetchSemanticsNodes().isNotEmpty()}
        compose.workshopAction("workshop-materials-next")
        compose.workshopAction("workshop-purpose-next")
        compose.onNodeWithTag("workshop-page").performScrollToKey("steps")
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-step-0"))
        compose.onNodeWithTag("workshop-step-0").performClick()
        compose.waitForIdle()
        // Every BackHandler waits for the same Room save queue before popping one window.
        repeat(5){
            val old=compose.onNodeWithTag("workshop-page").fetchSemanticsNode().id
            compose.activityRule.scenario.onActivity {it.onBackPressedDispatcher.onBackPressed()}
            compose.waitUntil(15000){compose.onNodeWithTag("workshop-page").fetchSemanticsNode().id!=old}
        }
        compose.waitUntil(15_000){compose.onAllNodesWithTag("workshop-catalog").fetchSemanticsNodes().isNotEmpty()}
        val created=runBlocking {store.observeRecords().first().single {it.kind=="project" && it.id !in before}}
        assertTrue(JSONObject(created.json).getJSONArray("steps").getJSONObject(0).getBoolean("done"))
        compose.activityRule.scenario.recreate()
        compose.workshopAction("workshop-sector:ideas")
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Continuar creando"))
        compose.onNodeWithText("Continuar creando").assertIsDisplayed()
        // Activate the explicit open button, rather than a card center shared with nested controls.
        compose.workshopAction("workshop-open:${created.id}")
        compose.waitUntil(15_000){compose.onAllNodesWithTag("workshop-guide:${created.id}").fetchSemanticsNodes().isNotEmpty()}
        compose.workshopAction("workshop-begin")
        compose.onNodeWithTag("workshop-page").performScrollToKey("steps")
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-step-0"))
        compose.onNodeWithTag("workshop-step-0").assertIsOn()
    }
}
