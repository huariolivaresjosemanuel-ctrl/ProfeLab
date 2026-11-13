package pe.profelab.feature.students

import androidx.activity.compose.BackHandler

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore

/** Private Cuaderno subsection; never a sixth root tab or an editorial catalog. */
@Composable
fun StudentsWorkspace(all:List<WorkRecord>,store:WorkspaceStore,back:()->Unit){
    BackHandler(onBack=back)
    var tab by rememberSaveable {mutableStateOf("classrooms")};var classroomId by rememberSaveable {mutableStateOf<String?>(null)};var showArchived by rememberSaveable {mutableStateOf(false)}
    var editor by rememberSaveable {mutableStateOf<String?>(null)};var editorId by rememberSaveable {mutableStateOf<String?>(null)};var archiveId by rememberSaveable {mutableStateOf<String?>(null)};var runId by rememberSaveable {mutableStateOf<String?>(null)}
    val classrooms=EducationData.classrooms(all);val selected=classrooms.find {it.id==classroomId};val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    fun edit(kind:String,id:String?=null){editor=kind;editorId=id}
    Column(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
        Row {Text("Mis alumnos",Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall);TextButton(onClick=back){Text("Volver a Cuaderno")}}
        Text("Aulas, equipos y seguimiento privados. No se crean estudiantes de ejemplo.",style=MaterialTheme.typography.bodySmall)
        EducationChoices(listOf("classrooms" to "Aulas","students" to "Alumnos","teams" to "Equipos","runs" to "Seguimiento","phrases" to "Frases"),tab){tab=it}
        EducationState(actions)
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(tab!="phrases" && tab!="classrooms")item("class-selector"){
                EducationChoices(listOf("" to "Sin aula seleccionada")+classrooms.filter {!it.archived || showArchived}.map {it.id to "${it.name} · ${it.grade}.º ${it.section}"},classroomId.orEmpty()){classroomId=it.takeIf {x->x.isNotBlank()}}
                selected?.let {Text(it.name+if(it.archived)" · archivada"else " · ${it.schoolYear}")}
            }
            item("archives"){Row {Checkbox(showArchived,{showArchived=it});Text("Mostrar archivos e historial archivado")}}
            when(tab){
                "classrooms"->{
                    item("create"){Button(onClick={edit("classroom")}){Text("Crear aula")}}
                    if(classrooms.isEmpty())item("empty"){Text("Crea un aula si deseas seguimiento por nombres. Taller también funciona solo con una cantidad de participantes.")}
                    items(classrooms.filter {!it.archived || showArchived},key={it.id}){c->EducationCard("${c.name} · ${c.grade}.º ${c.section} · ${c.schoolYear}"){
                        TextButton(onClick={classroomId=c.id;tab=if(c.archived)"runs"else "students"}){Text(if(c.archived)"Consultar historial"else "Abrir aula")}
                        if(!c.archived){TextButton(onClick={edit("classroom",c.id)}){Text("Editar aula")};TextButton(onClick={archiveId=c.id}){Text("Archivar conservando historial")}}
                    }}
                }
                "students"->{
                    item("create"){Button(onClick={edit("student")},enabled=selected!=null && !selected.archived){Text("Registrar estudiante")}}
                    items(EducationData.students(all).filter {it.classroomId==classroomId && (!it.archived || showArchived)},key={it.id}){s->EducationCard(s.displayName){
                        if(!s.archived && selected?.archived==false){TextButton(onClick={edit("student",s.id)}){Text("Editar nombre")};TextButton(onClick={archiveId=s.id}){Text("Archivar")}}else Text("Archivado; se conserva su historial.")
                    }}
                }
                "teams"->{
                    item("create"){Button(onClick={edit("team")},enabled=selected!=null && !selected.archived){Text("Crear equipo")}}
                    items(EducationData.teams(all).filter {it.classroomId==classroomId && (!it.archived || showArchived)},key={it.id}){team->EducationCard(team.name){
                        Text("${team.studentIds.size} integrantes de esta aula")
                        if(!team.archived && selected?.archived==false){TextButton(onClick={edit("team",team.id)}){Text("Editar integrantes")};TextButton(onClick={archiveId=team.id}){Text("Archivar equipo")}}
                    }}
                }
                "runs"->{
                    val runs=EducationData.runs(all).filter {it.classroomId==classroomId && (!it.archived || showArchived)}.sortedByDescending {it.createdAt}
                    if(runs.isEmpty())item("empty"){Text("No hay ejecuciones en esta selección. En Taller, prepara una copia personal y elige opcionalmente aula y participantes.")}
                    items(runs,key={it.id}){run->EducationCard(run.activityTitle.ifBlank {"Actividad personal"}){
                        Text("${run.participantCount} participantes · ${run.teams.size} equipos · versión ${run.contentVersion}")
                        TextButton(onClick={runId=run.id}){Text("Observaciones y retroalimentación")}
                        if(!run.archived)TextButton(onClick={archiveId=run.id}){Text("Archivar ejecución")}
                    }}
                }
                else->{
                    item("create"){Button(onClick={edit("phrase")}){Text("Añadir observación frecuente")};Text("Las sugerencias se pueden adaptar antes de guardarlas.")}
                    items(EducationData.phrases(all).filter {!it.archived || showArchived},key={it.id}){phrase->EducationCard(phrase.text){if(!phrase.archived){TextButton(onClick={edit("phrase",phrase.id)}){Text("Editar texto")};TextButton(onClick={archiveId=phrase.id}){Text("Archivar frase")}}}}
                }
            }
        }
    }
    editor?.let {kind->EducationEditor(kind,editorId,classroomId,all,store){editor=null}}
    archiveId?.let {id->AlertDialog(onDismissRequest={archiveId=null},title={Text("¿Archivar este registro?")},text={Text("Se conserva el historial y las observaciones. No se borran estudiantes ni ejecuciones anteriores.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(id);archiveId=null}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archiveId=null}){Text("Cancelar")}})}
    runId?.let {id->EducationData.runs(all).find {it.id==id}?.let {run->EducationDialog("Seguimiento privado",{runId=null}){EducationRunReview(run,all,store,null)}}}
}
