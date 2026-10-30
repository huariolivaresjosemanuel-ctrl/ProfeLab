package pe.profelab.app

import android.animation.ValueAnimator
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.test.platform.app.InstrumentationRegistry
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import pe.profelab.core.model.*
import pe.profelab.core.ui.ProfeTheme
import pe.profelab.feature.profile.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicBoolean

@RunWith(AndroidJUnit4::class)
class FlowPlaybackTest {
    @get:Rule val compose=createAndroidComposeRule<ComponentActivity>()
    @Test fun backgroundReleasesPlaybackAndResumeKeepsPositionWithoutLoop(){
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val oldScale=Settings.Global.getString(instrumentation.targetContext.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE)
        fun shell(command:String){ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command)).use {it.readBytes()}}
        shell("settings put global animator_duration_scale 1")
        try {
        compose.waitUntil(5000){ValueAnimator.areAnimatorsEnabled()}
        val positions=AtomicInteger(0);val completions=AtomicInteger(0);val failed=AtomicBoolean(false)
        compose.setContent {
            var state by remember {mutableStateOf(WelcomeState(loading=false,mode=WelcomeMode.RETURNING))}
            ProfeTheme {LocalWelcomeVideo(WelcomeClip.GREETING,state,Modifier.height(280.dp).aspectRatio(9f/16f),
                {_,position->positions.set(position);state=state.copy(positionMs=position)},
                {completions.incrementAndGet();state=state.copy(videoComplete=true)},
                {failed.set(true);state=state.copy(videoFailed=true)})}
        }
        compose.waitUntil(10000){compose.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription,"Vídeo sin audio en reproducción")).fetchSemanticsNodes().isNotEmpty() || failed.get()}
        assertFalse("Native video decoder failed",failed.get())
        Thread.sleep(350) // Real player progress, not the Compose animation clock.
        captureQaWindow("09_FLOW_SALUDO_NATIVO")
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        assertTrue("A nonzero resume position must be retained",positions.get()>100)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitUntil(12000){completions.get()==1 || failed.get()};assertFalse("Native video decoder failed",failed.get());assertEquals(1,completions.get())
        }finally{if(oldScale!=null && oldScale.matches(Regex("[0-9.]+")))shell("settings put global animator_duration_scale $oldScale")else shell("settings delete global animator_duration_scale")}
    }
    @Test fun decoderFailureShowsCanonicalFallbackAndContinue(){
        compose.setContent {ProfeTheme {WelcomeScreen(WelcomeState(loading=false,mode=WelcomeMode.RETURNING,videoFailed=true),{_,_->},{})}}
        compose.waitUntil(10000){compose.onAllNodesWithTag("mascot-canonical-static").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("welcome-continue").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("No se pudo reproducir el vídeo",substring=true).assertExists()
    }
}
