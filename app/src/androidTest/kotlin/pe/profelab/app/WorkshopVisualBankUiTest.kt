package pe.profelab.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.content.WorkshopMediaBindings
import pe.profelab.core.content.WorkshopSeed

/** Confirms actual bitmap-backed Image semantics, not merely a placeholder or a slot container. */
@RunWith(AndroidJUnit4::class)
class WorkshopVisualBankUiTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}

    private fun visibleImage(slot:String,alt:String){
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasTestTag(slot))
        compose.waitUntil(30000){compose.onAllNodesWithContentDescription(alt,substring=true,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription(alt,substring=true,useUnmergedTree=true).assertIsDisplayed()
    }

    @Test fun firstExistingProjectDisplaysItsHeroMaterialAndTwoDistinctStepPhotosWithoutWrites(){
        val app=ApplicationProvider.getApplicationContext<ProfeApplication>();val store=app.graph.workspace
        val before=runBlocking {store.observeRecords().first().associate {it.id to it.json}}
        val seed=WorkshopSeed(compose.activity);val definition=runBlocking {seed.templates().single {it.id=="mat-001"}}
        val catalog=runBlocking {seed.materials()};val steps=WorkshopFlowAdapter.read(definition).steps
        val hero=requireNotNull(WorkshopMediaBindings.resolve(definition,"detail_hero"))
        compose.openWorkshopDefinition("mat-001","taller")
        visibleImage("workshop-media:detail_hero:mat-001::",hero.alt)
        compose.workshopAction("workshop-to-materials")
        val material=WorkshopCodec.materials(JSONObject(definition.json),catalog).first()
        val materialPhoto=requireNotNull(WorkshopMediaBindings.resolve(definition,"material_icon",materialId=material.materialId))
        visibleImage("workshop-media:material_icon:mat-001::${material.materialId}",materialPhoto.alt)
        compose.workshopAction("workshop-materials-next")
        val first=requireNotNull(WorkshopMediaBindings.resolve(definition,"step_photo",stepId=steps[0].id))
        visibleImage("workshop-media:step_photo:mat-001:${steps[0].id}:",first.alt)
        compose.workshopAction("workshop-step-next")
        val second=requireNotNull(WorkshopMediaBindings.resolve(definition,"step_photo",stepId=steps[1].id))
        assertNotEquals("Each step must use its assigned photo",first.storageId,second.storageId)
        visibleImage("workshop-media:step_photo:mat-001:${steps[1].id}:",second.alt)
        assertEquals("Visiting a public procedure must not create runs or mutate inventory",before,runBlocking {store.observeRecords().first().associate {it.id to it.json}})
    }
}
