package pe.profelab.feature.students.assessment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.newId
import pe.profelab.feature.students.*

/** Templates are editorial and immutable: every adaptation saves a new version. */
@Composable
internal fun AssessmentTemplateEditor(subject:EduSubjectOffering,grade:Int,all:List<WorkRecord>,store:WorkspaceStore,
    previous:EduAssessmentTemplate?=null,dismiss:()->Unit){
    var competency by remember(subject.id,previous?.id){mutableStateOf(previous?.competency.orEmpty())}
    var activity by remember(subject.id,previous?.id){mutableStateOf(previous?.activityId.orEmpty())}
    var kind by remember(subject.id,previous?.id){mutableStateOf(previous?.kind ?: EduInstrumentKind.CHECKLIST)}
    val criteria=remember(subject.id,previous?.id){mutableStateListOf<EduRubricCriterion>().also {it.addAll(previous?.criteria ?: listOf(EduRubricCriterion(newId(),"")))}}
    var criterionSelection by remember {mutableStateOf(false)}
    var selectedIndex by remember {mutableIntStateOf(0)}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    val observed=EducationData.observations(all).filter {it.classroomId==subject.classroomId && !it.archived}.map {it.criterion}.distinctBy {it.id}
    EducationDialog(if(previous==null)"Nuevo instrumento"else "Adaptar instrumento · nueva versión",{if(!actions.busy)dismiss()}){
        EducationState(actions)
        Text("${subject.name} · $grade.º · SUGERIDO_NO_HOMOLOGADO")
        Text("Los textos son propuestas del docente. No se presentan como contenido oficial MINEDU. Guardar no cambia evaluaciones anteriores.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(competency,{competency=it.take(1000)},label={Text("Competencia o propósito propuesto")},modifier=Modifier.fillMaxWidth())
        EducationChoices(EduInstrumentKind.entries.map {it.name to it.label},kind.name){kind=EduInstrumentKind.valueOf(it)}
        OutlinedTextField(activity,{activity=it.take(180)},label={Text("ID de actividad (opcional)")},modifier=Modifier.fillMaxWidth())
        criteria.forEachIndexed {index,criterion->EducationCard("Criterio ${index+1}"){
            OutlinedTextField(criterion.label,{value->criteria[index]=criterion.copy(label=value.take(1000))},label={Text("Criterio observable")},modifier=Modifier.fillMaxWidth())
            if(observed.isNotEmpty())TextButton(onClick={selectedIndex=index;criterionSelection=true}){Text("Vincular criterio ya observado en Taller")}
            val keys=when(kind){EduInstrumentKind.ANALYTIC_RUBRIC->listOf("AD","A","B","C");EduInstrumentKind.DESCRIPTIVE_SCALE->listOf("AUTONOMOUS","WITH_SUPPORT","IN_PROGRESS","NOT_OBSERVED");EduInstrumentKind.CHECKLIST->listOf("OBSERVED","NOT_OBSERVED")}
            keys.forEach {key->OutlinedTextField(criterion.descriptors[key].orEmpty(),{value->criteria[index]=criteria[index].copy(descriptors=criteria[index].descriptors+(key to value.take(2000)))},
                label={Text(when(key){"OBSERVED"->"Evidencia observada";"NOT_OBSERVED"->"Aún no observado";"AUTONOMOUS"->"Autónomo";"WITH_SUPPORT"->"Con apoyo";"IN_PROGRESS"->"En proceso";else->"Descriptor $key"})},modifier=Modifier.fillMaxWidth())}
            if(criteria.size>1)TextButton(onClick={criteria.removeAt(index)}){Text("Quitar criterio de esta versión")}
        }}
        TextButton(onClick={criteria.add(EduRubricCriterion(newId(),""))},enabled=criteria.size<30){Text("Añadir criterio")}
        Text("Una escala rápida o lista de cotejo nunca asigna AD/A/B/C automáticamente.")
        Button(onClick={actions.perform {
            val cleaned=criteria.map {it.copy(descriptors=it.descriptors.filterValues(String::isNotBlank))}
            repo.saveTemplate(subject.id,competency,grade,activity,kind,cleaned,previous?.id);dismiss()
        }},enabled=!actions.busy && competency.isNotBlank() && criteria.all {it.label.isNotBlank()},modifier=Modifier.fillMaxWidth()){Text("Guardar versión del instrumento")}
    }
    if(criterionSelection)EducationDialog("Criterios observados en esta aula",{criterionSelection=false}){
        Text("El vínculo conserva el mismo criterio para revisar evidencias individuales.")
        observed.forEach {criterion->TextButton(onClick={criteria[selectedIndex]=criteria[selectedIndex].copy(id=criterion.id,label=criterion.label);criterionSelection=false}){Text(criterion.label)}}
    }
}
