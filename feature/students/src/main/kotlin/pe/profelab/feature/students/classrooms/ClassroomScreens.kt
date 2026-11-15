package pe.profelab.feature.students.classrooms

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.ui.CanonicalMascot
import pe.profelab.feature.students.*
import pe.profelab.feature.students.subjects.SubjectList
import java.time.LocalDate

@Composable
internal fun ColumnScope.ClassroomDirectory(all:List<WorkRecord>,store:WorkspaceStore,open:(String)->Unit,tools:()->Unit){
    val classrooms=EducationData.classrooms(all)
    var year by rememberSaveable {mutableIntStateOf(classrooms.maxOfOrNull {it.schoolYear} ?: LocalDate.now().year)}
    var search by rememberSaveable {mutableStateOf("")}
    var archived by rememberSaveable {mutableStateOf(false)}
    var help by rememberSaveable {mutableStateOf(false)}
    var editId by rememberSaveable {mutableStateOf<String?>(null)}
    var creating by rememberSaveable {mutableStateOf(false)}
    val years=(classrooms.map {it.schoolYear}+year+LocalDate.now().year).distinct().sortedDescending()
    EducationChoices(years.map {it.toString() to "Año escolar $it"},year.toString()){year=it.toInt()}
    OutlinedTextField(search,{search=it.take(100)},label={Text("Buscar aula")},singleLine=true,modifier=Modifier.fillMaxWidth())
    Row {Checkbox(archived,{archived=it});Text("Consultar aulas archivadas",Modifier.padding(top=12.dp))}
    val visible=classrooms.filter {it.schoolYear==year && (!it.archived || archived) && "${it.name} ${it.grade} ${it.section}".contains(search,true)}
    LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("classroom-directory"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)) {
        if(visible.isEmpty())item("empty"){EducationCard("Tu aula comienza aquí"){
            Text(if(classrooms.isEmpty())"Crea un aula para registrar estudiantes. Taller también funciona sin nombres."else "No hay aulas en esta selección. Cambia el año o la búsqueda.")
        }}
        items(visible,key={it.id}){room->EducationCard("${room.name} · ${room.grade}.º ${room.section}"){
            Text("${room.schoolYear} · ${Education32Data.studentsInClassroom(all,room.id).count {!it.archived}} estudiantes · ${Education32Data.offerings(all).count {it.classroomId==room.id && !it.archived}} áreas"+(if(room.archived)" · Archivada"else ""))
            Button(onClick={open(room.id)},modifier=Modifier.fillMaxWidth()){Text(if(room.archived)"Consultar aula e historial"else "Abrir aula")}
            if(!room.archived)TextButton(onClick={editId=room.id}){Text("Editar aula")}
        }}
        item("create"){Button(onClick={creating=true},modifier=Modifier.fillMaxWidth()){Text("Crear aula")}}
        item("tools"){TextButton(onClick=tools){Text("Seguimiento sin nombres y frases frecuentes")}}
        item("help"){
            TextButton(onClick={help=!help}){Text(if(help)"Ocultar consejo"else "Consejo de Concepto C")}
            if(help)EducationCard("Una ficha, toda su trayectoria"){
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    CanonicalMascot(Modifier.size(76.dp),reduceMotion=true)
                    Text("Reutiliza al estudiante al matricularlo en otro año o aula. Sus observaciones anteriores permanecen vinculadas a su identidad.",Modifier.weight(1f))
                }
            }
        }
    }
    if(creating)EducationEditor("classroom",null,null,all,store){creating=false}
    editId?.let {id->EducationEditor("classroom",id,null,all,store){editId=null}}
}

