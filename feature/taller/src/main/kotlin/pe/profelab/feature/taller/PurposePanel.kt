package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*
import pe.profelab.core.ui.CanonicalMascot

@Composable
internal fun PurposePanel(record:WorkRecord,view:ActivityViewState,mode:WorkshopPerspective,back:()->Unit,actions:ActivityActions){
    val j=JSONObject(record.json);val practical=j.optJSONObject("pedagogy")?.optJSONObject("practical");val guide=TeacherGuides.resolve(record)
    WorkshopPage("¿Para qué sirve?","Función pedagógica y educativa",back,screenTag="workshop-purpose"){
        item("mode"){PerspectiveBar(mode,actions.perspective)}
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("learning"){FlowCard("Aprendizajes que favorece",WorkshopColors.Yellow,Icons.Outlined.MyLocation){Text(practical?.optString("purpose").orEmpty().ifBlank {view.summary}.ifBlank {"Propósito por completar en tu copia."});if(mode!=WorkshopPerspective.PRACTICAL)Text(guide.optString("purpose"))}}
        item("classroom"){FlowCard("Cómo ayuda en clase",WorkshopColors.Blue,Icons.Outlined.School){Row {CanonicalMascot(Modifier.width(84.dp).height(110.dp));Column(Modifier.weight(1f)){Text(practical?.optString("howToUse").orEmpty().ifBlank {guide.optString("participation")});if(j.optString("classroomUse").isNotBlank())BriefText(j.optString("classroomUse"))}}}}
        item("observe"){FlowCard("Qué puedes observar",WorkshopColors.Peach,Icons.Outlined.Visibility){view.criteria.forEach {criterion->Row {Icon(Icons.Outlined.CheckCircle,null);Text(criterion.label,Modifier.weight(1f))}};if(view.criteria.isEmpty())Text(practical?.optString("whatToObserve").orEmpty().ifBlank {"Criterios por personalizar; no hay una evaluación automática."})}}
        item("inclusion"){FlowCard("Adaptaciones e inclusión",WorkshopColors.Mint){WorkshopCodec.strings(j.optJSONObject("inclusion")?.opt("suggestedAdjustments")).forEach {Text(it)};BriefText(guide.optString("inclusion"));if(j.optString("adaptations").isNotBlank())Text(j.optString("adaptations"))}}
        if(mode==WorkshopPerspective.TEACHER)item("teaching"){FlowCard("Docente · Cómo enseñarlo",WorkshopColors.Blue){BriefText(guide.optString("introShort"));TextButton(onClick={actions.navigate(WorkshopWindow.CLASSROOM)}){Text("Abrir presentación en clase")}}}
        if(mode==WorkshopPerspective.ACADEMIC)item("academic"){AcademicBlocks(j)}
        item("sources"){FlowCard("Área sugerida y fuentes",WorkshopColors.Paper){ProjectMetadata(j);Text(j.optString("source").ifBlank {"Elaboración del usuario; registra tus fuentes."});Text("Propuesta editorial sin homologación curricular automática. Los beneficios son posibilidades de práctica, no efectos psicológicos garantizados.",style=MaterialTheme.typography.bodySmall)}}
        item("next"){FlowAction(if(view.experiment)"Investigar paso a paso"else "Construir paso a paso","workshop-purpose-next",{actions.navigate(WorkshopWindow.STEPS)})}
    }
}
