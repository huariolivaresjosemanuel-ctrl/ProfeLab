package pe.profelab.feature.students

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

/** Existing observation phrases and anonymous runs remain accessible without a classroom. */
@Composable
internal fun ColumnScope.EducationTools(all:List<WorkRecord>,store:WorkspaceStore){
    var tab by rememberSaveable {mutableStateOf("runs")}
    var editor by rememberSaveable {mutableStateOf(false)}
    var editorId by rememberSaveable {mutableStateOf<String?>(null)}
    var archiveId by rememberSaveable {mutableStateOf<String?>(null)}
    var runId by rememberSaveable {mutableStateOf<String?>(null)}
    var showArchived by rememberSaveable {mutableStateOf(false)}
    val actions=rememberEducationActions();val repo=remember(store){EducationRepository(store)}
    EducationChoices(listOf("runs" to "Sin nombres","phrases" to "Frases frecuentes"),tab){tab=it}
    EducationState(actions)
    Row {Checkbox(showArchived,{showArchived=it});Text("Mostrar registros archivados",Modifier.padding(top=12.dp))}
    LazyColumn(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(bottom=16.dp)){
        if(tab=="runs"){
            val runs=EducationData.runs(all).filter {it.classroomId==null && (!it.archived || showArchived)}.sortedByDescending {it.createdAt}
            if(runs.isEmpty())item("empty"){Text("Sin ejecuciones registradas. Puedes preparar Taller solo con una cantidad de participantes, sin guardar nombres.")}
            items(runs,key={it.id}){run->EducationCard(run.activityTitle){Text("${run.participantCount} participantes · versión ${run.contentVersion}");TextButton(onClick={runId=run.id}){Text("Observaciones y retroalimentación")};if(!run.archived)TextButton(onClick={archiveId=run.id}){Text("Archivar ejecución")}}}
        }else {
            item("create"){Button(onClick={editor=true;editorId=null},modifier=Modifier.fillMaxWidth()){Text("Añadir observación frecuente")};Text("Cada sugerencia se puede adaptar antes de guardar una observación.")}
            items(EducationData.phrases(all).filter {!it.archived || showArchived},key={it.id}){phrase->EducationCard(phrase.text){if(!phrase.archived){TextButton(onClick={editorId=phrase.id;editor=true}){Text("Editar texto")};TextButton(onClick={archiveId=phrase.id}){Text("Archivar frase")}}else Text("Frase archivada · historial conservado")}}
        }
    }
    if(editor)EducationEditor("phrase",editorId,null,all,store){editor=false}
    archiveId?.let {id->AlertDialog(onDismissRequest={archiveId=null},title={Text("¿Archivar conservando el historial?")},text={Text("Las observaciones y ejecuciones anteriores conservan sus datos.")},confirmButton={TextButton(onClick={actions.perform {repo.archive(id);archiveId=null}},enabled=!actions.busy){Text("Archivar")}},dismissButton={TextButton(onClick={archiveId=null}){Text("Cancelar")}})}
    runId?.let {id->EducationData.runs(all).find {it.id==id}?.let {run->EducationDialog("Seguimiento privado",{runId=null}){EducationRunReview(run,all,store,null)}}}
}
