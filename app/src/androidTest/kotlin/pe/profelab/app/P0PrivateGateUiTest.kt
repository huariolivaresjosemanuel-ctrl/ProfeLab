package pe.profelab.app

import android.graphics.Bitmap
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.education.*
import pe.profelab.core.ui.ProfeTheme
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs

/** Synthetic states only: no app graph, user database, key, ciphertext or private record is read. */
@RunWith(AndroidJUnit4::class)
class P0PrivateGateUiTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()

    private fun state(status:EducationProtectionStatus,stage:EducationProtectionStage=EducationProtectionStage.OPEN_DB)=
        EducationProtectionState(status,stage)

    private fun edgeToEdge(){compose.activityRule.scenario.onActivity {it.enableEdgeToEdge()}}

    @Test fun onlyReadyExposesContentAndProtectedFailureNeverDisplaysExceptionPayload(){
        edgeToEdge()
        val current=mutableStateOf(state(EducationProtectionStatus.VERIFYING))
        val contentCompositions=AtomicInteger(0);val retries=AtomicInteger(0)
        compose.setContent {ProfeTheme {EducationStorageGateContent(current.value,{retries.incrementAndGet()}){
            SideEffect {contentCompositions.incrementAndGet()}
            Text("Contenido de laboratorio listo",Modifier.testTag("p0-test-content"))
        }}}
        compose.onNodeWithTag("private-storage-retry").assertIsNotEnabled()
        compose.onNodeWithTag("private-storage-progress").assertExists()
        compose.onNodeWithTag("p0-test-content").assertDoesNotExist()
        compose.runOnIdle {current.value=state(EducationProtectionStatus.RETRYABLE_ERROR,EducationProtectionStage.CLEANUP_STAGING)}
        compose.onNodeWithTag("private-storage-retry").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("No terminó la revisión de archivos temporales. Esta etapa no confirma la apertura del espacio privado.").assertExists()
        compose.onNodeWithTag("p0-test-content").assertDoesNotExist()
        compose.runOnIdle {
            current.value=state(EducationProtectionStatus.UNRECOVERABLE_PROTECTED_ERROR,EducationProtectionStage.VERIFY_EDU)
                .copy(exceptionType="synthetic-payload-must-not-reach-ui")
        }
        compose.onNodeWithTag("private-storage-retry").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("synthetic-payload-must-not-reach-ui",substring=true).assertDoesNotExist()
        compose.onNodeWithTag("p0-test-content").assertDoesNotExist()
        assertEquals(0,contentCompositions.get());assertEquals(0,retries.get())
        saveScreenshot("protected-error")
        compose.runOnIdle {current.value=state(EducationProtectionStatus.READY,EducationProtectionStage.READY)}
        compose.onNodeWithTag("p0-test-content").assertIsDisplayed()
        compose.onNodeWithTag("private-storage-gate").assertDoesNotExist()
        assertTrue(contentCompositions.get()>0)
    }

    @Test fun retryIsExplicitAndDisabledWhileVerificationIsInProgress(){
        edgeToEdge()
        val current=mutableStateOf(state(EducationProtectionStatus.RETRYABLE_ERROR,EducationProtectionStage.SET_SECURE_DELETE))
        val retries=AtomicInteger(0)
        compose.setContent {ProfeTheme {EducationStorageGateContent(current.value,{
            retries.incrementAndGet();current.value=state(EducationProtectionStatus.VERIFYING,EducationProtectionStage.SET_SECURE_DELETE)
        }){Text("Nunca antes de READY")}}}
        compose.waitForIdle();assertEquals(0,retries.get())
        compose.onNodeWithTag("private-storage-retry").performScrollTo().performClick()
        compose.onNodeWithTag("private-storage-retry").assertIsNotEnabled()
        compose.onNodeWithTag("private-storage-retry").performTouchInput {click()}
        compose.waitForIdle();assertEquals(1,retries.get())
        compose.onNodeWithText("Nunca antes de READY").assertDoesNotExist()
    }

    @Test fun edgeToEdgeTitleUsesSafeDrawingOnceAndControlsStayReachable(){
        edgeToEdge()
        var topInset=0;var leftInset=0;var pagePadding=0
        compose.setContent {ProfeTheme {
            val density=LocalDensity.current;val direction=LocalLayoutDirection.current
            val insets=WindowInsets.safeDrawing
            val top=insets.getTop(density);val left=insets.getLeft(density,direction);val padding=with(density){16.dp.roundToPx()}
            SideEffect {topInset=top;leftInset=left;pagePadding=padding}
            EducationStorageGateContent(state(EducationProtectionStatus.RETRYABLE_ERROR,EducationProtectionStage.SET_SECURE_DELETE),{}){}
        }}
        compose.onNodeWithTag("private-storage-title").assertIsDisplayed()
        val title=compose.onNodeWithTag("private-storage-title").fetchSemanticsNode().boundsInRoot
        assertTrue("Title must be below system bars with one page padding",abs(title.top-(topInset+pagePadding))<=1f)
        assertTrue("Title must respect safe horizontal drawing",title.left>=leftInset+pagePadding-1f)
        saveScreenshot("edge-to-edge-title")
        compose.onNodeWithTag("private-storage-retry").performScrollTo().assertIsDisplayed().assertIsEnabled()
    }

    @Test fun callerConsumedInsetsAreNotAddedAgainByTheGate(){
        edgeToEdge()
        var topInset=0;var pagePadding=0
        compose.setContent {ProfeTheme {
            val density=LocalDensity.current;val insets=WindowInsets.safeDrawing
            val top=insets.getTop(density);val padding=with(density){16.dp.roundToPx()}
            SideEffect {topInset=top;pagePadding=padding}
            Box(Modifier.fillMaxSize().windowInsetsPadding(insets)){
                EducationStorageGateContent(state(EducationProtectionStatus.VERIFYING),{}){}
            }
        }}
        val top=compose.onNodeWithTag("private-storage-title").fetchSemanticsNode().boundsInRoot.top
        assertTrue("safeDrawing must be consumed once even under an inset-aware caller",abs(top-(topInset+pagePadding))<=1f)
    }

    @Test fun largeTextCanScrollToRetryAndReadyDoesNotPadOtherScreens(){
        edgeToEdge()
        val current=mutableStateOf(state(EducationProtectionStatus.RETRYABLE_ERROR,EducationProtectionStage.ENCRYPT_EDU))
        compose.setContent {ProfeTheme {
            val density=LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density,2f)){
                EducationStorageGateContent(current.value,{}){Box(Modifier.fillMaxSize().testTag("p0-ready-host"))}
            }
        }}
        compose.onNodeWithTag("private-storage-retry").performScrollTo().assertIsDisplayed()
        saveScreenshot("large-text-retry")
        compose.runOnIdle {current.value=state(EducationProtectionStatus.READY,EducationProtectionStage.READY)}
        assertEquals("READY leaves insets to the app host",0f,compose.onNodeWithTag("p0-ready-host").fetchSemanticsNode().boundsInRoot.top,1f)
    }

    /** Actual emulator screenshot, written only when this test really runs. Not a mockup. */
    private fun saveScreenshot(name:String){
        compose.waitForIdle()
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val bitmap=requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory=requireNotNull(instrumentation.targetContext.getExternalFilesDir("qa-p0"))
        directory.mkdirs()
        val destination=File(directory,"gate-api${Build.VERSION.SDK_INT}-$name.png")
        try {destination.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}}
        finally {bitmap.recycle()}
        assertTrue(destination.length()>0)
    }
}
