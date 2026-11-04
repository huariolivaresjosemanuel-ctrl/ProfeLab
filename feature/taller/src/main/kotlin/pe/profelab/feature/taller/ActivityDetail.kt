package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

internal data class ActivityActions(val navigate:(WorkshopWindow)->Unit,val begin:()->Unit,val resume:()->Unit,
    val edit:()->Unit,val completeGuide:()->Unit,val editTeacher:()->Unit,val inventory:()->Unit,val shopping:()->Unit,
    val favorite:()->Unit,val export:()->Unit,val perspective:(WorkshopPerspective)->Unit)

@Composable
internal fun BriefText(text:String){
    var expanded by rememberSaveable(text){mutableStateOf(false)}
    Text(text,maxLines=if(expanded)Int.MAX_VALUE else 4,overflow=TextOverflow.Ellipsis)
    if(text.length>240)TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Ver menos"else "Leer completo")}
}
@Composable
internal fun ActivityDetail(record:WorkRecord,all:List<WorkRecord>,catalog:List<MaterialDefinition>,view:ActivityViewState,mode:WorkshopPerspective,back:()->Unit,actions:ActivityActions){
    val j=JSONObject(record.json);val a=WorkshopCodec.assess(record,all,catalog)
    WorkshopPage(if(view.experiment)"Experimentos"else "Taller",if(view.experiment)"Investiga y experimenta"else "Materiales didácticos",back,screenTag="workshop-guide:${record.id}"){
        item("cover"){Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
            WorkshopMediaSlot(record,"detail_hero",modifier=Modifier.fillMaxWidth())
        }}
        item("perspective"){PerspectiveBar(mode,actions.perspective)}
        item("title"){Text(record.title.ifBlank {"Mi proyecto"},style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold,color=WorkshopColors.Deep);Text(view.summary)}
        item("purpose"){FlowCard("Propósito breve",icon=Icons.Outlined.MyLocation){Text(when(mode){WorkshopPerspective.TEACHER->TeacherGuides.resolve(record).optString("purpose");WorkshopPerspective.ACADEMIC->j.optString("academicFoundation").ifBlank {"Consulta los fundamentos, fuentes y límites de esta propuesta editorial."};else->j.optJSONObject("pedagogy")?.optJSONObject("practical")?.optString("purpose").orEmpty().ifBlank {view.summary}})}}
        item("metadata"){FlowMetadata(j)}
        if(view.personal)item("progress"){FlowCard {Text("${view.completedSteps}/${view.steps.size} pasos completados");LinearProgressIndicator(progress={view.progress},modifier=Modifier.fillMaxWidth())}}
        item("begin"){if(view.personal)FlowAction("Continuar","workshop-begin",actions.resume)else FlowAction("Ver materiales","workshop-to-materials",{actions.navigate(WorkshopWindow.MATERIALS)})}
        item("teacher"){FlowAction("Ver guía docente","workshop-to-classroom",{actions.navigate(WorkshopWindow.CLASSROOM)},false)}
        item("shortcuts"){FlowCard("Explora la actividad",WorkshopColors.Paper){if(view.personal)FlowAction("Materiales y herramientas","workshop-to-materials",{actions.navigate(WorkshopWindow.MATERIALS)},false);FlowAction("Ver pasos","workshop-to-steps",{actions.navigate(WorkshopWindow.STEPS)},false);FlowAction("Resultados y reflexión","workshop-to-results",{actions.navigate(WorkshopWindow.RESULTS)},false);FlowAction("Más información pedagógica","workshop-to-purpose",{actions.navigate(WorkshopWindow.PURPOSE)},false)}}
        if(mode==WorkshopPerspective.TEACHER)item("teacher-preview"){FlowCard("Docente · Cómo enseñarlo",WorkshopColors.Blue){BriefText(TeacherGuides.resolve(record).optString("introShort"));TextButton(onClick={actions.navigate(WorkshopWindow.CLASSROOM)}){Text("Presentación en clase y evaluación")}}}
        if(mode==WorkshopPerspective.ACADEMIC)item("academic"){AcademicBlocks(j)}
        item("safety"){FlowCard("Antes de comenzar",WorkshopColors.Peach,Icons.Outlined.Info){Text(a.safety.label);WorkshopCodec.strings(j.optJSONObject("safety")?.opt("warnings")).forEach {Text(it)};Text("Propuesta editorial: revisa la adecuación al grupo. Los datos de tiempo y grado son sugeridos, no una correspondencia curricular acreditada.",style=MaterialTheme.typography.bodySmall)}}
        item("options"){FlowCard("Más opciones",WorkshopColors.Paper){if(!view.personal)FlowAction("Comenzar mi copia","workshop-begin",actions.begin,false,enabled=a.safety!=SafetyStatus.FORBIDDEN);TextButton(onClick=actions.favorite){Text(if(all.any {it.id=="favorite:${record.id}" && !it.archived})"Quitar favorito"else "Guardar favorito")};if(view.personal){TextButton(onClick=actions.edit){Text("Editar mi proyecto")};TextButton(onClick=actions.export){Text("Vista previa / exportar PDF")}};TextButton(onClick=actions.completeGuide){Text("Contenido completo, alternativas y evidencias")}}}
    }
}
