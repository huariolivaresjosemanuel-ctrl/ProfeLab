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

/** Produces evidence only when actually executed on Android. No reference PNG is read here. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WorkshopFlowCaptureTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enter(){compose.dismissWelcomeIfPresent()}
    private fun capture(name:String){
        compose.onNodeWithTag("workshop-page").performScrollToIndex(0)
        compose.waitForIdle()
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        try{Assert.assertTrue(bitmap.width>0 && bitmap.height>0);File(qaCaptureDirectory("taller-2-0"),"$name.png").outputStream().use {Assert.assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}}finally{bitmap.recycle()}
    }
    @Test fun sixWindowsAreNavigableAndCaptureRealComposeWithoutCreatingUserData(){
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val before=runBlocking {store.observeRecords().first().associate {it.id to it.json}}
        compose.onNodeWithTag("nav_taller").performClick()
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-sector:taller").fetchSemanticsNodes().isNotEmpty()}
        capture("w00_taller_portal")
        compose.workshopAction("workshop-sector:taller")
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-catalog-ready").fetchSemanticsNodes().isNotEmpty()}
        capture("a1_catalogo_candidato")
        compose.workshopAction("workshop-open:mat-003")
        compose.waitUntil(15000){compose.onAllNodesWithTag("workshop-guide:mat-003").fetchSemanticsNodes().isNotEmpty()}
        // Wait for the bundled conceptual image; absence is a failure, not a screenshot substitute.
        compose.waitUntil(15000){compose.onAllNodesWithContentDescription("Ilustración conceptual original; no foto del procedimiento").fetchSemanticsNodes().isNotEmpty()}
        capture("w01_ficha_visual")
        compose.workshopAction("workshop-to-materials");compose.onNodeWithTag("workshop-materials").assertExists();capture("w02_materiales")
        compose.workshopAction("workshop-materials-next");compose.onNodeWithTag("workshop-purpose").assertExists();capture("w03_proposito")
        compose.workshopAction("workshop-purpose-next");compose.onNodeWithTag("workshop-steps").assertExists();capture("w04_pasos_guiados")
        // A teacher shortcut is real navigation, without marking or consuming anything.
        compose.onNodeWithTag("workshop-page").performScrollToNode(hasText("Ver ficha del proyecto"))
        compose.onNodeWithText("Ver ficha del proyecto").performClick()
        compose.workshopAction("workshop-to-classroom");compose.onNodeWithTag("workshop-classroom").assertExists();capture("w05_clase_evaluacion")
        compose.onAllNodesWithText("Ana",useUnmergedTree=true).assertCountEquals(0)
        Assert.assertEquals(before,runBlocking {store.observeRecords().first().associate {it.id to it.json}})
        val files=qaCaptureDirectory("taller-2-0").listFiles().orEmpty().filter {it.extension=="png"}
        Assert.assertEquals(setOf("w00_taller_portal","a1_catalogo_candidato","w01_ficha_visual","w02_materiales","w03_proposito","w04_pasos_guiados","w05_clase_evaluacion"),files.map {it.nameWithoutExtension}.toSet())
    }
}
