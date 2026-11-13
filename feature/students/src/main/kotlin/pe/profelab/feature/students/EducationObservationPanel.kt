package pe.profelab.feature.students

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.newId

/** Content for the app-owned contextual Dialog. Closing never completes a step or changes stock. */
@Composable
fun EducationObservationPanel(record:WorkRecord,all:List<WorkRecord>,store:WorkspaceStore,stepId:String?,dismiss:()->Unit){
    val run=EducationData.selectedRun(record,all)
    Column(Modifier.fillMaxWidth().heightIn(max=520.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
    if(run==null){
        Text("No hay una ejecución docente seleccionada para esta copia. Puedes prepararla opcionalmente en Materiales → Aula y participantes. Registrar alumnos no es obligatorio.")
        TextButton(onClick=dismiss){Text("Volver a la actividad")}
    }else {
        EducationRunReview(run,all,store,stepId)
        TextButton(onClick=dismiss){Text("Continuar la actividad")}
    }
    }
}

@Composable
internal fun EducationRunReview(run:EduRun,all:List<WorkRecord>,store:WorkspaceStore,stepId:String?){
    var visibleLimit by rememberSaveable(run.id){mutableIntStateOf(30)}
    var newObservation by rememberSaveable(run.id){mutableStateOf(false)};var editId by rememberSaveable(run.id){mutableStateOf<String?>(null)};var feedbackTarget by remember {mutableStateOf<EduTarget?>(null)};var archiveId by remember {mutableStateOf<String?>(null)}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    val classroom=EducationData.classrooms(all).find {it.id==run.classroomId};val observations=EducationData.observations(all).filter {it.runId==run.id && !it.archived}.sortedByDescending {it.at}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text(run.activityTitle,style=MaterialTheme.typography.titleMedium)
        Text("${run.classroomLabel.ifBlank {classroom?.name ?: "Sin nombres"}} · ${run.participantCount} participantes · versión ${run.contentVersion}")
        Text("Observación formativa: no genera calificaciones oficiales. Un registro de equipo no equivale al desempeño individual.",style=MaterialTheme.typography.bodySmall)
        if(stepId!=null && stepId !in run.stepIds)Text("El paso actual cambió respecto de esta ejecución. El nuevo registro será general de la actividad.")
        EducationState(actions)
        if(!run.archived)Button(onClick={newObservation=true},modifier=Modifier.fillMaxWidth()){Text(if(stepId!=null && stepId in run.stepIds)"Registrar observación de este paso"else "Registrar observación de la actividad")}
        if(observations.isEmpty())Text("Sin observaciones registradas; esto no indica desaprobación ni bloquea la actividad.")
        observations.take(visibleLimit).forEach {observation->EducationCard(targetLabel(run,observation.target)){
            Text(observation.criterion.label);Text(observation.status.label)
            observation.stepId?.let {Text("Paso: $it",style=MaterialTheme.typography.bodySmall)}
            if(observation.note.isNotBlank())Text(observation.note)
            Text(java.time.Instant.ofEpochMilli(observation.at).atZone(java.time.ZoneId.systemDefault()).toLocalDate().toString(),style=MaterialTheme.typography.bodySmall)
            if(!run.archived){TextButton(onClick={editId=observation.id}){Text("Editar registro")};TextButton(onClick={archiveId=observation.id}){Text("Archivar observación")}}
        }}
        if(observations.size>visibleLimit)TextButton(onClick={visibleLimit+=30}){Text("Mostrar más registros (${visibleLimit} de ${observations.size})")}
        if(observations.isNotEmpty()){
            Text("Retroalimentación a partir de registros guardados",style=MaterialTheme.typography.titleSmall)
            observations.map {it.target}.distinct().forEach {target->TextButton(onClick={feedbackTarget=target}){Text("Revisar resumen · ${targetLabel(run,target)}")}}
        }
    }
    if(newObservation)ObservationEditor(run,all,store,stepId?.takeIf {it in run.stepIds},null){newObservation=false}
    editId?.let {id->observations.find {it.id==id}?.let {row->ObservationEditor(run,all,store,row.stepId,row){editId=null}}}
    feedbackTarget?.let {target->EducationFeedbackDialog(EducationFeedback.summary(run,target,observations)){feedbackTarget=null}}
    archiveId?.let {id->AlertDialog(onDismissRequest={archiveId=null},title={Text("¿Archivar esta observación?")},text={Text("Dejará de formar parte del resumen activo; el registro privado permanece en el respaldo.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(id);archiveId=null}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archiveId=null}){Text("Cancelar")}})}
}

internal fun targetLabel(run:EduRun,target:EduTarget):String=when(target.kind){
    EduTargetKind.STUDENT->run.participants.find {it.studentId==target.id}?.displayName ?: "Participante de esta ejecución"
    EduTargetKind.TEAM->"Equipo: "+(run.teams.find {it.teamId==target.id}?.name ?: "equipo de esta ejecución")
    EduTargetKind.ACTIVITY->"Registro general de actividad"
}

@Composable
private fun ObservationEditor(run:EduRun,all:List<WorkRecord>,store:WorkspaceStore,stepId:String?,existing:EduObservation?,dismiss:()->Unit){
    val source=all.find {it.id==run.personalProjectId};val criteria=(listOfNotNull(existing?.criterion)+source?.let {WorkshopFlowAdapter.read(it).criteria.map {c->EduCriterion(c.id,c.label)}}.orEmpty()).distinctBy {it.id}
    var kind by rememberSaveable(run.id,existing?.id){mutableStateOf((existing?.target?.kind ?: if(run.participants.isNotEmpty())EduTargetKind.STUDENT else EduTargetKind.ACTIVITY).name)}
    var targetId by rememberSaveable(run.id,existing?.id,kind){mutableStateOf(existing?.target?.id ?: when(EduTargetKind.valueOf(kind)){EduTargetKind.STUDENT->run.participants.firstOrNull()?.studentId;EduTargetKind.TEAM->run.teams.firstOrNull()?.teamId;else->null}.orEmpty())}
    var criterionId by rememberSaveable(run.id,existing?.id){mutableStateOf(existing?.criterion?.id ?: criteria.firstOrNull()?.id.orEmpty())};var customCriterion by rememberSaveable(run.id,existing?.id){mutableStateOf(existing?.criterion?.label.orEmpty())};val customId=rememberSaveable(run.id,existing?.id){newId()}
    var status by rememberSaveable(run.id,existing?.id){mutableStateOf((existing?.status ?: EduStatus.NOT_OBSERVED).name)};var note by rememberSaveable(run.id,existing?.id){mutableStateOf(existing?.note.orEmpty())}
    var selectedEvidence by rememberSaveable(run.id,existing?.id){mutableStateOf(JSONArray(existing?.evidenceIds.orEmpty().toList()).toString())}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    EducationDialog("Observación formativa",{if(!actions.busy)dismiss()}){
        EducationState(actions)
        if(existing==null){
            val kinds=listOfNotNull(if(run.participants.isNotEmpty())EduTargetKind.STUDENT.name to "Estudiante"else null,if(run.teams.isNotEmpty())EduTargetKind.TEAM.name to "Equipo"else null,EduTargetKind.ACTIVITY.name to "Actividad general")
            EducationChoices(kinds,kind){kind=it}
            val targets=when(EduTargetKind.valueOf(kind)){EduTargetKind.STUDENT->run.participants.map {it.studentId to it.displayName};EduTargetKind.TEAM->run.teams.map {it.teamId to it.name};else->emptyList()}
            if(targets.isNotEmpty())EducationChoices(targets,targetId){targetId=it}
        }else Text(targetLabel(run,existing.target))
        if(kind==EduTargetKind.TEAM.name)Text("Se guarda solo para el equipo; no se copian resultados a cada estudiante.")
        if(criteria.isNotEmpty())EducationChoices(criteria.map {it.id to it.label}+listOf("" to "Otro criterio observable"),criterionId){criterionId=it}
        val picked=criteria.find {it.id==criterionId}
        if(picked==null)OutlinedTextField(customCriterion,{customCriterion=it.take(1000)},label={Text("Criterio observable propuesto por el docente")},modifier=Modifier.fillMaxWidth())else Text(picked.label)
        EducationChoices(EduStatus.entries.map {it.name to it.label},status){status=it}
        Text("«No observado» no equivale a desaprobación.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(note,{note=it.take(6000)},label={Text("Observación breve, sin diagnósticos")},minLines=2,modifier=Modifier.fillMaxWidth())
        (EducationData.phrases(all).filter {!it.archived}.map {it.text}.ifEmpty {EducationData.suggestedPhrases}).forEach {phrase->TextButton(onClick={note=listOf(note,phrase).filter {it.isNotBlank()}.joinToString("\n").take(6000)}){Text(phrase)}}
        val currentEvidence=source?.let {WorkshopEvidenceData.privateImageIds(JSONObject(it.json))}.orEmpty()
        val evidence=(currentEvidence+existing?.evidenceIds.orEmpty()).distinct();val selected=EducationData.strings(JSONArray(selectedEvidence))
        if(evidence.isNotEmpty())Text("Evidencias privadas ya adjuntadas a esta copia · opcional")
        evidence.forEachIndexed {i,id->Row {Checkbox(id in selected,{checked->selectedEvidence=JSONArray((if(checked)selected+id else selected-id).toList()).toString()});Text("Fotografía privada ${i+1}"+if(id !in currentEvidence)" · vínculo histórico"else "",Modifier.weight(1f))}}
        Button(onClick={actions.perform {
            val criterion=picked ?: EduCriterion(existing?.criterion?.id ?: customId,customCriterion)
            val target=existing?.target ?: EduTarget(EduTargetKind.valueOf(kind),targetId.takeIf {it.isNotBlank() && kind!=EduTargetKind.ACTIVITY.name})
            repo.saveObservation(run.id,target,criterion,EduStatus.valueOf(status),note,stepId,existing?.id ?: newId(),EducationData.strings(JSONArray(selectedEvidence)));dismiss()
        }},enabled=!actions.busy && (picked!=null || customCriterion.isNotBlank()),modifier=Modifier.fillMaxWidth()){Text("Guardar observación")}
    }
}
