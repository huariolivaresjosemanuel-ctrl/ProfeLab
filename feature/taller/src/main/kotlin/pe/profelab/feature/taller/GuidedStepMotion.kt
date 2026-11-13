package pe.profelab.feature.taller

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.ActivityStepView
import pe.profelab.core.model.WorkRecord

internal data class StepPresentation(val id:String,val index:Int,val count:Int,val title:String,val instruction:String)

/** Hosted outside LazyColumn so an off-screen photograph cannot leave navigation locked. */
@Composable
internal fun rememberGuidedStepMotion(step:ActivityStepView?,index:Int,count:Int,enabled:Boolean,busyChanged:(Boolean)->Unit):Transition<StepPresentation>? {
    if(step==null)return null
    val target=StepPresentation(step.id,index,count,step.title,step.instruction)
    val transition=updateTransition(target,label="workshop-step-transition")
    // Retain a clock even when the read-only image item is outside the lazy viewport.
    transition.animateFloat(transitionSpec={tween(if(enabled)WorkshopMotionRules.STEP_MS else 0)},label="workshop-step-clock"){it.index.toFloat()}
    val onBusy by rememberUpdatedState(busyChanged)
    LaunchedEffect(transition.currentState.id,transition.targetState.id,transition.isRunning,enabled){
        onBusy(enabled && (transition.isRunning || transition.currentState.id!=transition.targetState.id))
    }
    return transition
}

/** Only read-only visuals overlap. Notes, checkboxes and business effects live outside this scope. */
@Composable
internal fun GuidedStepMotion(record:WorkRecord,transition:Transition<StepPresentation>,enabled:Boolean){
    transition.AnimatedContent(transitionSpec={
        val duration=if(enabled)WorkshopMotionRules.STEP_MS else 0
        val direction=WorkshopMotionRules.stepDirection(initialState.index,targetState.index)
        ((slideInHorizontally(tween(duration)){it/5*direction}+fadeIn(tween(duration))) togetherWith
            (slideOutHorizontally(tween(duration)){-it/5*direction}+fadeOut(tween(duration)))).using(SizeTransform(clip=true,sizeAnimationSpec={_,_->tween(duration)}))
    },contentKey={it.id}){shown->
        Column(Modifier.fillMaxWidth().testTag("workshop-step-visual:${shown.id}"),verticalArrangement=Arrangement.spacedBy(12.dp)){
            WorkshopMediaSlot(record,"step_photo",stepId=shown.id,modifier=Modifier.fillMaxWidth())
            FlowCard("Paso ${shown.index+1} de ${shown.count}",WorkshopColors.Mint,Icons.Outlined.Layers){
                Text(shown.title,style=MaterialTheme.typography.titleLarge)
                if(shown.instruction.isNotBlank())Text(shown.instruction)
            }
        }
    }
}
