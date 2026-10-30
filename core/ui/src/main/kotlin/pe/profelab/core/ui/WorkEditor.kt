package pe.profelab.core.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.*
import pe.profelab.core.files.DocumentFiles
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.content.Intent

@Composable
fun RecordEditor(record: WorkRecord, fields: List<Field>, all: List<WorkRecord>, save:(WorkRecord)->Unit,
    close:()->Unit, export:(WorkRecord)->Unit, capture:(String)->Unit,
    extra: @Composable (JSONObject,(JSONObject)->Unit)->Unit={_,_->}) {
    var data by remember(record.id) {mutableStateOf(JSONObject(record.json))}
    var title by remember(record.id) {mutableStateOf(record.title)}
    val context=LocalContext.current;val scope=rememberCoroutineScope();var error by remember {mutableStateOf<String?>(null)}
    fun persist(next:JSONObject=data, name:String=title){data=next;title=name;save(record.copy(title=name,json=next.toString()))}
    val attach=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->if(uri!=null) scope.launch {
        try {
            val mime=context.contentResolver.getType(uri) ?: "application/octet-stream"
            require(mime.startsWith("image/") || mime.startsWith("audio/") || mime=="application/pdf")
            val f=DocumentFiles(context).importUri(uri.toString(),mime,30L*1024*1024)
            val next=JSONObject(data.toString());val files=next.optJSONArray("attachments") ?: JSONArray()
            files.put(JSONObject().put("id",f.id).put("mime",mime));next.put("attachments",files);persist(next)
        }catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo adjuntar el archivo. Revisa formato y espacio disponible."}
    }}
    Page {
        PageTitle(if(title.isBlank())"Nuevo trabajo" else title,back=close)
        error?.let {ErrorNotice(it)}
        Text("Los cambios se guardan en este dispositivo. Comprueba los avisos de guardado antes de salir.",style=MaterialTheme.typography.bodySmall)
        OutlinedTextField(title,{persist(name=it.take(160))},label={Text("Título")},modifier=Modifier.fillMaxWidth())
        BoxWithConstraints {
            if(maxWidth>=700.dp)Row(horizontalArrangement=Arrangement.spacedBy(20.dp)){
                Column(Modifier.weight(2f),verticalArrangement=Arrangement.spacedBy(12.dp)){JsonFields(data,fields){persist(it)};extra(data){persist(it)}}
                Column(Modifier.weight(1f)){Text("Recursos vinculados",style=MaterialTheme.typography.titleMedium);RecordLinks(all.filter {it.id!=record.id},data){persist(it)}}
            }else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){JsonFields(data,fields){persist(it)};extra(data){persist(it)};RecordLinks(all.filter {it.id!=record.id},data){persist(it)}}
        }
        val openPage=LocalOpenDocumentPage.current
        data.optJSONArray("citations")?.let {citations->for(i in 0 until citations.length()){val c=citations.getJSONObject(i);val id=c.optString("documentId");TextButton(enabled=openPage!=null,onClick={openPage?.invoke(id,c.optInt("pageIndex"))}){Text("Abrir cita: ${all.find {it.id==id}?.title ?: "Origen no disponible"} · página ${c.optInt("pageIndex")+1}")}}}
        TextButton(onClick={attach.launch(arrayOf("image/*","audio/*","application/pdf"))}){Text("Adjuntar foto, audio o PDF")}
        TextButton(onClick={capture(record.id)}){Text("Foto / OCR para este trabajo")}
        TextButton(onClick={save(record.copy(id="history:${record.id}:${System.currentTimeMillis()}",kind="revision",title=title,json=data.toString(),archived=true))}){Text("Guardar versión recuperable")}
        all.filter {it.kind=="revision" && it.id.startsWith("history:${record.id}:")}.sortedByDescending {it.updatedAt}.take(10).forEach {version->
            TextButton(onClick={persist(JSONObject(version.json),version.title)}){Text("Recuperar versión · ${java.time.Instant.ofEpochMilli(version.updatedAt)}")}
        }
        val attachments=data.optJSONArray("attachments") ?: JSONArray()
        for(i in 0 until attachments.length()) {
            val a=attachments.getJSONObject(i)
            TextButton(onClick={
                try {
                    val f=DocumentFiles(context).file(a.getString("id"))
                    val uri=androidx.core.content.FileProvider.getUriForFile(context,"${context.packageName}.files",f)
                    context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri,a.getString("mime")).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                }catch(_:Exception){error="No hay una aplicación compatible o falta el archivo."}
            }){Text("Abrir adjunto ${i+1} · ${a.optString("mime")}")}
            TextButton(onClick={val next=JSONArray();(0 until attachments.length()).filter {it!=i}.forEach {next.put(attachments.get(it))};persist(JSONObject(data.toString()).put("attachments",next))}){Text("Quitar vínculo al adjunto ${i+1}")}
        }
        InfoCard("Privacidad","Evita datos identificables de menores. Revisa todos los textos y anexos antes de exportar.")
        Row {Checkbox(data.optBoolean("exportImages"),{persist(JSONObject(data.toString()).put("exportImages",it))});Text("Incluir mis imágenes adjuntas en la copia PDF")}
        Button(onClick={export(record.copy(title=title,json=data.toString()))}){Text("Vista previa / exportar PDF")}
    }
}

@Composable
fun ReorderSteps(data:JSONObject,changed:(JSONObject)->Unit) {
    val steps=data.optJSONArray("steps") ?: JSONArray()
    var nextText by remember {mutableStateOf("")}
    Text("Procedimiento",style=MaterialTheme.typography.titleMedium)
    for(i in 0 until steps.length()) {
        val raw=steps.get(i);val step=if(raw is JSONObject) raw else JSONObject().put("title",raw.toString())
        Row {Checkbox(step.optBoolean("done"),{done->val next=JSONArray(steps.toString());next.put(i,JSONObject(step.toString()).put("done",done));changed(JSONObject(data.toString()).put("steps",next))});OutlinedTextField(step.optString("title"),{text->val next=JSONArray(steps.toString());next.put(i,JSONObject(step.toString()).put("title",text));changed(JSONObject(data.toString()).put("steps",next))},label={Text("Paso ${i+1}")},modifier=Modifier.weight(1f))}
        Row {
            if(i>0) TextButton(onClick={val next=JSONArray(steps.toString());val old=next.get(i-1);next.put(i-1,next.get(i));next.put(i,old);changed(JSONObject(data.toString()).put("steps",next))}){Text("Subir")}
            TextButton(onClick={val next=JSONArray();(0 until steps.length()).filter {it!=i}.forEach {next.put(steps.get(it))};changed(JSONObject(data.toString()).put("steps",next))}){Text("Quitar paso")}
        }
    }
    OutlinedTextField(nextText,{nextText=it},label={Text("Añadir un paso")},modifier=Modifier.fillMaxWidth())
    TextButton(onClick={if(nextText.isNotBlank()){changed(JSONObject(data.toString()).put("steps",JSONArray(steps.toString()).put(JSONObject().put("title",nextText).put("done",false))));nextText=""}}){Text("Añadir paso")}
}