@Composable
internal fun ColumnScope.ClassroomDetail(room:EduClassroom,all:List<WorkRecord>,store:WorkspaceStore,openStudent:(String)->Unit){
    var tab by rememberSaveable(room.id){mutableStateOf("subjects")}
    var search by rememberSaveable(room.id){mutableStateOf("")}
    var historical by rememberSaveable(room.id){mutableStateOf(room.archived)}
    var editor by rememberSaveable(room.id){mutableStateOf<String?>(null)}
    var editorId by rememberSaveable(room.id){mutableStateOf<String?>(null)}
    var enrollment by rememberSaveable(room.id){mutableStateOf(false)}
    var archiveId by rememberSaveable(room.id){mutableStateOf<String?>(null)}
    var runId by rememberSaveable(room.id){mutableStateOf<String?>(null)}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    Text("${room.grade}.º ${room.section} · Año escolar ${room.schoolYear}"+(if(room.archived)" · Solo consulta"else ""))
    EducationChoices(listOf("subjects" to "Áreas","students" to "Estudiantes","teams" to "Equipos","runs" to "Taller"),tab){tab=it}
    EducationState(actions)
    if(tab=="subjects")SubjectList(room,all,store)
    else {
        if(tab=="students"){
            OutlinedTextField(search,{search=it.take(100)},label={Text("Buscar estudiante")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Row {Checkbox(historical,{historical=it});Text("Mostrar matrículas históricas y archivados",Modifier.padding(top=12.dp))}
        }
        LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)) {
            when(tab){
                "students"->{
                    if(!room.archived)item("actions"){
                        Button(onClick={editor="student";editorId=null},modifier=Modifier.fillMaxWidth()){Text("Registrar estudiante")}
                        TextButton(onClick={enrollment=true}){Text("Matricular estudiante ya registrado")}
                    }
                    val students=(if(historical)EducationData.students(all).filter {s->Education32Data.enrollments(all).any {it.studentId==s.id && it.classroomId==room.id}}else Education32Data.studentsInClassroom(all,room.id)).filter {it.displayName.contains(search,true)}
                    if(students.isEmpty())item("empty"){Text("Sin estudiantes en esta selección. No se crean alumnos de ejemplo.")}
                    items(students,key={it.id}){student->EducationCard(student.displayName){
                        Text(if(student.archived)"Archivado · historial conservado"else "Identidad permanente · matrícula ${room.schoolYear}")
                        Button(onClick={openStudent(student.id)},modifier=Modifier.fillMaxWidth()){Text("Abrir ficha")}
                    }}
                }
                "teams"->{
                    if(!room.archived)item("create"){Button(onClick={editor="team";editorId=null},modifier=Modifier.fillMaxWidth()){Text("Crear equipo")}}
                    val teams=EducationData.teams(all).filter {it.classroomId==room.id && !it.archived}
                    if(teams.isEmpty())item("empty"){Text("No hay equipos. Puedes crearlos con estudiantes ya matriculados.")}
                    items(teams,key={it.id}){team->EducationCard(team.name){
                        Text("${team.studentIds.size} integrantes · las ejecuciones previas conservan su composición")
                        if(!room.archived){TextButton(onClick={editor="team";editorId=team.id}){Text("Editar integrantes")};TextButton(onClick={archiveId=team.id}){Text("Archivar equipo")}}
                    }}
                }
                else->{
                    val runs=EducationData.runs(all).filter {it.classroomId==room.id}.sortedByDescending {it.createdAt}
                    item("info"){Text("Prepara una copia personal en Taller y selecciona esta aula en «Aula y participantes». Consultar o evaluar no consume materiales.")}
                    if(runs.isEmpty())item("empty"){Text("Todavía no hay ejecuciones en esta aula.")}
                    items(runs,key={it.id}){run->EducationCard(run.activityTitle){Text("${run.participantCount} participantes · versión ${run.contentVersion}");TextButton(onClick={runId=run.id}){Text("Revisar observaciones")};if(!run.archived)TextButton(onClick={archiveId=run.id}){Text("Archivar ejecución")}}}
                }
            }
            if(!room.archived)item("archive-room"){TextButton(onClick={archiveId=room.id}){Text("Archivar aula conservando historial")}}
        }
    }
    editor?.let {kind->EducationEditor(kind,editorId,room.id,all,store){editor=null}}
    if(enrollment)EnrollmentChooser(room,all,store){enrollment=false}
    archiveId?.let {id->AlertDialog(onDismissRequest={archiveId=null},title={Text("¿Archivar conservando el historial?")},text={Text("Los registros anteriores permanecen disponibles. No se borran observaciones ni estudiantes.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(id);archiveId=null}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archiveId=null}){Text("Cancelar")}})}
    runId?.let {id->EducationData.runs(all).find {it.id==id}?.let {run->EducationDialog("Seguimiento privado",{runId=null}){EducationRunReview(run,all,store,null)}}}
}

@Composable
private fun EnrollmentChooser(room:EduClassroom,all:List<WorkRecord>,store:WorkspaceStore,dismiss:()->Unit){
    var search by rememberSaveable {mutableStateOf("")}
    var chosen by rememberSaveable {mutableStateOf("")}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    val current=Education32Data.studentsInClassroom(all,room.id).map {it.id}.toSet()
    EducationDialog("Reutilizar identidad del estudiante",{if(!actions.busy)dismiss()}){
        Text("Una nueva matrícula conserva el historial de años y aulas anteriores. No se crea otra persona.")
        OutlinedTextField(search,{search=it.take(100)},label={Text("Buscar por nombre")},modifier=Modifier.fillMaxWidth())
        EducationData.students(all).filter {!it.archived && it.id !in current && it.displayName.contains(search,true)}.forEach {student->
            Row {RadioButton(chosen==student.id,{chosen=student.id});Text(student.displayName,Modifier.padding(top=12.dp))}
        }
        EducationState(actions)
        Button(onClick={actions.perform {repo.enrollStudent(chosen,room.id);dismiss()}},enabled=chosen.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Confirmar matrícula en ${room.schoolYear}")}
    }
}
