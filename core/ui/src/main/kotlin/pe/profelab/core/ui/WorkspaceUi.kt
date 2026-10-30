package pe.profelab.core.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import pe.profelab.core.model.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color

val LocalOpenResource=staticCompositionLocalOf<((String)->Unit)?> {null}
val LocalOpenDocumentPage=staticCompositionLocalOf<((String,Int)->Unit)?> {null}

class WorkspaceViewModel(val store: WorkspaceStore): ViewModel() {
    private val queue=Channel<suspend ()->Unit>(Channel.UNLIMITED)
    private val _error=MutableStateFlow<String?>(null)
    val error=_error.asStateFlow()
    val records=store.observeRecords().catch { _error.value="No se pudieron leer los trabajos locales";emit(emptyList()) }
        .stateIn(viewModelScope,SharingStarted.WhileSubscribed(5000),emptyList())
    init { viewModelScope.launch { for(op in queue) try {op()} catch(e: CancellationException){throw e} catch(_:Exception){_error.value="No se pudo guardar. El último guardado se conserva; reintenta antes de salir."} } }
    fun save(record: WorkRecord) { queue.trySend {store.saveRecord(record.copy(updatedAt=System.currentTimeMillis()));_error.value=null} }
    fun archive(id: String) {queue.trySend {store.archiveRecord(id)}}
    fun dismissError(){_error.value=null}
    fun duplicate(record: WorkRecord) {save(record.copy(id=newId(),title=record.title+" (copia)",revision=1,archived=false))}
}
data class Field(val key: String,val label: String,val lines: Int=2)
@Composable
fun JsonFields(data: JSONObject, fields: List<Field>, changed: (JSONObject)->Unit) {
    fields.forEach { field ->
        OutlinedTextField(value=data.optString(field.key),onValueChange={ value ->
            changed(JSONObject(data.toString()).put(field.key,value.take(20000)))
        },label={Text(field.label)},minLines=field.lines,modifier=Modifier.fillMaxWidth())
    }
}
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChoiceRow(options: List<String>, value: String, choose:(String)->Unit) {
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {options.forEach {option->FilterChip(selected=value==option,onClick={choose(option)},label={Text(option)},modifier=Modifier.heightIn(min=48.dp),colors=FilterChipDefaults.filterChipColors(selectedContainerColor=ProfeColors.Green,selectedLabelColor=Color.White))}}
}
@Composable
fun RecordLinks(all: List<WorkRecord>, data: JSONObject, changed: (JSONObject)->Unit) {
    val open=LocalOpenResource.current
    var show by remember {mutableStateOf(false)}
    TextButton(onClick={show=!show}){Text("Vincular recursos por ID")}
    val links=data.optJSONArray("links") ?: org.json.JSONArray()
    (0 until links.length()).forEach { i ->
        val id=links.getString(i);val target=all.find {it.id==id}
        TextButton(enabled=target!=null && open!=null,onClick={open?.invoke(id)}){Text("Abrir: ${target?.title ?: "origen no disponible"}")}
        TextButton(onClick={val next=org.json.JSONArray();(0 until links.length()).filter {it!=i}.forEach {next.put(links.getString(it))};changed(JSONObject(data.toString()).put("links",next))}) {Text("${target?.title ?: "Origen no disponible"} · quitar vínculo")}
    }
    if(show) all.filter {!it.archived && it.kind in setOf("project","experiment","session","note","document")}.take(100).forEach {r->
        TextButton(onClick={val ids=(0 until links.length()).map {links.getString(it)};if(r.id !in ids) changed(JSONObject(data.toString()).put("links",org.json.JSONArray(ids+r.id)))}){Text("${r.kind}: ${r.title}")}
    }
}
