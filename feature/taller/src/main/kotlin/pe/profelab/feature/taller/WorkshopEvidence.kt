package pe.profelab.feature.taller

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.files.DocumentFiles
import pe.profelab.core.model.*
import java.time.Instant
import java.time.LocalDate

@Composable
internal fun WorkshopEvidence(record:WorkRecord,vm:WorkshopViewModel,capture:(String)->Unit){
    val context=LocalContext.current;val scope=rememberCoroutineScope();var error by remember {mutableStateOf<String?>(null)}
    var remove by remember {mutableStateOf<String?>(null)}
    val attach=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)scope.launch {
        try {
            val mime=context.contentResolver.getType(uri).orEmpty();require(mime.startsWith("image/"))
            val copied=DocumentFiles(context).importUri(uri.toString(),mime,30L*1024*1024)
            vm.patch(record){data->val a=JSONArray((data.optJSONArray("attachments") ?: JSONArray()).toString());a.put(JSONObject().put("id",copied.id).put("mime",mime).put("at",Instant.now().toString()));data.put("attachments",a)}
        }catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo adjuntar la imagen. Revisa formato, acceso y espacio."}
    }}
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Fotografías y bocetos privados. Evita nombres, rostros o datos identificables de menores.",style=MaterialTheme.typography.bodySmall)
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={scope.launch {if(vm.flush())attach.launch(arrayOf("image/*"))else error="Resuelve el guardado antes de adjuntar."}},modifier=Modifier.fillMaxWidth()){Text("Añadir foto o boceto del dispositivo")}
        OutlinedButton(onClick={scope.launch {if(vm.flush())capture(record.id)else error="Resuelve el guardado antes de abrir la cámara."}},modifier=Modifier.fillMaxWidth()){Text("Cámara / OCR para este proyecto")}
        val images=projectImages(record).filter {it.label.startsWith("Fotografía")}
        androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(10.dp)){items(count=images.size,key={images[it].id}){index->val image=images[index];Column(Modifier.width(200.dp)){WorkshopPicture(image,Modifier.fillMaxWidth().height(170.dp));TextButton(onClick={remove=image.id}){Text("Quitar vínculo a esta evidencia")}}}}
        val data=JSONObject(record.json)
        Row(verticalAlignment=Alignment.CenterVertically){Checkbox(data.optBoolean("exportImages"),{checked->vm.patch(record){it.put("exportImages",checked)}});Text("Incluir mis imágenes en la copia PDF",modifier=Modifier.weight(1f))}
    }
    remove?.let {id->AlertDialog(onDismissRequest={remove=null},title={Text("¿Quitar el vínculo a esta evidencia?")},text={Text("Se quita de esta creación y de sus observaciones. El archivo privado se conserva para no destruir otros trabajos que lo utilicen.")},confirmButton={TextButton(onClick={vm.patch(record){j->
        val old=j.optJSONArray("attachments") ?: JSONArray();val next=JSONArray()
        for(i in 0 until old.length()){val a=old.getJSONObject(i);if(a.optString("id")!=id)next.put(a)}
        j.put("attachments",next)
        val entries=j.optJSONArray("records") ?: JSONArray()
        for(i in 0 until entries.length()){val observation=entries.optJSONObject(i) ?: continue
            observation.put("photoAttachmentIds",JSONArray(pe.profelab.core.content.WorkshopCodec.strings(observation.opt("photoAttachmentIds")).filterNot {it==id}))
        }
    };remove=null}){Text("Quitar vínculo")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
@Composable
internal fun WorkshopObservations(record:WorkRecord,vm:WorkshopViewModel){
    var date by rememberSaveable(record.id){mutableStateOf(LocalDate.now().toString())};var note by rememberSaveable(record.id){mutableStateOf("")}
    var measure by rememberSaveable(record.id){mutableStateOf("")};var unit by rememberSaveable(record.id){mutableStateOf("")};var moment by rememberSaveable(record.id){mutableStateOf("Durante")}
    var error by remember {mutableStateOf<String?>(null)};var remove by remember {mutableStateOf<Int?>(null)}
    var chosenPhotos by rememberSaveable(record.id){mutableStateOf("[]")}
    val data=JSONObject(record.json);val observations=data.optJSONArray("records") ?: JSONArray()
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        Text("Registra lo que ocurrió realmente; no se precargan resultados.",style=MaterialTheme.typography.bodySmall)
        WorkshopChoices(listOf("Antes","Durante","Después"),moment){moment=it}
        OutlinedTextField(date,{date=it},label={Text("Fecha (AAAA-MM-DD)")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(note,{note=it.take(10000)},label={Text("Observación anónima")},minLines=2,modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(measure,{measure=it},label={Text("Medida (opcional)")},modifier=Modifier.weight(1f));OutlinedTextField(unit,{unit=it},label={Text("Unidad")},modifier=Modifier.weight(1f))}
        val photos=projectImages(record).filter {it.label.startsWith("Fotografía")};val selected=pe.profelab.core.content.WorkshopCodec.strings(JSONArray(chosenPhotos)).toSet()
        if(photos.isNotEmpty())Text("Vincular evidencias ya añadidas a este registro",style=MaterialTheme.typography.bodySmall)
        photos.forEachIndexed {index,photo->Row {Checkbox(photo.id in selected,{checked->chosenPhotos=JSONArray((if(checked)selected+photo.id else selected-photo.id).toList()).toString()});Text("Fotografía / boceto ${index+1}")}}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(onClick={
            val validDate=runCatching {LocalDate.parse(date)}.isSuccess
            val validMeasure=measure.isBlank() || (measure.replace(',','.').toBigDecimalOrNull()!=null && unit.isNotBlank())
            if(!validDate || !validMeasure || (note.isBlank() && measure.isBlank()))error="Revisa la fecha, escribe una observación o una medida con unidad."
            else {val entry=JSONObject().put("id",newId()).put("at",Instant.now().toString()).put("date",date).put("moment",moment).put("observation",note).put("measurement",measure).put("measurementUnit",unit).put("photoAttachmentIds",JSONArray(selected.toList()))
                vm.patch(record){j->j.put("records",JSONArray((j.optJSONArray("records") ?: JSONArray()).toString()).put(entry))};note="";measure="";unit="";chosenPhotos="[]";error=null}
        }){Text("Guardar registro con fecha")}
        val dated=(0 until observations.length()).sortedBy {observations.optJSONObject(it)?.optString("date",observations.optJSONObject(it)?.optString("at").orEmpty()).orEmpty()}
        for(i in dated){
            val o=observations.optJSONObject(i) ?: continue
            WorkshopCard(o.optString("date",o.optString("at")),WorkshopColors.Paper){
                Text(o.optString("moment"));Text(o.optString("observation"))
                if(o.optString("measurement").isNotBlank())Text("${o.optString("measurement")} ${o.optString("measurementUnit")}")
                val linked=pe.profelab.core.content.WorkshopCodec.strings(o.opt("photoAttachmentIds")).distinct()
                if(linked.isNotEmpty())androidx.compose.foundation.lazy.LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)){items(count=linked.size,key={linked[it]}){i->WorkshopPicture(WorkshopImage(linked[i],label="Evidencia vinculada a este registro"),Modifier.width(150.dp).height(130.dp))}}
                TextButton(onClick={remove=i}){Text("Quitar registro")}
            }
        }
    }
    remove?.let {index->AlertDialog(onDismissRequest={remove=null},title={Text("¿Quitar este registro?")},confirmButton={TextButton(onClick={vm.patch(record){j->val a=j.optJSONArray("records") ?: JSONArray();val next=JSONArray();for(i in 0 until a.length())if(i!=index)next.put(a.get(i));j.put("records",next)};remove=null}){Text("Quitar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
