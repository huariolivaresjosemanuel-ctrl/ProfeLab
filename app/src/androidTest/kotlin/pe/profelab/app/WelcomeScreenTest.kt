package pe.profelab.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.*
import pe.profelab.core.ui.ProfeTheme
import pe.profelab.feature.profile.*
import java.io.File

@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
class WelcomeScreenTest {
    @get:Rule val compose=createComposeRule()
    private fun capture(name:String){
        compose.waitUntil(10_000){compose.onAllNodesWithTag("mascot-canonical-static").fetchSemanticsNodes().isNotEmpty()}
        val folder=File(requireNotNull(InstrumentationRegistry.getInstrumentation().context.getExternalFilesDir(null)),"visual").apply {mkdirs()}
        val bitmap=compose.onRoot().captureToImage().asAndroidBitmap()
        File(folder,"$name.png").outputStream().use {assertTrue(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it))}
    }
    @Test fun firstVisitIsStandaloneAndCanSkipPersonalization(){
        var confirmed=false
        compose.setContent {ProfeTheme {WelcomeScreen(WelcomeState(loading=false,mode=WelcomeMode.REGISTER),{name,role->assertEquals("",name);assertEquals(LocalRole.UNSPECIFIED,role);confirmed=true},{})}}
        capture("06_BIENVENIDA_NUEVO")
        compose.onAllNodesWithTag("nav_home").assertCountEquals(0)
        compose.onNodeWithTag("welcome-skip-registration").performScrollTo().performClick();assertTrue(confirmed)
    }
    @Test fun existingStudentKeepsNameAndAvoidsRegistration(){
        val prefs=UserPreferences(displayName="Ana",role=LocalRole.STUDENT,onboardingCompleted=true)
        compose.setContent {ProfeTheme {WelcomeScreen(WelcomeState(loading=false,mode=WelcomeMode.EXISTING_INTRO,prefs=prefs),{_,_->},{})}}
        capture("07_BIENVENIDA_EXISTENTE")
        compose.onAllNodesWithTag("welcome-start").assertCountEquals(0)
        compose.onNodeWithText("futuro docente Ana",substring=true).assertExists()
        compose.onNodeWithTag("welcome-continue").performScrollTo().assertIsDisplayed()
    }
    @Test fun reducedMotionUsesStaticCanonWithoutAnAnimationGate(){
        val prefs=UserPreferences(displayName="Luz",role=LocalRole.INTERN,onboardingCompleted=true,welcomeIntroSeen=true,reduceMotion=true)
        var continued=false
        compose.setContent {ProfeTheme {WelcomeScreen(WelcomeState(loading=false,mode=WelcomeMode.RETURNING,prefs=prefs),{_,_->continued=true},{})}}
        capture("08_BIENVENIDA_RETORNO_ESTATICO")
        compose.onNodeWithTag("welcome-continue").performScrollTo().performClick();assertTrue(continued)
    }
}
