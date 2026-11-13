package pe.profelab.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Logical intent is independent from art availability; unsupported states remain canonical static. */
@Stable
class AssistantMascotController {
    var state by mutableStateOf(MascotGesture.IDLE)
        private set
    fun rest(){state=MascotGesture.IDLE}
    fun greet(){state=MascotGesture.WAVE}
    fun explain(){state=MascotGesture.EXPLAIN}
    fun celebrate(){state=MascotGesture.CELEBRATE}
    fun displayedGesture()=state.takeIf {MascotCapabilities.supported(it)} ?: MascotGesture.IDLE
}

@Composable
fun rememberAssistantMascotController()=remember {AssistantMascotController()}

/** Place in a reserved trailing inset, so it never covers a page's primary button. */
@Composable
fun FloatingAssistant(visible:Boolean,controller:AssistantMascotController,reduceMotion:Boolean,onOpen:()->Unit,modifier:Modifier=Modifier){
    if(!visible || !LocalWindowInfo.current.isWindowFocused)return
    Surface(onClick=onOpen,modifier=modifier.size(64.dp).testTag("assistant-open").semantics {contentDescription="Abrir ayuda de ProfeLab"},
        shape=CircleShape,color=ProfeColors.Mint,shadowElevation=3.dp){
        CanonicalMascot(Modifier.fillMaxSize().padding(4.dp),reduceMotion=reduceMotion,gesture=controller.displayedGesture())
    }
}
