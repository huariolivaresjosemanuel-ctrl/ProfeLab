package pe.profelab.feature.students.assessment

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.ui.LocalReviewPrivateEvidence
import pe.profelab.feature.students.*

/** Competency judgment remains separate from quick observation status and numeric grades. */
@Composable
internal fun TeacherJudgmentEditor(student:EduStudent,initialClassroomId:String?,all:List<WorkRecord>,store:WorkspaceStore,
    existing:EduCompetencyJudgment?=null,dismiss:()->Unit){
    val reviewEvidence=LocalReviewPrivateEvidence.current
    val enrollments=Education32Data.enrollments(all).filter {it.studentId==student.id && !it.archived}
    var enrollmentId by remember(student.id,existing?.id){mutableStateOf(existing?.enrollmentId ?: enrollments.find {it.classroomId==initialClassroomId}?.id ?: enrollments.firstOrNull()?.id.orEmpty())}
    val enrollment=enrollments.find {it.id==enrollmentId}
    val offerings=Education32Data.offerings(all).filter {it.classroomId==enrollment?.classroomId}
    val templates=Education32Data.templates(all).filter {t->!t.archived && offerings.any {it.id==t.subjectId}}
    var templateId by remember(student.id,enrollmentId,existing?.id){mutableStateOf(existing?.templateId.orEmpty())}
    val template=templates.find {it.id==templateId}
    var criterionId by remember(student.id,templateId,existing?.id){mutableStateOf(existing?.criterionId.orEmpty())}
    var selected by remember(student.id,criterionId,existing?.id){mutableStateOf(existing?.observationIds.orEmpty())}
    var level by remember(student.id,existing?.id){mutableStateOf(existing?.level)}
    var teacher by remember(student.id,existing?.id){mutableStateOf(existing?.teacher.orEmpty())}
    var description by remember(student.id,existing?.id){mutableStateOf(existing?.description.orEmpty())}
    var confirm by remember(student.id,level,selected,teacher,description,templateId,criterionId){mutableStateOf(false)}
    val observations=Education32Data.timeline(all,student.id).filter {it.classroomId==enrollment?.classroomId && it.criterion.id==criterionId && it.status!=EduStatus.NOT_OBSERVED}
    val actions=rememberEducationActions();val repo=remember(store){Education32Repository(store)}
    EducationDialog("Valoración docente con instrumento",{if(!actions.busy)dismiss()}){
        EducationState(actions)
        Text(student.displayName,style=MaterialTheme.typography.titleMedium)
        Text("Selecciona instrumento, criterio y evidencias individuales. El estado rápido nunca se transforma automáticamente en AD/A/B/C.")
        EducationChoices(enrollments.map {it.id to "Matrícula ${it.schoolYear}"},enrollmentId){if(existing==null)enrollmentId=it}
        if(templates.isEmpty())Text("Primero crea un instrumento desde Aula → Áreas. También puedes registrar una evaluación manual independiente desde Aprendizaje.")
        EducationChoices(templates.map {it.id to "${offerings.find {s->s.id==it.subjectId}?.name.orEmpty()} · ${it.competency} · v${it.version}"},templateId){if(existing==null)templateId=it}
        template?.let {row->
            Text(row.sourceStatus,style=MaterialTheme.typography.labelSmall)
            EducationChoices(row.criteria.map {it.id to it.label},criterionId){if(existing==null)criterionId=it}
            row.criteria.find {it.id==criterionId}?.descriptors?.forEach {(key,value)->Text("$key · $value",style=MaterialTheme.typography.bodySmall)}
        }
        if(criterionId.isNotBlank() && observations.isEmpty())Text("Sin evidencia individual observable de este criterio. Puedes guardar un borrador sin asignar nivel; «no observado» no equivale a C.")
        observations.forEach {observation->
            Row {
                Checkbox(observation.id in selected,{checked->selected=if(checked)selected+observation.id else selected-observation.id})
                Text("${observation.criterion.label}\n${observation.status.label}\n${observation.note}",Modifier.weight(1f))
            }
            observation.evidenceIds.sorted().forEachIndexed {index,evidenceId->TextButton(onClick={reviewEvidence?.invoke(evidenceId)},enabled=reviewEvidence!=null){Text("Revisar evidencia privada ${index+1}")}}
        }
        EducationChoices(listOf("" to "Sin evaluar")+EduAchievementLevel.entries.map {it.name to it.name},level?.name.orEmpty()){level=it.takeIf(String::isNotBlank)?.let(EduAchievementLevel::valueOf)}
        OutlinedTextField(description,{description=it.take(6000)},label={Text("Conclusión descriptiva docente")},minLines=2,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(teacher,{teacher=it.take(100)},label={Text("Docente responsable")},modifier=Modifier.fillMaxWidth())
        Row {Checkbox(confirm,{confirm=it});Text("He revisado estas evidencias y confirmo el nivel elegido",Modifier.weight(1f))}
        Button(onClick={actions.perform {
            repo.saveJudgment(student.id,enrollmentId,templateId,criterionId,level,selected,teacher,description,true,existing?.id);dismiss()
        }},enabled=confirm && level!=null && selected.isNotEmpty() && teacher.isNotBlank() && description.isNotBlank() && template!=null && criterionId.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Confirmar y guardar valoración")}
        if(existing==null)OutlinedButton(onClick={actions.perform {
            repo.saveJudgment(student.id,enrollmentId,templateId,criterionId,level,selected,teacher,description,false);dismiss()
        }},enabled=template!=null && criterionId.isNotBlank() && !actions.busy,modifier=Modifier.fillMaxWidth()){Text("Guardar borrador sin confirmar")}
    }
}
