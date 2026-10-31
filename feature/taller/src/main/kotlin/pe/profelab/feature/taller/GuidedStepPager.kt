package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@Composable
internal fun GuidedStepPager(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions){
    val j=JSONObject(record.json)
    var list by rememberSaveable(record.id){mutableStateOf(false)}
    var selectedId by rememberSaveable(record.id){mutableStateOf(view.steps.getOrNull(view.resumeIndex)?.id)}
    val index=view.steps.indexOfFirst {it.id==selectedId}.takeIf {it>=0} ?: view.resumeIndex
    fun visit(i:Int){view.steps.getOrNull(i)?.let {selectedId=it.id;if(view.personal)vm.visitStep(record,it.id)}}
    LaunchedEffect(record.id){if(view.personal && view.steps.isNotEmpty())vm.visitStep(record,view.steps[index].id)}
    WorkshopPage("Paso a paso",if(view.experiment)"Pregunta, prueba y registra lo que realmente observas"else "Construye tu material con calma",back,screenTag="workshop-steps"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("view"){WorkshopChoices(listOf("Vista guiada","Ver todos los pasos"),if(list)"Ver todos los pasos"else "Vista guiada"){list=it!="Vista guiada"}}
        if(view.experiment)item("inquiry"){FlowCard("Pregunta e hipótesis",WorkshopColors.Yellow,Icons.Outlined.Science){Text(j.optString("researchQuestion").ifBlank {"Define tu pregunta en el editor."});Text("Tu hipótesis: ${j.optString("hypothesis").ifBlank {"sin registrar"}}");Text("Variable a cambiar: ${j.optString("independentVariable").ifBlank {"por definir"}}");Text("Qué observar: ${j.optString("dependentVariable").ifBlank {"por definir"}}");Text("Mantener: ${WorkshopCodec.text(j.opt("controlConditions")).ifBlank {j.optString("controls")}}");if(view.personal)TextButton(onClick=actions.edit){Text("Editar hipótesis y variables")}}}
        item("progress"){Text(if(view.steps.isEmpty())"Sin pasos definidos"else "Paso ${index+1} de ${view.steps.size}",style=MaterialTheme.typography.titleLarge);LinearProgressIndicator(progress={view.progress},modifier=Modifier.fillMaxWidth());Text("${view.completedSteps} completados · navegar no marca pasos",style=MaterialTheme.typography.bodySmall)}
        if(list){view.steps.forEachIndexed {i,step->item("step:${step.id}"){
            FlowCard("${i+1}. ${step.title}",WorkshopColors.Paper){StepCompletion(record,step,i,view.personal,vm);TextButton(onClick={visit(i);list=false}){Text("Ver este paso ilustrado")}}
        }}}
        else view.steps.getOrNull(index)?.let {step->
            item("step-image"){WorkshopPicture(step.imageFileIds.firstOrNull()?.let {installedWorkshopImage(j,it)},Modifier.fillMaxWidth().height(240.dp),large=true,missingMessage="Imagen específica de este paso pendiente. Sigue la instrucción escrita; la portada no representa el procedimiento.")}
            item("steps"){FlowCard("${index+1}. ${step.title}",WorkshopColors.Mint,Icons.Outlined.Layers){if(step.instruction.isNotBlank())Text(step.instruction);StepCompletion(record,step,index,view.personal,vm)
                if(view.personal)OutlinedTextField(step.notes,{value->vm.stepNote(record,step.id,value.take(4000))},label={Text("Mi observación de este paso")},modifier=Modifier.fillMaxWidth(),minLines=2)
                if(step.imageFileIds.size>1)step.imageFileIds.drop(1).forEach {WorkshopPicture(installedWorkshopImage(j,it),Modifier.fillMaxWidth().height(180.dp))}
            }}
            item("safety"){FlowCard("Seguridad",WorkshopColors.Yellow,Icons.Outlined.Shield){val warnings=WorkshopCodec.strings(j.optJSONObject("safety")?.opt("warnings"));if(warnings.isEmpty())Text("Revisa riesgos, preparación y adecuación al grupo antes de realizar el paso.")else warnings.forEach {Text(it)};WorkshopCodec.strings(j.optJSONObject("safety")?.opt("childrenForbiddenActions")).forEach {Text("Solo adulto: $it")}}}
            item("step-selector"){WorkshopChoices(view.steps.indices.map {"Paso ${it+1}"},"Paso ${index+1}"){visit(it.removePrefix("Paso ").toInt()-1)}}
            item("navigation"){FlowAction("Anterior","workshop-step-previous",{visit(index-1)},false,enabled=index>0);FlowAction(if(index<view.steps.lastIndex)"Siguiente"else "Presentación en clase","workshop-step-next",{if(index<view.steps.lastIndex)visit(index+1)else actions.navigate(WorkshopWindow.CLASSROOM)})}
        }
        if(!view.personal)item("begin"){FlowAction("Comenzar mi proyecto para guardar avances","workshop-steps-begin",actions.begin,false)}
        if(view.personal && view.completedSteps==view.steps.size && view.steps.isNotEmpty())item("consumption"){FlowCard("Pasos completados",WorkshopColors.Mint){Text("El inventario sigue intacto. Si utilizaste consumibles, revisa y confirma las cantidades reales.");FlowAction("Revisar uso real de materiales","workshop-review-actual-use",{actions.navigate(WorkshopWindow.MATERIALS)},false)}}
        if(view.experiment)item("evidence"){FlowCard("Observación y explicación",WorkshopColors.Blue){Text("Registra resultados propios; no se completan a partir de una imagen o una hipótesis.");Text("Resultados: ${j.optString("results").ifBlank {"sin registrar"}}");Text("Limitaciones: ${j.optString("limitations").ifBlank {"sin registrar"}}");Text("Conclusión: ${j.optString("conclusion").ifBlank {"sin registrar"}}");TextButton(onClick=actions.completeGuide){Text("Registros, fotografías y seguimiento")};if(view.personal)TextButton(onClick=actions.edit){Text("Escribir mi análisis")}}}
    }
}
@Composable
private fun StepCompletion(record:WorkRecord,step:ActivityStepView,index:Int,personal:Boolean,vm:WorkshopViewModel){
    Row {Checkbox(step.done,{vm.completeStep(record,step.id,it)},enabled=personal,modifier=Modifier.testTag("workshop-step-$index"));Text(if(personal)"He completado este paso"else "Consulta libre · comienza una copia para guardar",Modifier.weight(1f))}
}
