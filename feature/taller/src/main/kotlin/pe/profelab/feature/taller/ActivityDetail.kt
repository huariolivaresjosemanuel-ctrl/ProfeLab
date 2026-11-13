package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
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
        item("purpose"){
            WorkshopPerspectiveMotion(mode){shown->Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
                FlowCard("Propósito breve",icon=Icons.Outlined.MyLocation){Text(when(shown){WorkshopPerspective.TEACHER->TeacherGuides.resolve(record).optString("purpose");WorkshopPerspective.ACADEMIC->j.optString("academicFoundation").ifBlank {"Consulta los fundamentos, fuentes y límites de esta propuesta editorial."};else->j.optJSONObject("pedagogy")?.optJSONObject("practical")?.optString("purpose").orEmpty().ifBlank {view.summary}})}
                if(shown==WorkshopPerspective.TEACHER)FlowCard("Docente · Cómo enseñarlo",WorkshopColors.Blue){BriefText(TeacherGuides.resolve(record).optString("introShort"))}
                if(shown==WorkshopPerspective.ACADEMIC)AcademicBlocks(j)
            }}
            if(mode==WorkshopPerspective.TEACHER)TextButton(onClick={actions.navigate(WorkshopWindow.CLASSROOM)}){Text("Presentación en clase y evaluación")}
        }
        item("metadata"){FlowMetadata(j)}
        if(view.personal)item("progress"){FlowCard {Text("${view.completedSteps}/${view.steps.size} pasos completados");WorkshopProgressIndicator(view.progress,Modifier.fillMaxWidth())}}
        item("begin"){if(view.personal)FlowAction("Continuar actividad","workshop-begin",actions.resume)else FlowAction("Preparar actividad","workshop-to-materials",{actions.navigate(WorkshopWindow.MATERIALS)})}
        item("teacher"){TextButton(onClick={actions.navigate(WorkshopWindow.CLASSROOM)},modifier=Modifier.testTag("workshop-to-classroom")){Text("Consultar guía docente")}}
        item("shortcuts"){FlowCard("Consultar",WorkshopColors.Paper){if(view.personal)TextButton(onClick={actions.navigate(WorkshopWindow.MATERIALS)},modifier=Modifier.testTag("workshop-to-materials")){Text("Materiales y preparación")};TextButton(onClick={actions.navigate(WorkshopWindow.STEPS)},modifier=Modifier.testTag("workshop-to-steps")){Text("Procedimiento")};TextButton(onClick={actions.navigate(WorkshopWindow.RESULTS)},modifier=Modifier.testTag("workshop-to-results")){Text("Resultados y reflexión")};TextButton(onClick={actions.navigate(WorkshopWindow.PURPOSE)},modifier=Modifier.testTag("workshop-to-purpose")){Text("Fundamentos ampliados")}}}
        item("safety"){FlowCard("Antes de comenzar",WorkshopColors.Peach,Icons.Outlined.Info){Text(a.safety.label);WorkshopCodec.strings(j.optJSONObject("safety")?.opt("warnings")).forEach {Text(it)};Text("Propuesta editorial: revisa la adecuación al grupo. Los datos de tiempo y grado son sugeridos, no una correspondencia curricular acreditada.",style=MaterialTheme.typography.bodySmall)}}
        item("options"){FlowCard("Más opciones",WorkshopColors.Paper){if(!view.personal)TextButton(onClick=actions.begin,enabled=a.safety!=SafetyStatus.FORBIDDEN,modifier=Modifier.testTag("workshop-begin")){Text("Comenzar mi copia")};TextButton(onClick=actions.favorite){Text(if(all.any {it.id=="favorite:${record.id}" && !it.archived})"Quitar favorito"else "Guardar favorito")};if(view.personal){TextButton(onClick=actions.edit){Text("Editar mi proyecto")};TextButton(onClick=actions.export){Text("Vista previa / exportar PDF")}};TextButton(onClick=actions.completeGuide){Text("Contenido completo, alternativas y evidencias")}}}
    }
}
