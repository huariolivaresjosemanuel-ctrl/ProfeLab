package pe.profelab.app

import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/** Captures real Compose output for human comparison; screenshots alone do not approve fidelity. */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class MainScreensCaptureTest {
    @get:Rule val compose=createAndroidComposeRule<MainActivity>()
    @Before fun enterExistingApp(){compose.dismissWelcomeIfPresent()}
    @Test fun captureTheFiveApprovedRootSections(){
        val graph=ApplicationProvider.getApplicationContext<ProfeApplication>().graph
        // Welcome/assistant/video suites keep their own evidence in the parent directory.
        val output=qaCaptureDirectory("main-screens")
        val sections=listOf(Triple("home","01_INICIO","Tu avance esta semana"),Triple("taller","02_TALLER",""),Triple("sessions","03_SESIONES","Sesiones"),Triple("notebook","04_CUADERNO","Cuaderno"),Triple("library","05_BIBLIOTECA","Biblioteca"))
        for((route,name,title) in sections){
            compose.onNodeWithTag("nav_$route").performClick()
            when(route){
                "home"->{
                    compose.waitUntil(15_000){compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()}
                    if(runBlocking {graph.repository.observeProfile().first()}!=null)compose.waitUntil(15_000){runBlocking {graph.preferences.preferences.first().onboardingCompleted}}
                }
                "taller"->{
                    compose.waitUntil(15_000){compose.onAllNodesWithTag("workshop-open:mat-001").fetchSemanticsNodes().isNotEmpty()}
                    compose.waitUntil(15_000){compose.onAllNodesWithContentDescription("Ilustración conceptual original; no foto del procedimiento").fetchSemanticsNodes().isNotEmpty()}
                }
                else->compose.waitUntil(15_000){compose.onAllNodesWithTag("section-$title").fetchSemanticsNodes().isNotEmpty()}
            }
            compose.waitForIdle()
            val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
            assertTrue(bitmap.width>0 && bitmap.height>0)
            File(output,"$name.png").outputStream().use {assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}
        }
        val expected=sections.map {it.second+".png"}.toSet()
        val actual=output.listFiles {f->f.extension=="png"}.orEmpty()
        assertEquals(5,actual.size)
        assertEquals(expected,actual.map {it.name}.toSet())
        actual.forEach {assertTrue("Captura vacía: ${it.name}",it.length()>0)}
    }
}
