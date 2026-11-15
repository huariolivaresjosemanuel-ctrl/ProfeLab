package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@Composable
internal fun TeacherClassroomPanel(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions,observe:()->Unit={}){
    val guide=TeacherGuides.resolve(record);val flow=JSONObject(record.json).optJSONObject("workshopFlow") ?: JSONObject()
    val observed=WorkshopCodec.strings(flow.opt("observedCriterionIds")).toSet()
    var section by rememberSaveable(record.id){mutableStateOf("Presentación")}
    WorkshopPage("Guía docente",record.title.ifBlank {"Cómo explicarlo y qué observar"},back,screenTag="workshop-classroom"){
        item("journey"){WorkshopJourney(if(section=="Evaluación")2 else 0)}
        item("sections"){WorkshopChoices(listOf("Presentación","Evaluación","Adaptación"),section){section=it}}
        if(section=="Presentación"){
        item("script"){FlowCard("Guion breve",WorkshopColors.Mint,Icons.Outlined.MenuBook){BriefText(guide.optString("introShort").ifBlank {"Guion vacío por elección del usuario. Puedes editarlo en tu copia."})}}
        item("questions"){FlowCard("Preguntas para el aula",WorkshopColors.Blue,Icons.Outlined.QuestionAnswer){(WorkshopCodec.strings(guide.opt("priorKnowledgeQuestions"))+WorkshopCodec.strings(guide.opt("reflectiveQuestions"))).distinct().take(3).forEachIndexed {i,q->Text("${i+1}. $q")}}}
        item("observe-hint"){FlowCard("Qué observar",WorkshopColors.Paper){view.criteria.take(3).forEach {Text("• ${it.label}")};Text("Observa hechos; no se generan calificaciones automáticas.",style=MaterialTheme.typography.bodySmall)}}
        item("sequence"){TeacherPresentationCard(record,guide)}
        item("expanded-guide"){OptionalWorkshopSection("workshop-expanded-guide","Guion ampliado, preguntas y fuentes"){
            (WorkshopCodec.strings(guide.opt("priorKnowledgeQuestions"))+WorkshopCodec.strings(guide.opt("reflectiveQuestions"))).distinct().forEach {Text(it)}
            TextButton(onClick=if(view.personal)actions.editTeacher else actions.begin){Text(if(view.personal)"Editar mi guion docente"else "Crear mi guion docente")}
            TextButton(onClick=actions.completeGuide){Text("Consultar guía completa y fuentes")}
        }}
        }
        if(section=="Evaluación"){
        item("formative"){FlowCard("Evaluación formativa simple",WorkshopColors.Peach,Icons.Outlined.FactCheck){
            Text("Estos checks describen la actividad y no generan notas. Las observaciones individuales se guardan en el panel privado de alumnos.",style=MaterialTheme.typography.bodySmall)
            view.criteria.take(3).forEach {criterion->Text((if(criterion.id in observed)"Observado: "else "Sin registro general: ")+criterion.label)}
            flow.optString("teacherReflection").takeIf {it.isNotBlank()}?.let {BriefText(it)}
            if(view.personal)FlowAction("Registrar observación","workshop-classroom-observe",observe)
            else Text("Comienza una copia para guardar observaciones docentes.")
            flow.optJSONObject("criteriaById")?.let {previous->previous.keys().asSequence().filter {id->view.criteria.none {it.id==id}}.forEach {id->Text("Criterio anterior conservado: ${previous.getJSONObject(id).optString("label")}",style=MaterialTheme.typography.bodySmall)}}
        }}
        }
        if(section=="Adaptación"){
        item("adaptations"){FlowCard("Adaptaciones y seguridad",WorkshopColors.Yellow,Icons.Outlined.Shield){BriefText(guide.optString("inclusion"));BriefText(guide.optString("safety"));val j=JSONObject(record.json);WorkshopCodec.strings(j.optJSONObject("safety")?.opt("childrenForbiddenActions")).forEach {Text("Solo adulto: $it")};TextButton(onClick=actions.completeGuide){Text("Ver guía completa, variantes y fuentes")}}}
        }
        item("classroom-followup"){FlowCard("Seguimiento privado",WorkshopColors.Mint,Icons.Outlined.EventNote){Text("Puedes guardar observaciones generales sin registrar alumnos. Con una ejecución seleccionada, las observaciones corresponden solo al alumno, equipo o actividad elegidos.");if(view.personal)TextButton(onClick=observe){Text("Observar y evaluar / revisar registros")}else Text("Comienza una copia para guardar observaciones; las aulas son opcionales y se administran desde Cuaderno → Mis alumnos.")}}
        if(view.personal)item("export"){FlowAction("Vista previa / exportar PDF","workshop-classroom-export",actions.export,false)}
        item("finish"){FlowAction("Comenzar procedimiento","workshop-classroom-procedure",{actions.navigate(WorkshopWindow.STEPS)},section!="Evaluación");FlowAction("Volver a la ficha","workshop-classroom-finish",{actions.navigate(WorkshopWindow.DETAIL)},false);FlowAction("Ver resultados y reflexión","workshop-classroom-results",{actions.navigate(WorkshopWindow.RESULTS)},false);FlowAction("Revisar materiales y uso real","workshop-classroom-materials",{actions.navigate(WorkshopWindow.MATERIALS)},false);FlowAction("Perspectivas y fundamentos","workshop-classroom-purpose",{actions.navigate(WorkshopWindow.PURPOSE)},false)}
    }
}
