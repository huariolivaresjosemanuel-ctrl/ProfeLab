package pe.profelab.feature.students.assessment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.feature.students.*

/** A teacher-entered assessment needs no workshop execution, conversion, or inferred score. */
@Composable
internal fun AcademicAssessmentEditor(student:EduStudent,initialClassroomId:String?,all:List<WorkRecord>,store:WorkspaceStore,dismiss:()->Unit){
    val enrollments=Education32Data.enrollments(all).filter {it.studentId==student.id && !it.archived}
    val rooms=EducationData.classrooms(all).associateBy {it.id}
    var enrollmentId by remember(student.id){mutableStateOf(enrollments.find {it.classroomId==initialClassroomId}?.id ?: enrollments.firstOrNull()?.id.orEmpty())}
    val enrollment=enrollments.find {it.id==enrollmentId}
    val subjects=Education32Data.offerings(all).filter {it.classroomId==enrollment?.classroomId && !it.archived}
    var subjectId by remember(student.id,enrollmentId){mutableStateOf("")}
    val templates=Education32Data.templates(all).filter {it.subjectId==subjectId && !it.archived}
    var templateId by remember(student.id,subjectId){mutableStateOf("")}
    var templateCriterionId by remember(student.id,templateId){mutableStateOf("")}
    val template=templates.find {it.id==templateId}
    var instrumentOptions by remember(student.id){mutableStateOf(false)}
    var sourceOptions by remember(student.id){mutableStateOf(false)}
    var origin by remember(student.id,enrollmentId){mutableStateOf(EduAssessmentOrigin.MANUAL)}
    var sourceRecordId by remember(student.id,enrollmentId,origin){mutableStateOf("")}
    var competency by remember(student.id){mutableStateOf("")}
    var criterion by remember(student.id){mutableStateOf("")}
    var period by remember(student.id){mutableStateOf("")}
    var kind by remember(student.id){mutableStateOf<EduAcademicTaskKind?>(null)}
    var scale by remember(student.id){mutableStateOf<EduOriginalScale?>(null)}
    var value by remember(student.id,scale){mutableStateOf("")}
    var evidence by remember(student.id){mutableStateOf("")}
    var observation by remember(student.id){mutableStateOf("")}
    var improvement by remember(student.id){mutableStateOf("")}
    var teacher by remember(student.id){mutableStateOf("")}
    var reviewed by remember(student.id){mutableStateOf(false)}
    var phase by remember(student.id){mutableIntStateOf(1)}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    EducationDialog("Registrar evaluación · $phase de 3",{if(!actions.busy)dismiss()}){
        EducationState(actions)
        Text(student.displayName,style=MaterialTheme.typography.titleMedium)
        when(phase){
            1->{
                Text("1. Contexto de la evaluación")
                EducationChoices(enrollments.map {e->e.id to "${rooms[e.classroomId]?.name.orEmpty()} · ${e.schoolYear}"},enrollmentId){enrollmentId=it}
                if(subjects.isEmpty())Text("Añade un área desde Aula → Áreas antes de evaluar. No necesitas iniciar Taller.")
                EducationChoices(subjects.map {it.id to it.name},subjectId){subjectId=it}
                if(templates.isNotEmpty()){
                    TextButton(onClick={instrumentOptions=!instrumentOptions}){Text(if(instrumentOptions)"Ocultar instrumentos"else "Elegir instrumento del área (opcional)")}
                    if(instrumentOptions){
                        EducationChoices(listOf("" to "Registrar sin instrumento")+templates.map {it.id to "${it.kind.label} · ${it.competency} · v${it.version}"},templateId){id->
                            templateId=id;templateCriterionId="";templates.find {it.id==id}?.let {competency=it.competency;criterion=""}
                        }
                        template?.let {instrument->EducationChoices(instrument.criteria.map {it.id to it.label},templateCriterionId){id->templateCriterionId=id;criterion=instrument.criteria.single {it.id==id}.label}}
                    }
                }
                OutlinedTextField(competency,{competency=it.take(1000);templateId="";templateCriterionId=""},label={Text("Competencia o propósito")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(criterion,{criterion=it.take(1000);templateId="";templateCriterionId=""},label={Text("Criterio evaluado")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(period,{period=it.take(100)},label={Text("Periodo: bimestre, trimestre u otro")},modifier=Modifier.fillMaxWidth())
                EducationChoices(EduAcademicTaskKind.entries.map {it.name to it.label},kind?.name.orEmpty()){kind=EduAcademicTaskKind.valueOf(it)}
                TextButton(onClick={sourceOptions=!sourceOptions}){Text(if(sourceOptions)"Ocultar origen opcional"else "Vincular a Taller o Sesión (opcional)")}
                if(sourceOptions){
                    EducationChoices(listOf(EduAssessmentOrigin.MANUAL.name to "Registro manual",EduAssessmentOrigin.WORKSHOP.name to "Taller",EduAssessmentOrigin.SESSION.name to "Sesión"),origin.name){origin=EduAssessmentOrigin.valueOf(it)}
                    val sources=when(origin){
                        EduAssessmentOrigin.MANUAL->emptyList()
                        EduAssessmentOrigin.WORKSHOP->EducationData.runs(all).filter {!it.archived && student.id in it.participantIds && it.classroomId==enrollment?.classroomId}.map {it.id to it.activityTitle}
                        EduAssessmentOrigin.SESSION->all.filter {it.kind=="session" && !it.archived}.map {it.id to it.title}
                    }
                    if(origin!=EduAssessmentOrigin.MANUAL && sources.isEmpty())Text("No hay un origen compatible. Puedes continuar como registro manual.")
                    EducationChoices(sources,sourceRecordId){sourceRecordId=it}
                }
                Text("Texto docente · SUGERIDO_NO_HOMOLOGADO",style=MaterialTheme.typography.bodySmall)
                Button(onClick={phase=2},enabled=subjectId.isNotBlank() && competency.isNotBlank() && criterion.isNotBlank() && period.isNotBlank() && kind!=null && (origin==EduAssessmentOrigin.MANUAL || sourceRecordId.isNotBlank()) && (templateId.isBlank() || templateCriterionId.isNotBlank()),modifier=Modifier.fillMaxWidth()){Text("Continuar con evidencia y valoración")}
            }
            2->{
                Text("2. Escala original y evidencia")
                EducationChoices(EduOriginalScale.entries.map {it.name to it.label},scale?.name.orEmpty()){scale=EduOriginalScale.valueOf(it)}
                if(scale==EduOriginalScale.AD_A_B_C)EducationChoices(EduAchievementLevel.entries.map {it.name to it.name},value){value=it}
                else if(scale!=null)OutlinedTextField(value,{value=it.take(2000)},label={Text(if(scale==EduOriginalScale.NUMERIC_0_20)"Valor original (0–20)"else "Valoración original del docente")},modifier=Modifier.fillMaxWidth())
                Text("Se conserva tu escala. No se convierten notas, no se promedian competencias y no se deduce una calificación por rapidez o conducta.",style=MaterialTheme.typography.bodySmall)
                OutlinedTextField(evidence,{evidence=it.take(6000)},label={Text("Evidencia: qué trabajo o desempeño revisaste")},minLines=2,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(observation,{observation=it.take(6000)},label={Text("Observación docente (opcional)")},minLines=2,modifier=Modifier.fillMaxWidth())
                Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()){
                    TextButton(onClick={phase=1}){Text("Atrás")}
                    Button(onClick={phase=3;reviewed=false},enabled=scale!=null && value.isNotBlank() && evidence.isNotBlank()){Text("Revisar")}
                }
            }
            else->{
                Text("3. Revisión y decisión docente")
                Text("${subjects.find {it.id==subjectId}?.name.orEmpty()} · $period\n${kind?.label.orEmpty()} · $competency\nCriterio: $criterion")
                Text("${scale?.label.orEmpty()}: $value",style=MaterialTheme.typography.titleMedium)
                Text("Origen: ${when(origin){EduAssessmentOrigin.MANUAL->"Registro manual";EduAssessmentOrigin.WORKSHOP->"Taller seleccionado";EduAssessmentOrigin.SESSION->"Sesión seleccionada"}}",style=MaterialTheme.typography.bodySmall)
                Text("Evidencia: $evidence")
                if(observation.isNotBlank())Text("Observación: $observation")
                OutlinedTextField(improvement,{improvement=it.take(6000);reviewed=false},label={Text("Acción de mejora elegida por el docente (opcional)")},minLines=2,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(teacher,{teacher=it.take(100);reviewed=false},label={Text("Docente que registra y confirma")},modifier=Modifier.fillMaxWidth())
                Row {Checkbox(reviewed,{reviewed=it},modifier=Modifier.testTag("academic-review-confirmation"));Text("Confirmo mi valoración tras revisar esta evidencia",Modifier.weight(1f))}
                TextButton(onClick={phase=2}){Text("Volver a editar")}
                Button(onClick={actions.perform {
                    repo.saveAcademicAssessment(student.id,enrollmentId,subjectId,competency,criterion,period,requireNotNull(kind),requireNotNull(scale),value,evidence,observation,improvement,teacher,origin,sourceRecordId.takeIf(String::isNotBlank),templateId=templateId.takeIf(String::isNotBlank),templateCriterionId=templateCriterionId.takeIf(String::isNotBlank));dismiss()
                }},enabled=reviewed && teacher.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Confirmar y guardar evaluación")}
            }
        }
    }
}
