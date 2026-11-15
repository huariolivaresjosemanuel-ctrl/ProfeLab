package pe.profelab.feature.students.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.ui.LocalReviewPrivateEvidence
import pe.profelab.feature.students.*
import pe.profelab.feature.students.students.label
import java.time.Instant
import java.time.ZoneId

@Composable
internal fun ColumnScope.StudentTimeline(studentId:String,all:List<WorkRecord>){
    val reviewEvidence=LocalReviewPrivateEvidence.current
    var subjectId by remember(studentId){mutableStateOf("")}
    var competency by remember(studentId){mutableStateOf("")}
    var period by remember(studentId){mutableStateOf("")}
    val assessments=Education32Data.academicAssessments(all).filter {it.studentId==studentId && !it.archived}
    val observations=Education32Data.timeline(all,studentId)
    val historicalObservations=EducationData.observations(all).filter {it.target==EduTarget(EduTargetKind.STUDENT,studentId)}
    val judgments=Education32Data.judgments(all).filter {it.studentId==studentId && !it.archived}
    val offerings=Education32Data.offerings(all).associateBy {it.id}
    val templates=Education32Data.templates(all).associateBy {it.id}
    val runs=EducationData.runs(all).associateBy {it.id}
    val subjectIds=(assessments.map {it.subjectId}+judgments.mapNotNull {templates[it.templateId]?.subjectId}).toSet()
    val competencies=(assessments.map {it.competency}+judgments.mapNotNull {templates[it.templateId]?.competency}).distinct().sorted()
    EducationChoices(listOf("" to "Todas las áreas")+subjectIds.map {it to (offerings[it]?.name ?: "Área histórica")},subjectId){subjectId=it}
    EducationChoices(listOf("" to "Todas las competencias")+competencies.map {it to it},competency){competency=it}
    EducationChoices(listOf("" to "Todos los periodos")+assessments.map {it.period}.distinct().sorted().map {it to it},period){period=it}
    val shownAssessments=assessments.filter {(subjectId.isBlank() || it.subjectId==subjectId) && (competency.isBlank() || it.competency==competency) && (period.isBlank() || it.period==period)}
    val shownJudgments=judgments.filter {row->val template=templates[row.templateId];(subjectId.isBlank() || template?.subjectId==subjectId) && (competency.isBlank() || template?.competency==competency) && period.isBlank()}
    val shownObservations=if(subjectId.isBlank() && competency.isBlank() && period.isBlank())observations else emptyList()
    val entries=(shownAssessments.map {it.createdAt to "academic:${it.id}"}+shownJudgments.map {it.createdAt to "judgment:${it.id}"}+shownObservations.map {it.at to "observation:${it.id}"}).sortedByDescending {it.first}
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)){
        item("info"){Text("Registros reales, sin promedios ni conversiones de escala. Los registros de equipo no se atribuyen a esta ficha.",style=MaterialTheme.typography.bodySmall)}
        if(entries.isEmpty())item("empty"){Text("Sin registros para esta selección. Esto no indica dificultad ni desaprobación.")}
        entries.forEach {(at,key)->item(key){
            val id=key.substringAfter(':')
            val date=Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
            when {
                key.startsWith("academic:")->{val row=shownAssessments.single {it.id==id};EducationCard("${row.taskKind.label} · $date"){
                    Text("${offerings[row.subjectId]?.name ?: "Área histórica"} · ${row.period}")
                    Text(row.competency);Text("Criterio: ${row.criterion}")
                    Text("${row.scale.label}: ${row.value}",style=MaterialTheme.typography.titleMedium)
                    Text("Evidencia revisada: ${row.evidence}")
                    if(row.observation.isNotBlank())Text("Observación docente: ${row.observation}")
                    if(row.improvementAction.isNotBlank())Text("Acción elegida por el docente: ${row.improvementAction}")
                    Text("Origen: ${when(row.origin){EduAssessmentOrigin.MANUAL->"Registro manual";EduAssessmentOrigin.WORKSHOP->"Taller";EduAssessmentOrigin.SESSION->"Sesión"}} · ${row.teacher}",style=MaterialTheme.typography.bodySmall)
                }}
                key.startsWith("judgment:")->{val row=shownJudgments.single {it.id==id};val template=templates[row.templateId];EducationCard("Valoración de competencia · $date"){
                    Text(template?.competency ?: "Competencia histórica");Text("${row.level?.name ?: "Sin evaluar"} · ${row.state.label()}")
                    Text(row.description);Text("Instrumento v${template?.version ?: "?"} · ${row.teacher}",style=MaterialTheme.typography.bodySmall)
                    if(row.evidenceSummary.isNotBlank())Text("Evidencias al confirmar:\n${row.evidenceSummary}")
                    val evidence=(if(row.observationSnapshots.isNotEmpty())row.evidenceIds else historicalObservations.filter {it.id in row.observationIds}.flatMap {it.evidenceIds}.toSet()).sorted()
                    evidence.forEachIndexed {index,evidenceId->TextButton(onClick={reviewEvidence?.invoke(evidenceId)},enabled=reviewEvidence!=null){Text("Revisar evidencia privada ${index+1}")}}
                }}
                else->{val row=shownObservations.single {it.id==id};val run=runs[row.runId];EducationCard("Taller · $date"){
                    Text(run?.activityTitle ?: "Actividad histórica");Text(run?.classroomLabel.orEmpty(),style=MaterialTheme.typography.bodySmall)
                    Text(row.criterion.label);Text("Observación rápida: ${row.status.label}")
                    if(row.note.isNotBlank())Text(row.note)
                    Text("${row.evidenceIds.size} vínculos de evidencia privada · no equivale a AD/A/B/C",style=MaterialTheme.typography.bodySmall)
                    row.evidenceIds.sorted().forEachIndexed {index,evidenceId->TextButton(onClick={reviewEvidence?.invoke(evidenceId)},enabled=reviewEvidence!=null){Text("Revisar evidencia privada ${index+1}")}}
                }}
            }
        }}
    }
}
