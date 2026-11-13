package pe.profelab.feature.students

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import org.json.JSONArray
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import java.time.LocalDate

@Composable
internal fun EducationEditor(kind:String,id:String?,classroomId:String?,all:List<WorkRecord>,store:WorkspaceStore,dismiss:()->Unit){
    val classroom=EducationData.classrooms(all).find {it.id==id};val student=EducationData.students(all).find {it.id==id};val team=EducationData.teams(all).find {it.id==id};val phrase=EducationData.phrases(all).find {it.id==id}
    var name by rememberSaveable(kind,id){mutableStateOf(when(kind){"classroom"->classroom?.name;"student"->student?.displayName;"team"->team?.name;else->phrase?.text}.orEmpty())}
    var grade by rememberSaveable(kind,id){mutableStateOf((classroom?.grade ?: 1).toString())};var section by rememberSaveable(kind,id){mutableStateOf(classroom?.section.orEmpty())}
    var year by rememberSaveable(kind,id){mutableStateOf((classroom?.schoolYear ?: LocalDate.now().year).toString())}
    var members by rememberSaveable(kind,id){mutableStateOf(JSONArray(team?.studentIds.orEmpty().toList()).toString())}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    EducationDialog(when(kind){"classroom"->"Aula";"student"->"Estudiante";"team"->"Equipo";else->"Observación frecuente"},{if(!actions.busy)dismiss()}){
        EducationState(actions)
        Text(if(kind=="student")"Registra únicamente un nombre visible. No se solicitan DNI, dirección o teléfono."else "Datos privados en este dispositivo.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(name,{name=it.take(if(kind=="phrase")1000 else 100)},label={Text(if(kind=="phrase")"Texto editable"else "Nombre visible")},modifier=Modifier.fillMaxWidth())
        if(kind=="classroom"){
            OutlinedTextField(grade,{grade=it.take(1)},label={Text("Grado (1–6)")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(section,{section=it.take(30)},label={Text("Sección")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(year,{year=it.take(4)},label={Text("Año escolar")},modifier=Modifier.fillMaxWidth())
        }
        if(kind=="team"){
            Text("Selecciona integrantes de esta aula. La composición de ejecuciones anteriores se conserva.")
            val selected=EducationData.strings(JSONArray(members))
            EducationData.students(all).filter {it.classroomId==classroomId && (!it.archived || it.id in selected)}.forEach {s->Row {
                Checkbox(s.id in selected,{checked->members=JSONArray((if(checked)selected+s.id else selected-s.id).toList()).toString()})
                Text(s.displayName+if(s.archived)" (archivado; quitar para guardar)"else "",Modifier.weight(1f))
            }}
        }
        if(kind=="phrase")EducationData.suggestedPhrases.forEach {suggestion->TextButton(onClick={name=suggestion}){Text(suggestion)}}
        Button(onClick={actions.perform {
            val actualId=id ?: pe.profelab.core.model.newId()
            when(kind){
                "classroom"->repo.saveClassroom(name,requireNotNull(grade.toIntOrNull()){"Revisa el grado"},section,requireNotNull(year.toIntOrNull()){"Revisa el año"},actualId)
                "student"->repo.saveStudent(requireNotNull(classroomId),name,actualId)
                "team"->repo.saveTeam(requireNotNull(classroomId),name,EducationData.strings(JSONArray(members)),actualId)
                else->repo.savePhrase(name,actualId)
            };dismiss()
        }},enabled=!actions.busy && name.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("Guardar")}
    }
}
