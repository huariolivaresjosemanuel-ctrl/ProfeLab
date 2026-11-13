package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@Composable
internal fun GuidedStepPager(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions,observe:(String?)->Unit={},visibleStep:(String?)->Unit={}){
    val data=JSONObject(record.json)
    var list by rememberSaveable(record.id){mutableStateOf(false)}
    var inquiry by rememberSaveable(record.id){mutableStateOf(false)}
    var selectedId by rememberSaveable(record.id){mutableStateOf(view.steps.getOrNull(view.resumeIndex)?.id)}
    val motionEnabled=workshopMotionEnabled()
    var stepBusy by remember(record.id){mutableStateOf(false)}
    val index=view.steps.indexOfFirst {it.id==selectedId}.takeIf {it>=0} ?: view.resumeIndex
    LaunchedEffect(record.id,index){visibleStep(view.steps.getOrNull(index)?.id)}
    val stepTransition=rememberGuidedStepMotion(view.steps.getOrNull(index),index,view.steps.size,motionEnabled){stepBusy=it}
    fun visit(i:Int){if(stepBusy)return;view.steps.getOrNull(i)?.takeIf {it.id!=view.steps.getOrNull(index)?.id}?.let {stepBusy=motionEnabled;selectedId=it.id;if(view.personal)vm.visitStep(record,it.id)}}
    LaunchedEffect(record.id){if(view.personal && view.steps.isNotEmpty() && view.lastVisitedStepId!=view.steps[index].id)vm.visitStep(record,view.steps[index].id)}
    WorkshopPage("Procedimiento guiado",if(view.experiment)"Sigue la investigación paso a paso"else "Realiza tu actividad paso a paso",back,screenTag="workshop-steps"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("teaching-help"){Row {TextButton(onClick={actions.navigate(WorkshopWindow.CLASSROOM)}){Text("Guía docente")};if(view.personal)TextButton(onClick={observe(view.steps.getOrNull(index)?.id)}){Text("Observar alumnos")}}}
        item("step-selector"){GuidedStepSelector(view.steps.size,index,::visit,enabled=!stepBusy)}
        if(list){
            item("list-heading"){Text("Todos los pasos",style=MaterialTheme.typography.titleLarge)}
            view.steps.forEachIndexed {i,step->item("step:${step.id}"){
                FlowCard("${i+1}. ${step.title}",WorkshopColors.Paper){if(step.instruction.isNotBlank())Text(step.instruction);StepCompletion(record,step,i,view.personal,vm);TextButton(onClick={visit(i);list=false}){Text("Ver este paso ilustrado")}}
            }}
        }else view.steps.getOrNull(index)?.let {step->
            item("step-image"){stepTransition?.let {GuidedStepMotion(record,it,motionEnabled)}}
            item("steps"){FlowCard(color=WorkshopColors.Mint){
                StepCompletion(record,step,index,view.personal,vm,enabled=!stepBusy)
                if(view.personal)OutlinedTextField(step.notes,{value->if(!stepBusy)vm.stepNote(record,step.id,value.take(4000))},enabled=!stepBusy,label={Text("Mi observación de este paso")},modifier=Modifier.fillMaxWidth(),minLines=2)
            }}
            if(step.imageFileIds.size>1)item("step-gallery:${step.id}"){
                var extra by rememberSaveable(record.id,step.id){mutableStateOf(false)}
                TextButton(onClick={extra=!extra}){Text(if(extra)"Ocultar otras imágenes del paso"else "Más imágenes de este paso")}
                if(extra)step.imageFileIds.drop(1).forEach {id->WorkshopPicture(installedWorkshopImage(data,id),Modifier.fillMaxWidth().aspectRatio(16f/9f))}
            }
            item("safety"){FlowCard("Seguridad",WorkshopColors.Yellow,Icons.Outlined.Shield){
                val warnings=WorkshopCodec.strings(data.optJSONObject("safety")?.opt("warnings"));if(warnings.isEmpty())Text("Revisa riesgos, preparación y adecuación al grupo antes de realizar el paso.")else warnings.forEach {Text(it)}
                WorkshopCodec.strings(data.optJSONObject("safety")?.opt("childrenForbiddenActions")).forEach {Text("Solo adulto: $it")}
            }}
            item("navigation"){GuidedStepNavigation(index,view.steps.lastIndex,{visit(index-1)},{if(index<view.steps.lastIndex)visit(index+1)else actions.navigate(WorkshopWindow.RESULTS)},enabled=!stepBusy)}
        }
        if(view.steps.isEmpty())item("empty"){FlowCard("Sin pasos definidos"){Text("Puedes consultar la ficha y los resultados, o añadir pasos en el editor de tu copia.");FlowAction("Resultados y reflexión","workshop-empty-steps-results",{actions.navigate(WorkshopWindow.RESULTS)},false)}}
        item("view"){FlowAction(if(list)"Volver a vista guiada"else "Ver todos los pasos (${view.steps.size})","workshop-step-view",{list=!list},false,enabled=!stepBusy)}
        item("progress"){WorkshopProgressIndicator(view.progress,Modifier.fillMaxWidth());Text("${view.completedSteps}/${view.steps.size} completados · navegar no marca pasos",style=MaterialTheme.typography.bodySmall)}
        if(view.experiment)item("inquiry"){
            TextButton(onClick={inquiry=!inquiry}){Text(if(inquiry)"Ocultar pregunta y variables"else "Consultar pregunta, hipótesis y variables")}
            if(inquiry)FlowCard("Pregunta e hipótesis",WorkshopColors.Blue,Icons.Outlined.Science){
                Text(data.optString("researchQuestion").ifBlank {"Define tu pregunta en el editor."});Text("Tu hipótesis: ${data.optString("hypothesis").ifBlank {"sin registrar"}}")
                Text("Variable a cambiar: ${data.optString("independentVariable").ifBlank {"por definir"}}");Text("Qué observar: ${data.optString("dependentVariable").ifBlank {"por definir"}}")
                Text("Mantener: ${WorkshopCodec.text(data.opt("controlConditions")).ifBlank {data.optString("controls")}}")
                if(view.personal)TextButton(onClick=actions.edit){Text("Editar hipótesis y variables")}
            }
        }
        if(!view.personal)item("begin"){FlowAction("Comenzar mi proyecto para guardar avances","workshop-steps-begin",actions.begin,false)}
        if(view.personal && view.completedSteps==view.steps.size && view.steps.isNotEmpty())item("consumption"){FlowCard("Pasos completados",WorkshopColors.Mint){Text("El inventario sigue intacto. Si utilizaste consumibles, revisa y confirma las cantidades reales.");FlowAction("Revisar uso real de materiales","workshop-review-actual-use",{actions.navigate(WorkshopWindow.MATERIALS)},false)}}
        item("results-shortcut"){FlowAction("Abrir resultados y evidencias","workshop-steps-results",{actions.navigate(WorkshopWindow.RESULTS)},false)}
    }
}
@Composable
private fun StepCompletion(record:WorkRecord,step:ActivityStepView,index:Int,personal:Boolean,vm:WorkshopViewModel,enabled:Boolean=true){
    val scale by animateFloatAsState(if(step.done)1.06f else 1f,tween(if(workshopMotionEnabled())WorkshopMotionRules.PROGRESS_MS else 0),label="workshop-step-check")
    Row {Checkbox(step.done,{if(enabled)vm.completeStep(record,step.id,it)},enabled=personal && enabled,modifier=Modifier.testTag("workshop-step-$index").graphicsLayer {scaleX=scale;scaleY=scale});Text(if(personal)"He completado este paso"else "Consulta libre · comienza una copia para guardar",Modifier.weight(1f))}
}
