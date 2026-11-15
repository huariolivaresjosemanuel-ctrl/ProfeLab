package pe.profelab.feature.students.students

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.feature.students.*
import pe.profelab.feature.students.assessment.AcademicAssessmentEditor
import pe.profelab.feature.students.assessment.TeacherJudgmentEditor
import pe.profelab.feature.students.guardians.GuardianContacts
import pe.profelab.feature.students.timeline.StudentTimeline

@Composable
internal fun ColumnScope.StudentProfile(student:EduStudent,classroomId:String?,all:List<WorkRecord>,store:WorkspaceStore){
    var tab by rememberSaveable(student.id){mutableStateOf("learning")}
    var editName by remember(student.id){mutableStateOf(false)}
    var assessing by remember(student.id){mutableStateOf(false)}
    var judgment by remember(student.id){mutableStateOf(false)}
    var editJudgment by remember(student.id){mutableStateOf<EduCompetencyJudgment?>(null)}
    var archiving by remember(student.id){mutableStateOf(false)}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    EducationCard(student.displayName){
        Text(if(student.archived)"Estudiante archivado · historial conservado"else "Identidad única · las áreas comparten su ficha")
        val rooms=EducationData.classrooms(all).associateBy {it.id}
        Education32Data.enrollments(all).filter {it.studentId==student.id}.sortedByDescending {it.schoolYear}.forEach {enrollment->Text("${enrollment.schoolYear} · ${rooms[enrollment.classroomId]?.name.orEmpty()} · ${if(enrollment.active)"Matrícula activa"else "Matrícula histórica"}",style=MaterialTheme.typography.bodySmall)}
    }
    EducationChoices(listOf("learning" to "Aprendizaje","timeline" to "Historial","family" to "Familia"),tab){tab=it}
    EducationState(actions)
    if(tab=="timeline")StudentTimeline(student.id,all)
    else LazyColumn(Modifier.weight(1f).testTag("student-profile-$tab"),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)){
        if(tab=="family")item("family"){GuardianContacts(student,all,store)}
        else {
            item("register"){
                Text("Tu valoración se registra con su evidencia y escala original. No hace falta iniciar Taller.")
                if(!student.archived)Button(onClick={assessing=true},modifier=Modifier.fillMaxWidth()){Text("Registrar evaluación")}
            }
            val evaluations=Education32Data.academicAssessments(all).filter {it.studentId==student.id && !it.archived}.sortedByDescending {it.createdAt}
            val judgments=Education32Data.judgments(all).filter {it.studentId==student.id && !it.archived}.sortedByDescending {it.createdAt}
            if(evaluations.isEmpty() && judgments.isEmpty())item("empty"){EducationCard("Sin evaluar"){
                Text("No hay valoraciones guardadas. «Sin evaluar» y «No observado» no son calificaciones C ni B.")
            }}
            evaluations.take(10).forEach {evaluation->item("academic-${evaluation.id}"){EducationCard("${evaluation.taskKind.label} · ${evaluation.period}"){
                Text(evaluation.competency)
                Text("${evaluation.scale.label}: ${evaluation.value}",style=MaterialTheme.typography.titleMedium)
                Text(evaluation.criterion)
                Text("Registrado por ${evaluation.teacher}",style=MaterialTheme.typography.bodySmall)
            }}}
            judgments.take(10).forEach {row->item("judgment-${row.id}"){EducationCard("Valoración docente · ${row.state.label()}"){
                val template=Education32Data.templates(all).find {it.id==row.templateId}
                Text(template?.competency ?: "Competencia histórica")
                Text(row.level?.name ?: "Sin nivel asignado")
                Text(row.description)
                Text("Instrumento v${template?.version ?: "?"} · decisión manual",style=MaterialTheme.typography.bodySmall)
                if(!student.archived && row.state!=EduJudgmentState.SUPERSEDED)TextButton(onClick={editJudgment=row;judgment=true}){Text("Revisar y confirmar una nueva valoración")}
            }}}
            if(!student.archived)item("workshop-assessment"){OutlinedButton(onClick={editJudgment=null;judgment=true},modifier=Modifier.fillMaxWidth()){Text("Valorar evidencias de Taller con instrumento")}}
            item("history"){TextButton(onClick={tab="timeline"}){Text("Ver historial completo y filtrar")}}
        }
        if(!student.archived)item("edit-student"){
            TextButton(onClick={editName=true}){Text("Editar nombre visible")}
            TextButton(onClick={archiving=true}){Text("Archivar estudiante conservando historial")}
        }
    }
    if(editName)EducationEditor("student",student.id,student.classroomId,all,store){editName=false}
    if(assessing)AcademicAssessmentEditor(student,classroomId,all,store){assessing=false}
    if(judgment)TeacherJudgmentEditor(student,classroomId,all,store,editJudgment){judgment=false}
    if(archiving)AlertDialog(onDismissRequest={archiving=false},title={Text("¿Archivar estudiante?")},text={Text("Se conservarán sus matrículas, observaciones y evaluaciones. No se borran datos ni se cambian ejecuciones anteriores.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(student.id);archiving=false}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archiving=false}){Text("Cancelar")}})
}

internal fun EduJudgmentState.label()=when(this){EduJudgmentState.DRAFT->"Borrador";EduJudgmentState.CONFIRMED->"Confirmada";EduJudgmentState.SUPERSEDED->"Revisada posteriormente"}
