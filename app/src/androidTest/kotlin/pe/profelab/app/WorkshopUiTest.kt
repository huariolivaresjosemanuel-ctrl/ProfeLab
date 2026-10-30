package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith

/** Real Compose interactions; pending execution on emulator/device. */
@RunWith(AndroidJUnit4::class)
class WorkshopUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @get:Rule val diagnostics=object:TestWatcher(){
        override fun failed(error:Throwable,description:Description){
            android.util.Log.e("ProfeLab-QA",description.methodName,error)
            runCatching {android.util.Log.e("ProfeLab-QA",description.methodName+"\n"+compose.onRoot(useUnmergedTree=true).printToString())}
        }
    }
    private fun guide(){
        compose.onNodeWithTag("nav_taller").performClick()
        compose.waitUntil(15_000){compose.onAllNodesWithTag("workshop-open:mat-001").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-open:mat-001"))
        compose.onNodeWithTag("workshop-open:mat-001").performClick()
    }
    @Test fun catalogGuideHasStructuredMaterialsAndThreeModes(){
        guide()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Materiales y herramientas"))
        compose.onNodeWithText("Materiales y herramientas").assertIsDisplayed()
        compose.onAllNodes(hasText("|",substring=true)).assertCountEquals(0)
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
        compose.onNodeWithText("Comenzar mi proyecto").performScrollTo().performClick()
        compose.waitUntil(15_000){compose.onAllNodesWithText("Editar mi proyecto").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-step-0"))
        compose.onNodeWithTag("workshop-step-0").performClick()
        compose.waitForIdle()
        // Dispatch on the Activity's UI thread so Compose BackHandler flushes the Room save queue.
        compose.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }
        // The saved catalog scroll can keep its title outside LazyColumn composition.
        // Observe the screen itself, then scroll explicitly to the control under test.
        compose.waitUntil(15_000){compose.onAllNodesWithTag("workshop-catalog").fetchSemanticsNodes().isNotEmpty()}
        val created=runBlocking {store.observeRecords().first().single {it.kind=="project" && it.id !in before}}
        assertTrue(JSONObject(created.json).getJSONArray("steps").getJSONObject(0).getBoolean("done"))
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Mis ideas"))
        compose.onNodeWithText("Mis ideas").performClick()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Continuar creando"))
        compose.onNodeWithText("Continuar creando").assertIsDisplayed()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-card:${created.id}"))
        compose.onNodeWithTag("workshop-card:${created.id}").performClick()
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-step-0"))
        compose.onNodeWithTag("workshop-step-0").assertIsOn()
    }
}
