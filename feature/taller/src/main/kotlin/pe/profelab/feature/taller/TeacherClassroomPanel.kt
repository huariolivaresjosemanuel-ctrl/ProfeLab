package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@Composable
internal fun TeacherClassroomPanel(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions){
    val guide=TeacherGuides.resolve(record);val flow=JSONObject(record.json).optJSONObject("workshopFlow") ?: JSONObject()
    val observed=WorkshopCodec.strings(flow.opt("observedCriterionIds")).toSet()
    WorkshopPage("Presentación en clase","Cómo explicarlo y qué observar",back,screenTag="workshop-classroom"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("script"){FlowCard("Guion breve",WorkshopColors.Paper,Icons.Outlined.MenuBook){BriefText(guide.optString("introShort").ifBlank {"Guion vacío por elección del usuario. Puedes editarlo en tu copia."});TextButton(onClick=if(view.personal)actions.editTeacher else actions.begin){Text(if(view.personal)"Editar mi guion docente"else "Crear mi guion docente")}}}
        item("sequence"){FlowCard("Inicio, desarrollo y cierre",WorkshopColors.Mint){Text("Inicio: ${guide.optString("motivation")}");WorkshopCodec.strings(guide.opt("teachingSequence")).forEachIndexed {i,line->Text("${i+1}. $line")};Text("Cierre: ${guide.optString("close")}")}}
        item("questions"){FlowCard("Preguntas para el aula",WorkshopColors.Blue,Icons.Outlined.QuestionAnswer){(WorkshopCodec.strings(guide.opt("priorKnowledgeQuestions"))+WorkshopCodec.strings(guide.opt("reflectiveQuestions"))).distinct().forEachIndexed {i,q->Text("${i+1}. $q")}}}
        item("formative"){FlowCard("Evaluación formativa simple",WorkshopColors.Peach,Icons.Outlined.FactCheck){
            Text("Observación de la actividad, sin notas automáticas ni evaluación individual del alumnado.",style=MaterialTheme.typography.bodySmall)
            view.criteria.forEach {criterion->Row {Checkbox(criterion.id in observed,{value->vm.patch(record){j->
                val current=j.optJSONObject("workshopFlow") ?: JSONObject();val old=WorkshopCodec.strings(current.opt("observedCriterionIds")).toSet()
                current.put("version",1).put("observedCriterionIds",JSONArray((if(value)old+criterion.id else old-criterion.id).toList()))
                val snapshots=current.optJSONObject("criteriaById") ?: JSONObject();snapshots.put(criterion.id,JSONObject().put("label",criterion.label).put("status",criterion.reviewStatus));current.put("criteriaById",snapshots);j.put("workshopFlow",current)
            }},enabled=view.personal);Text(criterion.label,Modifier.weight(1f))}}
            if(view.personal)OutlinedTextField(flow.optString("teacherReflection"),{value->vm.patch(record){j->val current=j.optJSONObject("workshopFlow") ?: JSONObject();j.put("workshopFlow",current.put("version",1).put("teacherReflection",value.take(6000))) }},label={Text("Mi reflexión docente")},minLines=3,modifier=Modifier.fillMaxWidth())
            else Text("Comienza una copia para guardar observaciones docentes.")
            flow.optJSONObject("criteriaById")?.let {previous->previous.keys().asSequence().filter {id->view.criteria.none {it.id==id}}.forEach {id->Text("Criterio anterior conservado: ${previous.getJSONObject(id).optString("label")}",style=MaterialTheme.typography.bodySmall)}}
        }}
        item("adaptations"){FlowCard("Adaptaciones y seguridad",WorkshopColors.Yellow,Icons.Outlined.Shield){BriefText(guide.optString("inclusion"));BriefText(guide.optString("safety"));val j=JSONObject(record.json);WorkshopCodec.strings(j.optJSONObject("safety")?.opt("childrenForbiddenActions")).forEach {Text("Solo adulto: $it")};TextButton(onClick=actions.completeGuide){Text("Ver guía completa, variantes y fuentes")}}}
        item("future"){FlowCard("Próximamente Mis alumnos",WorkshopColors.Mint,Icons.Outlined.EventNote){Text("En una futura integración podrás relacionar esta actividad con tu aula y recoger evidencias individuales. Actualmente no se guardan estudiantes ni calificaciones.");OutlinedButton(onClick={},enabled=false,modifier=Modifier.fillMaxWidth()){Text("Próxima integración")}}}
        if(view.personal)item("export"){FlowAction("Vista previa / exportar PDF","workshop-classroom-export",actions.export,false)}
        item("finish"){FlowAction("Volver a la ficha","workshop-classroom-finish",{actions.navigate(WorkshopWindow.DETAIL)});FlowAction("Revisar materiales y uso real","workshop-classroom-materials",{actions.navigate(WorkshopWindow.MATERIALS)},false)}
    }
}
