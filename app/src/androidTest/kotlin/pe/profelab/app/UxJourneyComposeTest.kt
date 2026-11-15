package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.*
import org.junit.runner.RunWith
import pe.profelab.core.content.*

/** Actual mat-001 journey, optional stock/students, explicit observation and real closure. */
@RunWith(AndroidJUnit4::class)
class UxJourneyComposeTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}
    @Test fun mat001ResumesItsStepAndSavesGeneralObservationWithoutCreatingStockOrStudents(){
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val before=runBlocking {store.observeRecords().first()};val ids=before.map {it.id}.toSet()
        fun saved()=runBlocking {store.observeRecords().first().single {it.id !in ids && it.kind=="project"}}
        compose.openWorkshopDefinition();compose.workshopAction("workshop-begin")
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-materials").fetchSemanticsNodes().isNotEmpty()}
        compose.waitUntil(15000){runBlocking {store.observeRecords().first().any {it.id !in ids && it.kind=="project"}}}
        compose.workshopAction("workshop-materials-to-guide");compose.workshopAction("workshop-classroom-procedure")
        compose.workshopAction("workshop-step-next")
        compose.waitUntil(15000){WorkshopFlowAdapter.read(saved()).lastVisitedStepId=="step-1"}
        Assert.assertEquals(0,WorkshopFlowAdapter.read(saved()).completedSteps)
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Guía docente"))
        compose.onNodeWithText("Guía docente").performClick();compose.workshopAction("workshop-classroom-procedure")
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-step-1"))
        compose.onNodeWithTag("workshop-step-1").assertIsOff()
        compose.workshopAction("workshop-steps-observe")
        val criterion=WorkshopFlowAdapter.read(saved()).criteria.first()
        compose.onNodeWithTag("workshop-general-criterion:${criterion.id}").performScrollTo().performClick()
        compose.onNodeWithTag("workshop-save-general-observation").performScrollTo().performClick()
        compose.waitUntil(15000){WorkshopCodec.strings(JSONObject(saved().json).optJSONObject("workshopFlow")?.opt("observedCriterionIds")).contains(criterion.id)}
        compose.workshopAction("workshop-steps-results");compose.workshopAction("workshop-save-and-finish")
        compose.waitUntil(15000){WorkshopClosure.savedAt(saved())>0}
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-close-confirmed").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("workshop-close-confirmed").performScrollTo().assertIsDisplayed()
        val after=runBlocking {store.observeRecords().first()}
        Assert.assertEquals(before.filter {it.kind=="inventory"}.associate {it.id to it.json},after.filter {it.kind=="inventory"}.associate {it.id to it.json})
        Assert.assertEquals(before.filter {it.kind.startsWith("edu_")},after.filter {it.kind.startsWith("edu_")})
        Assert.assertEquals(0,WorkshopFlowAdapter.read(saved()).completedSteps)
    }
}
