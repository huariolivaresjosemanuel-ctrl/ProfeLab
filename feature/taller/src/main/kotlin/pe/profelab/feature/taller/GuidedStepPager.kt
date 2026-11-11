package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@Composable
internal fun GuidedStepPager(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions){
    val data=JSONObject(record.json)
    var list by rememberSaveable(record.id){mutableStateOf(false)}
    var inquiry by rememberSaveable(record.id){mutableStateOf(false)}
    var selectedId by rememberSaveable(record.id){mutableStateOf(view.steps.getOrNull(view.resumeIndex)?.id)}
    val index=view.steps.indexOfFirst {it.id==selectedId}.takeIf {it>=0} ?: view.resumeIndex
    fun visit(i:Int){view.steps.getOrNull(i)?.let {selectedId=it.id;if(view.personal)vm.visitStep(record,it.id)}}
    LaunchedEffect(record.id){if(view.personal && view.steps.isNotEmpty())vm.visitStep(record,view.steps[index].id)}
    WorkshopPage("Procedimiento guiado",if(view.experiment)"Sigue la investigación paso a paso"else "Realiza tu actividad paso a paso",back,screenTag="workshop-steps"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("step-selector"){GuidedStepSelector(view.steps.size,index,::visit)}
        if(list){
            item("list-heading"){Text("Todos los pasos",style=MaterialTheme.typography.titleLarge)}
            view.steps.forEachIndexed {i,step->item("step:${step.id}"){
                FlowCard("${i+1}. ${step.title}",WorkshopColors.Paper){if(step.instruction.isNotBlank())Text(step.instruction);StepCompletion(record,step,i,view.personal,vm);TextButton(onClick={visit(i);list=false}){Text("Ver este paso ilustrado")}}
            }}
        }else view.steps.getOrNull(index)?.let {step->
            item("step-image"){WorkshopMediaSlot(record,"step_photo",stepId=step.id,modifier=Modifier.fillMaxWidth())}
            item("steps"){FlowCard("Paso ${index+1} de ${view.steps.size}",WorkshopColors.Mint,Icons.Outlined.Layers){
                Text(step.title,style=MaterialTheme.typography.titleLarge)
                if(step.instruction.isNotBlank())Text(step.instruction)
                StepCompletion(record,step,index,view.personal,vm)
                if(view.personal)OutlinedTextField(step.notes,{value->vm.stepNote(record,step.id,value.take(4000))},label={Text("Mi observación de este paso")},modifier=Modifier.fillMaxWidth(),minLines=2)
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
            item("navigation"){GuidedStepNavigation(index,view.steps.lastIndex,{visit(index-1)},{if(index<view.steps.lastIndex)visit(index+1)else actions.navigate(WorkshopWindow.RESULTS)})}
        }
        if(view.steps.isEmpty())item("empty"){FlowCard("Sin pasos definidos"){Text("Puedes consultar la ficha y los resultados, o añadir pasos en el editor de tu copia.");FlowAction("Resultados y reflexión","workshop-empty-steps-results",{actions.navigate(WorkshopWindow.RESULTS)},false)}}
        item("view"){FlowAction(if(list)"Volver a vista guiada"else "Ver todos los pasos (${view.steps.size})","workshop-step-view",{list=!list},false)}
        item("progress"){LinearProgressIndicator(progress={view.progress},modifier=Modifier.fillMaxWidth());Text("${view.completedSteps}/${view.steps.size} completados · navegar no marca pasos",style=MaterialTheme.typography.bodySmall)}
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
private fun StepCompletion(record:WorkRecord,step:ActivityStepView,index:Int,personal:Boolean,vm:WorkshopViewModel){
    Row {Checkbox(step.done,{vm.completeStep(record,step.id,it)},enabled=personal,modifier=Modifier.testTag("workshop-step-$index"));Text(if(personal)"He completado este paso"else "Consulta libre · comienza una copia para guardar",Modifier.weight(1f))}
}
