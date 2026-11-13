package pe.profelab.feature.taller

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.WorkshopPerspective

/** Public pure rules are shared by motion and its instrumented contract tests. */
object WorkshopMotionRules {
    const val MODULE_MS=250
    const val WINDOW_MS=220
    const val STEP_MS=240
    const val PROGRESS_MS=180
    const val PERSPECTIVE_MS=160
    fun allowed(animatorScale:Float,transitionScale:Float,touchExploration:Boolean)=animatorScale>0f && transitionScale>0f && !touchExploration
    fun stepDirection(from:Int,to:Int)=when {to>from->1;to<from->-1;else->0}
}

internal val LocalWorkshopMotionEnabled=staticCompositionLocalOf {false}
internal val LocalWorkshopTransitionBusy=staticCompositionLocalOf {false}

@Composable
internal fun workshopMotionEnabled()=LocalWorkshopMotionEnabled.current

/** No app preference or perpetual animation: Android's controls remain authoritative. */
@Composable
internal fun rememberWorkshopMotionEnabled():Boolean {
    val context=LocalContext.current
    val manager=remember(context){context.getSystemService(AccessibilityManager::class.java)}
    fun read():Boolean=runCatching {
        WorkshopMotionRules.allowed(Settings.Global.getFloat(context.contentResolver,Settings.Global.ANIMATOR_DURATION_SCALE,1f),
            Settings.Global.getFloat(context.contentResolver,Settings.Global.TRANSITION_ANIMATION_SCALE,1f),manager?.isTouchExplorationEnabled==true) && ValueAnimator.areAnimatorsEnabled()
    }.getOrDefault(false)
    var enabled by remember(context){mutableStateOf(read())}
    DisposableEffect(context,manager){
        val observer=object:ContentObserver(Handler(Looper.getMainLooper())){override fun onChange(selfChange:Boolean){enabled=read()}}
        val listener=AccessibilityManager.TouchExplorationStateChangeListener {enabled=read()}
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),false,observer)
        context.contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.TRANSITION_ANIMATION_SCALE),false,observer)
        manager?.addTouchExplorationStateChangeListener(listener)
        onDispose {context.contentResolver.unregisterContentObserver(observer);manager?.removeTouchExplorationStateChangeListener(listener)}
    }
    return enabled
}

/** Exactly one screen subtree. Outgoing editors/ViewModels are never composed a second time. */
@Composable
internal fun WorkshopRouteMotion(routeKey:String,destination:String,initial:Boolean,backward:Boolean,enabled:Boolean,onFinished:()->Unit,content:@Composable ()->Unit){
    val phase=remember(routeKey){Animatable(if(enabled)1f else 0f)}
    val finish by rememberUpdatedState(onFinished)
    val offset=with(LocalDensity.current){if(initial)12.dp.toPx()else 20.dp.toPx()}
    LaunchedEffect(routeKey,enabled){
        if(enabled)phase.animateTo(0f,tween(if(initial)WorkshopMotionRules.MODULE_MS else WorkshopMotionRules.WINDOW_MS))else phase.snapTo(0f)
        finish()
    }
    Box(Modifier.fillMaxSize().clipToBounds().testTag(if(phase.value>0f)"workshop-motion-busy"else "workshop-motion-idle").graphicsLayer {
        alpha=1f-phase.value
        translationY=if(initial)offset*phase.value else 0f
        translationX=if(!initial && destination in setOf("catalog","detail","materials","steps","results","classroom","purpose"))offset*phase.value*(if(backward)-1 else 1)else 0f
    }){
        content()
        if(phase.value>0f)Box(Modifier.matchParentSize().pointerInput(routeKey){
            awaitPointerEventScope {while(true){val event=awaitPointerEvent();event.changes.forEach {it.consume()}}}
        })
    }
}

@Composable
internal fun WorkshopProgressIndicator(progress:Float,modifier:Modifier=Modifier){
    val animated by animateFloatAsState(progress.coerceIn(0f,1f),tween(if(workshopMotionEnabled())WorkshopMotionRules.PROGRESS_MS else 0),label="workshop-progress")
    LinearProgressIndicator(progress={animated},modifier=modifier)
}

/** Content passed here is presentation only; editable fields and VM effects stay outside. */
@Composable
internal fun WorkshopPerspectiveMotion(mode:WorkshopPerspective,content:@Composable (WorkshopPerspective)->Unit){
    Crossfade(targetState=mode,animationSpec=tween(if(workshopMotionEnabled())WorkshopMotionRules.PERSPECTIVE_MS else 0),label="workshop-perspective",content=content)
}
