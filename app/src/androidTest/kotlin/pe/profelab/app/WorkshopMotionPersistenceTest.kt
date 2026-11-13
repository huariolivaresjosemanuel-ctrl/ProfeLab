package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.content.WorkshopSeed
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.newId
import pe.profelab.feature.taller.ActivityRunData

/** Uses the production save queue and UI; animation must never become a business operation. */
@RunWith(AndroidJUnit4::class)
class WorkshopMotionPersistenceTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}

    @Test fun resumeNextPreviousAndRecreationPreserveNotesWithoutCompletingOrConsuming(){
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val seed=WorkshopSeed(compose.activity);val definition=runBlocking {seed.templates().single {it.id=="mat-001"}}
        val catalog=runBlocking {seed.materials()};val stepIds=WorkshopFlowAdapter.read(definition).steps.map {it.id}
        assertTrue(stepIds.size>=3)
        val body=ActivityRunData.freshPayload(JSONObject(definition.json))
            .put("forkedFromTemplateId",definition.id).put("activityId",definition.id)
            .put("results","Registro propio anterior").put("conclusion","Reflexión propia anterior")
            .put("records",JSONArray().put(JSONObject().put("id","qa-record").put("observation","Conservar esta observación")))
            .put("workshopFlow",JSONObject().put("version",1).put("lastVisitedStepId",stepIds[1]).put("perspective","PRACTICAL"))
        body.getJSONArray("steps").getJSONObject(1).put("notes","Nota anterior del paso")
        val run=WorkRecord(newId(),"project","QA animación y persistencia",body.toString(),System.currentTimeMillis())
        val material=WorkshopCodec.materials(body,catalog).first()
        val stock=WorkRecord(newId(),"inventory","QA stock conservado",JSONObject().put("materialId",material.materialId)
            .put("materialKind",material.kind.name).put("quantity","20").put("reserved","0").put("unit",material.unit).toString(),System.currentTimeMillis())
        runBlocking {store.saveBatch(listOf(run,stock))}
        // Inventory creation is audited by T1; compare the persisted balance, not its input JSON.
        val storedStock=runBlocking {requireNotNull(store.record(stock.id))}
        val storedRun=runBlocking {requireNotNull(store.record(run.id))}
        val baseline=runBlocking {store.observeRecords().first().filterNot {it.id in setOf(run.id,stock.id)}.associate {it.id to it.json}}
        fun saved()=runBlocking {requireNotNull(store.record(run.id))}
        fun waitForVisit(stepId:String){compose.waitUntil(15000){JSONObject(saved().json).optJSONObject("workshopFlow")?.optString("lastVisitedStepId")==stepId}}
        fun selectedStep(index:Int){
            compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag("workshop-step-$index"))
            compose.onNodeWithTag("workshop-step-$index").assertIsOff()
        }
        try {
            compose.openWorkshopDefinition(run.id,"ideas")
            compose.workshopAction("workshop-begin")
            selectedStep(1)
            assertEquals("Opening and resuming must not rewrite a saved run",storedRun,saved())
            compose.workshopAction("workshop-step-next");selectedStep(2);waitForVisit(stepIds[2])
            compose.workshopAction("workshop-step-previous");selectedStep(1);waitForVisit(stepIds[1])
            compose.workshopAction("workshop-step-next");selectedStep(2);waitForVisit(stepIds[2])
            compose.activityRule.scenario.recreate();compose.waitForIdle();selectedStep(2)
            val after=JSONObject(saved().json)
            assertEquals(stepIds[2],after.getJSONObject("workshopFlow").getString("lastVisitedStepId"))
            val originalWithoutVisit=JSONObject(run.json).also {it.remove("workshopFlow")}
            val savedWithoutVisit=JSONObject(after.toString()).also {it.remove("workshopFlow")}
            assertEquals("Animation and visit may only update location",originalWithoutVisit.toString(),savedWithoutVisit.toString())
            assertEquals(storedStock,runBlocking {store.record(stock.id)})
            assertEquals(baseline,runBlocking {store.observeRecords().first().filterNot {it.id in setOf(run.id,stock.id)}.associate {it.id to it.json}})
        } finally {
            runBlocking {store.archiveRecord(run.id);store.archiveRecord(stock.id)}
        }
    }
}
