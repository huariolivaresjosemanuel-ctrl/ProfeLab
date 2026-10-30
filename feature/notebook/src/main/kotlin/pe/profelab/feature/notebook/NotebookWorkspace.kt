package pe.profelab.feature.notebook

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import java.io.File
import java.time.Instant
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.unit.dp

@Composable
fun NotebookWorkspace(all:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,export:(WorkRecord)->Unit,capture:(String)->Unit,initialId:String?=null,initialSection:String="Apuntes") {
    var section by rememberSaveable {mutableStateOf(initialSection)};var selected by rememberSaveable(initialId){mutableStateOf(initialId)}
    var query by rememberSaveable {mutableStateOf("")};var portfolio by rememberSaveable {mutableStateOf(false)}
    var remove by remember {mutableStateOf<WorkRecord?>(null)}
    val current=all.find {it.id==selected};val context=LocalContext.current
    var recorder by remember {mutableStateOf<MediaRecorder?>(null)};var audioId by remember {mutableStateOf<String?>(null)}
    var audioError by remember {mutableStateOf<String?>(null)}
    fun newNote(type:String=section):WorkRecord {val n=WorkRecord(newId(),"note","",JSONObject().put("noteType",type).put("date",Instant.now().toString()).toString(),System.currentTimeMillis());save(n);selected=n.id;return n}
    @Suppress("DEPRECATION")
    fun startAudio(){
        if(recorder!=null)return
        val id=newId();val file=File(context.filesDir,"documents/$id");file.parentFile?.mkdirs()
        var candidate:MediaRecorder?=null
        try {val r=MediaRecorder();candidate=r;r.setAudioSource(MediaRecorder.AudioSource.MIC);r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);r.setOutputFile(file.absolutePath);r.prepare();r.start();audioId=id;recorder=r;audioError=null}
        catch(_:Exception){runCatching {candidate?.release()};file.delete();audioError="No se pudo iniciar la grabación. Puedes escribir la nota."}
    }
    fun stopAudio(){
        val r=recorder ?: return;val id=audioId;recorder=null;audioId=null
        try {r.stop();require(id!=null && File(context.filesDir,"documents/$id").length()>0)
            val n=WorkRecord(newId(),"note","Nota de voz",JSONObject().put("noteType",section).put("date",Instant.now().toString()).put("attachments",JSONArray().put(JSONObject().put("id",id).put("mime","audio/mp4"))).toString(),System.currentTimeMillis());save(n);selected=n.id
        }catch(_:Exception){id?.let {File(context.filesDir,"documents/$it").delete()};audioError="La grabación no pudo guardarse. Intenta una toma más larga."}
        finally{runCatching {r.release()}}
    }
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){if(it)startAudio() else audioError="Micrófono denegado; puedes seguir escribiendo."}
    val owner=LocalLifecycleOwner.current
    val latestStop by rememberUpdatedState(::stopAudio)
    DisposableEffect(owner){val observer=LifecycleEventObserver {_,event->if(event==Lifecycle.Event.ON_STOP)latestStop()};owner.lifecycle.addObserver(observer)
        onDispose {owner.lifecycle.removeObserver(observer);recorder?.let {runCatching {it.stop()};runCatching {it.release()}};audioId?.let {File(context.filesDir,"documents/$it").delete()}}}
    if(current!=null) RecordEditor(current,listOf(Field("body","Texto / listas / reflexión",6),Field("tags","Etiquetas separadas por comas"),Field("date","Fecha"),Field("school","Institución de práctica (opcional)"),Field("schoolGrade","Grado escolar de práctica (1 a 6, independiente del ciclo)"),Field("observation","Observación anónima"),Field("reflection","Reflexión"),Field("nextSteps","Próximos pasos")),all,save,{selected=null},export,capture){data,change->
        ChoiceRow(listOf("Apuntes","Prácticas","Ideas"),data.optString("noteType","Apuntes")){change(JSONObject(data.toString()).put("noteType",it))}
        TextButton(onClick={val n=current.copy(id=newId(),title=current.title+" (copia)");save(n);selected=n.id}){Text("Duplicar nota")}
        TextButton(onClick={remove=current}){Text("Archivar nota")}
    } else if(portfolio) {
        val saved=all.find {it.kind=="portfolio" && !it.archived}
        var chosen by remember {mutableStateOf(saved?.let {val a=JSONObject(it.json).optJSONArray("links") ?: JSONArray();(0 until a.length()).map {a.getString(it)}.toSet()} ?: emptySet())}
        var includeImages by remember {mutableStateOf(saved?.let {JSONObject(it.json).optBoolean("exportImages")} ?: false)}
        Page {PageTitle("Portafolio","Selecciona solo trabajos que autorizas exportar",back={portfolio=false})
            all.filter {it.kind in setOf("note","project","experiment","session") && !it.archived}.forEach {r->Row {Checkbox(r.id in chosen,{checked->chosen=if(checked)chosen+r.id else chosen-r.id});Text(r.title.ifBlank {"Sin título"},Modifier.weight(1f))}}
            InfoCard("Revisión de privacidad","Revisa nombres y evidencias de los trabajos seleccionados. Los archivos de audio y PDFs anexos se mantienen vinculados; no se fusionan con el portafolio.")
            Row {Checkbox(includeImages,{includeImages=it});Text("Autorizo incluir las imágenes de los trabajos elegidos")}
            Button(onClick={val r=WorkRecord(saved?.id ?: newId(),"portfolio","Mi portafolio",JSONObject().put("links",JSONArray(chosen.toList())).put("exportImages",includeImages).toString(),System.currentTimeMillis());save(r);export(r)}){Text("Guardar portafolio / vista previa")}
        }
    } else Page {
        PageTitle("Cuaderno","Notas, prácticas y reflexiones")
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
            SoftCard("Nueva nota","Escribe y organiza tus ideas",Icons.Outlined.NoteAdd,ProfeColors.Mint,Modifier.weight(1f)){newNote()}
            SoftCard(if(recorder==null)"Grabar voz" else "Detener audio","Conserva tu voz en el dispositivo",Icons.Outlined.Mic,ProfeColors.Coral,Modifier.weight(1f)){if(recorder!=null)stopAudio()else if(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)startAudio()else permission.launch(Manifest.permission.RECORD_AUDIO)}
            SoftCard("Foto / OCR","Captura apuntes y documentos",Icons.Outlined.PhotoCamera,ProfeColors.Blue,Modifier.weight(1f)){val n=newNote();capture(n.id)}
        }
        Text("Audio local, sin transcripción. Al enviar la app al fondo se detiene y guarda la toma.",style=MaterialTheme.typography.bodySmall)
        audioError?.let {ErrorNotice(it)}
        ChoiceRow(listOf("Apuntes","Prácticas","Ideas"),section){section=it}
        OutlinedTextField(query,{query=it},label={Text("Buscar texto o etiquetas")},modifier=Modifier.fillMaxWidth())
        val shown=all.filter {it.kind=="note" && !it.archived && JSONObject(it.json).optString("noteType","Apuntes")==section && (it.title.contains(query,true)||it.json.contains(query,true))}
        shown.forEach {r->Card(onClick={selected=r.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){ListItem(headlineContent={Text(r.title.ifBlank {"Borrador sin título"})},supportingContent={Text(JSONObject(r.json).optString("body").take(160))})}}
        if(shown.isEmpty())InfoCard("$section · sin notas","Añade una nota para conservar tu reflexión.")
        Button(onClick={portfolio=true}){Text("Portafolio / agregar evidencias")}
    }
    remove?.let {r->AlertDialog(onDismissRequest={remove=null},title={Text("¿Archivar la nota?")},confirmButton={TextButton(onClick={archive(r.id);selected=null;remove=null}){Text("Archivar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
