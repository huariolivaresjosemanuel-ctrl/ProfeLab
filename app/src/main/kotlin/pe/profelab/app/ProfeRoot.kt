package pe.profelab.app

import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pe.profelab.core.model.WelcomeMode
import pe.profelab.core.ui.*
import pe.profelab.feature.profile.WelcomeFlowModel
import pe.profelab.feature.profile.WelcomeScreen

@Composable
internal fun ProfeRoot(graph:AppGraph,welcome:WelcomeFlowModel){
    val state by welcome.state.collectAsStateWithLifecycle()
    val context=LocalContext.current
    DisposableEffect(context,welcome){
        val manager=context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val listener=AccessibilityManager.TouchExplorationStateChangeListener {enabled->welcome.accessibilityChanged(enabled)}
        welcome.accessibilityChanged(manager.isTouchExplorationEnabled);manager.addTouchExplorationStateChangeListener(listener)
        onDispose {manager.removeTouchExplorationStateChangeListener(listener)}
    }
    val incoming by graph.incomingPdf.collectAsStateWithLifecycle()
    LaunchedEffect(incoming){if(incoming!=null)welcome.externalContentRequested()}
    if(state.loading){
        Surface(Modifier.fillMaxSize(),color=ProfeColors.Cream){Box(Modifier.safeDrawingPadding(),contentAlignment=Alignment.Center){CircularProgressIndicator()}}
        return
    }
    Box(Modifier.fillMaxSize()){
        // Stable call site: closing welcome does not replace or reset the NavHost/ViewModels.
        val modal=state.startupError || state.mode!=WelcomeMode.NONE && incoming==null
        Box(if(modal)Modifier.clearAndSetSemantics {}else Modifier){ProfeApp(graph,state.prefs)}
        if(state.startupError)Surface(Modifier.fillMaxSize(),color=ProfeColors.Cream){
            Column(Modifier.safeDrawingPadding().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
                ErrorNotice(state.error.orEmpty(),welcome::retry)
                Button(onClick=welcome::dismissForSession){Text("Entrar sin modificar mi perfil")}
            }
        }else if(state.mode!=WelcomeMode.NONE && incoming==null){
            WelcomeScreen(state,welcome::continueWelcome,welcome::dismissForSession,welcome::videoPosition,welcome::videoFinished,welcome::videoFailed)
        }
    }
}
