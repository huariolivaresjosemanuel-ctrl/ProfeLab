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
        item("sections"){WorkshopChoices(listOf("Presentación","Evaluación","Adaptación"),section){section=it}}
        if(section=="Presentación"){
        item("sequence"){TeacherPresentationCard(record,guide)}
        item("questions"){FlowCard("Preguntas para el aula",WorkshopColors.Blue,Icons.Outlined.QuestionAnswer){(WorkshopCodec.strings(guide.opt("priorKnowledgeQuestions"))+WorkshopCodec.strings(guide.opt("reflectiveQuestions"))).distinct().forEachIndexed {i,q->Text("${i+1}. $q")}}}
        item("script"){FlowCard("Guion breve",WorkshopColors.Paper,Icons.Outlined.MenuBook){BriefText(guide.optString("introShort").ifBlank {"Guion vacío por elección del usuario. Puedes editarlo en tu copia."});TextButton(onClick=if(view.personal)actions.editTeacher else actions.begin){Text(if(view.personal)"Editar mi guion docente"else "Crear mi guion docente")}}}
        }
        if(section=="Evaluación"){
        item("formative"){FlowCard("Evaluación formativa simple",WorkshopColors.Peach,Icons.Outlined.FactCheck){
            Text("Estos checks describen la actividad y no generan notas. Las observaciones individuales se guardan en el panel privado de alumnos.",style=MaterialTheme.typography.bodySmall)
            view.criteria.forEach {criterion->Row {Checkbox(criterion.id in observed,{value->vm.patch(record){j->
                val current=j.optJSONObject("workshopFlow") ?: JSONObject();val old=WorkshopCodec.strings(current.opt("observedCriterionIds")).toSet()
                current.put("version",1).put("observedCriterionIds",JSONArray((if(value)old+criterion.id else old-criterion.id).toList()))
                val snapshots=current.optJSONObject("criteriaById") ?: JSONObject();snapshots.put(criterion.id,JSONObject().put("label",criterion.label).put("status",criterion.reviewStatus));current.put("criteriaById",snapshots);j.put("workshopFlow",current)
            }},enabled=view.personal);Text(criterion.label,Modifier.weight(1f))}}
            if(view.personal)OutlinedTextField(flow.optString("teacherReflection"),{value->vm.patch(record){j->val current=j.optJSONObject("workshopFlow") ?: JSONObject();j.put("workshopFlow",current.put("version",1).put("teacherReflection",value.take(6000))) }},label={Text("Mi reflexión docente")},minLines=3,modifier=Modifier.fillMaxWidth())
            else Text("Comienza una copia para guardar observaciones docentes.")
            flow.optJSONObject("criteriaById")?.let {previous->previous.keys().asSequence().filter {id->view.criteria.none {it.id==id}}.forEach {id->Text("Criterio anterior conservado: ${previous.getJSONObject(id).optString("label")}",style=MaterialTheme.typography.bodySmall)}}
        }}
        }
        if(section=="Adaptación"){
        item("adaptations"){FlowCard("Adaptaciones y seguridad",WorkshopColors.Yellow,Icons.Outlined.Shield){BriefText(guide.optString("inclusion"));BriefText(guide.optString("safety"));val j=JSONObject(record.json);WorkshopCodec.strings(j.optJSONObject("safety")?.opt("childrenForbiddenActions")).forEach {Text("Solo adulto: $it")};TextButton(onClick=actions.completeGuide){Text("Ver guía completa, variantes y fuentes")}}}
        }
        item("classroom-followup"){FlowCard("Seguimiento privado",WorkshopColors.Mint,Icons.Outlined.EventNote){Text("Puedes observar alumnos o equipos sin notas automáticas. Registrar alumnos es opcional y no bloquea el procedimiento.");if(view.personal)TextButton(onClick=observe){Text("Observar alumnos / revisar registros")}else Text("Comienza una copia para vincular una ejecución privada; las aulas se administran desde Cuaderno → Mis alumnos.")}}
        if(view.personal)item("export"){FlowAction("Vista previa / exportar PDF","workshop-classroom-export",actions.export,false)}
        item("finish"){FlowAction("Comenzar procedimiento","workshop-classroom-procedure",{actions.navigate(WorkshopWindow.STEPS)});FlowAction("Volver a la ficha","workshop-classroom-finish",{actions.navigate(WorkshopWindow.DETAIL)},false);FlowAction("Ver resultados y reflexión","workshop-classroom-results",{actions.navigate(WorkshopWindow.RESULTS)},false);FlowAction("Revisar materiales y uso real","workshop-classroom-materials",{actions.navigate(WorkshopWindow.MATERIALS)},false);FlowAction("Perspectivas y fundamentos","workshop-classroom-purpose",{actions.navigate(WorkshopWindow.PURPOSE)},false)}
    }
}
