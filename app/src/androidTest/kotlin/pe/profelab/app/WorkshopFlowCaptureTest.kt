package pe.profelab.app

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.*
import org.junit.runner.RunWith
import java.io.File
import pe.profelab.core.content.WorkshopSeed
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.content.WorkshopMediaBindings
import pe.profelab.core.content.WorkshopBoundMedia
import pe.profelab.core.content.WorkshopCodec
import org.json.JSONObject

/** Produces evidence only when actually executed on Android. No reference PNG is read here. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WorkshopFlowCaptureTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}
    private fun capture(name:String,photo:WorkshopBoundMedia?=null){
        compose.onNodeWithTag("workshop-page").performScrollToIndex(0)
        photo?.let {expected->
            // Image semantics only appear after bytes decode; do not capture a loading spinner.
            compose.waitUntil(30000){compose.onAllNodesWithContentDescription(expected.alt,substring=true,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}
        }
        compose.waitForIdle()
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        try{Assert.assertTrue(bitmap.width>0 && bitmap.height>0);File(qaCaptureDirectory("taller-3-0"),"$name.png").outputStream().use {Assert.assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}}finally{bitmap.recycle()}
    }
    @Test fun sevenApprovedWindowsAreNavigableAndCaptureRealComposeWithoutCreatingUserData(){
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val before=runBlocking {store.observeRecords().first().associate {it.id to it.json}}
        val seed=WorkshopSeed(compose.activity)
        val definition=runBlocking {seed.templates().single {it.id=="exp-001"}}
        val steps=WorkshopFlowAdapter.read(definition).steps
        val firstMaterial=WorkshopCodec.materials(JSONObject(definition.json),runBlocking {seed.materials()}).first()
        compose.onNodeWithTag("nav_taller").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-hub-ready").fetchSemanticsNodes().isNotEmpty()}
        capture("w00_mi_taller")
        compose.workshopAction("workshop-sector:experiments")
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-catalog-ready").fetchSemanticsNodes().isNotEmpty()}
        capture("w01_catalogo",requireNotNull(WorkshopMediaBindings.resolve(definition,"catalog_cover")))
        compose.workshopAction("workshop-open:exp-001")
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-guide:exp-001").fetchSemanticsNodes().isNotEmpty()}
        capture("w02_ficha",requireNotNull(WorkshopMediaBindings.resolve(definition,"detail_hero")))
        compose.workshopAction("workshop-to-materials");compose.onNodeWithTag("workshop-materials").assertExists()
        capture("w03_materiales",requireNotNull(WorkshopMediaBindings.resolve(definition,"material_icon",materialId=firstMaterial.materialId)))
        compose.workshopAction("workshop-materials-next");compose.onNodeWithTag("workshop-steps").assertExists()
        capture("w04_procedimiento",requireNotNull(WorkshopMediaBindings.resolve(definition,"step_photo",stepId=steps.first().id)))
        val count=steps.size
        Assert.assertTrue("Fixture must have actual steps",count>0)
        repeat(count){compose.workshopAction("workshop-step-next")}
        compose.onNodeWithTag("workshop-results").assertExists()
        capture("w05_resultados",requireNotNull(WorkshopMediaBindings.resolve(definition,"outcome_reference")))
        compose.workshopAction("workshop-results-next");compose.onNodeWithTag("workshop-classroom").assertExists()
        capture("w06_guia_docente",requireNotNull(WorkshopMediaBindings.resolve(definition,"teacher_reference")))
        compose.onAllNodesWithText("Ana",useUnmergedTree=true).assertCountEquals(0)
        Assert.assertEquals(before,runBlocking {store.observeRecords().first().associate {it.id to it.json}})
        val files=qaCaptureDirectory("taller-3-0").listFiles().orEmpty().filter {it.extension=="png"}
        Assert.assertEquals(setOf("w00_mi_taller","w01_catalogo","w02_ficha","w03_materiales","w04_procedimiento","w05_resultados","w06_guia_docente"),files.map {it.nameWithoutExtension}.toSet())
    }
}
