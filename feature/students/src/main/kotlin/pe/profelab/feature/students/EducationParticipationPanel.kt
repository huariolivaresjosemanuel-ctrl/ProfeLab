package pe.profelab.feature.students

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore

/** Optional inline W03 preparation; no new Taller root window or requirement to enter names. */
@Composable
fun EducationParticipationPanel(record:WorkRecord,all:List<WorkRecord>,store:WorkspaceStore){
    if(record.kind !in setOf("project","experiment") || record.archived)return
    val selected=EducationData.selectedRun(record,all);val runs=EducationData.runs(all).filter {it.personalProjectId==record.id && !it.archived}
    var expanded by rememberSaveable(record.id){mutableStateOf(false)};var classroomId by rememberSaveable(record.id){mutableStateOf(selected?.classroomId.orEmpty())}
    var people by rememberSaveable(record.id,classroomId){mutableStateOf("[]")};var teams by rememberSaveable(record.id,classroomId){mutableStateOf("[]")}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    EducationCard("Aula y participantes · opcional"){
        Text(selected?.let {"Ejecución preparada: ${it.participantCount} participantes · ${it.teams.size} equipos"} ?: "Puedes continuar con una cantidad de participantes, sin nombres ni registro de alumnos.")
        TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Ocultar selección"else "Preparar o cambiar ejecución docente")}
        if(expanded){
            EducationState(actions)
            if(runs.isNotEmpty()){
                Text("Retomar una ejecución de esta misma copia")
                runs.forEach {run->val room=EducationData.classrooms(all).find {it.id==run.classroomId}
                    TextButton(onClick={actions.perform {repo.selectRun(record.id,run.id)}},enabled=!actions.busy){Text("${run.classroomLabel.ifBlank {room?.name ?: "Sin nombres"}} · ${run.participantCount} participantes · ${java.time.Instant.ofEpochMilli(run.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()}")}
                }
            }
            Text("Nueva ejecución independiente",style=MaterialTheme.typography.titleSmall)
            val classrooms=EducationData.classrooms(all).filter {!it.archived}
            EducationChoices(listOf("" to "Solo cantidad, sin nombres")+classrooms.map {it.id to "${it.name} · ${it.grade}.º ${it.section}"},classroomId){classroomId=it}
            if(classrooms.isEmpty())Text("Para usar nombres más adelante, crea aulas y estudiantes desde Cuaderno → Mis alumnos.")
            if(classroomId.isNotBlank()){
                val selectedPeople=EducationData.strings(JSONArray(people));val selectedTeams=EducationData.strings(JSONArray(teams))
                Text("Estudiantes de esta aula")
                EducationData.students(all).filter {it.classroomId==classroomId && !it.archived}.forEach {student->Row {
                    Checkbox(student.id in selectedPeople,{checked->people=JSONArray((if(checked)selectedPeople+student.id else selectedPeople-student.id).toList()).toString()})
                    Text(student.displayName,Modifier.weight(1f))
                }}
                Text("Equipos · se conservará la composición de esta ejecución")
                EducationData.teams(all).filter {it.classroomId==classroomId && !it.archived}.forEach {team->Row {
                    Checkbox(team.id in selectedTeams,{checked->teams=JSONArray((if(checked)selectedTeams+team.id else selectedTeams-team.id).toList()).toString()})
                    Text("${team.name} · ${team.studentIds.size} integrantes",Modifier.weight(1f))
                }}
            }
            Text("La cantidad total incluye a los seleccionados y puede incluir participantes sin nombre. Confirmar esta selección no multiplica ni consume materiales.",style=MaterialTheme.typography.bodySmall)
            Button(onClick={actions.perform {
                val studentIds=EducationData.strings(JSONArray(people));val teamIds=EducationData.strings(JSONArray(teams))
                val union=studentIds+EducationData.teams(all).filter {it.id in teamIds}.flatMap {it.studentIds}
                val count=maxOf(JSONObject(record.json).optJSONObject("teacherPreparation")?.optInt("participantCount",1) ?: 1,union.size)
                repo.createRun(record,classroomId.takeIf {it.isNotBlank()},studentIds,teamIds,count);expanded=false
            }},enabled=!actions.busy,modifier=Modifier.fillMaxWidth()){Text("Confirmar nueva ejecución")}
        }
    }
}
