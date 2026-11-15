package pe.profelab.feature.students.subjects

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.newId
import pe.profelab.feature.students.*
import pe.profelab.feature.students.assessment.AssessmentTemplateEditor

@Composable
internal fun ColumnScope.SubjectList(room:EduClassroom,all:List<WorkRecord>,store:WorkspaceStore){
    var editor by remember(room.id){mutableStateOf(false)}
    var editSubject by remember(room.id){mutableStateOf<EduSubjectOffering?>(null)}
    var name by remember(room.id){mutableStateOf("")}
    var templateSubject by remember(room.id){mutableStateOf<EduSubjectOffering?>(null)}
    var previousTemplate by remember(room.id){mutableStateOf<EduAssessmentTemplate?>(null)}
    var archive by remember(room.id){mutableStateOf<String?>(null)}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    val subjects=Education32Data.offerings(all).filter {it.classroomId==room.id && !it.archived}
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)){
        item("intro"){Text("Las áreas comparten la misma matrícula. Crear un curso no duplica estudiantes.")}
        if(subjects.isEmpty())item("empty"){Text("Sin áreas registradas. Añade las que impartes en esta aula.")}
        items(subjects,key={it.id}){subject->EducationCard(subject.name){
            val templates=Education32Data.templates(all).filter {it.subjectId==subject.id && !it.archived}.sortedByDescending {it.version}
            Text("${templates.map {it.templateKey}.distinct().size} instrumentos · propuestas docentes sin homologación curricular")
            if(!room.archived){
                TextButton(onClick={editSubject=subject;name=subject.name;editor=true}){Text("Editar área")}
                TextButton(onClick={templateSubject=subject;previousTemplate=null}){Text("Crear instrumento de evaluación")}
                TextButton(onClick={archive=subject.id}){Text("Archivar área conservando evaluaciones")}
            }
            templates.forEach {template->
                Text("${template.kind.label} · ${template.competency} · v${template.version}",style=MaterialTheme.typography.bodyMedium)
                Text(template.sourceStatus,style=MaterialTheme.typography.labelSmall)
                if(!room.archived)TextButton(onClick={templateSubject=subject;previousTemplate=template}){Text("Adaptar en una nueva versión")}
            }
        }}
        if(!room.archived)item("create"){Button(onClick={editSubject=null;name="";editor=true},modifier=Modifier.fillMaxWidth()){Text("Añadir área o curso")}}
    }
    if(editor)EducationDialog(if(editSubject==null)"Nueva área o curso"else "Editar área",{if(!actions.busy)editor=false}){
        EducationState(actions)
        OutlinedTextField(name,{name=it.take(100)},label={Text("Nombre del área")},modifier=Modifier.fillMaxWidth())
        Button(onClick={actions.perform {repo.saveOffering(room.id,name,editSubject?.id ?: newId());editor=false}},enabled=name.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Guardar área")}
    }
    templateSubject?.let {subject->AssessmentTemplateEditor(subject,room.grade,all,store,previousTemplate){templateSubject=null}}
    archive?.let {id->AlertDialog(onDismissRequest={archive=null},title={Text("¿Archivar área?")},text={Text("Las evaluaciones y versiones anteriores se conservan.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(id);archive=null}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archive=null}){Text("Cancelar")}})}
}
