package pe.profelab.feature.library

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import org.json.JSONObject

@Composable
fun LibraryWorkspace(sources:List<SourceReference>,all:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,
    importPdf:(String?)->Unit,open:(WorkRecord)->Unit,openSource:(String)->Unit,capture:()->Unit,available:(WorkRecord)->Boolean) {
    var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableStateOf("MINEDU")}
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    var remove by remember {mutableStateOf<WorkRecord?>(null)}
    Page {
        PageTitle("Biblioteca","Guías y recursos oficiales")
        OutlinedTextField(query,{query=it},label={Text("Buscar título, autor o área")},modifier=Modifier.fillMaxWidth())
        ChoiceRow(listOf("MINEDU","Currículo","PDF propios","Favoritos"),filter){filter=it}
        Row {Button(onClick={importPdf(null)}){Text("Importar PDF")};TextButton(onClick=capture){Text("Foto / OCR")}}
        val docs=all.filter {it.kind=="document" && !it.archived}
        docs.filter {it.title.contains(query,true) && (filter=="PDF propios" || filter=="Favoritos" && JSONObject(it.json).optBoolean("favorite"))}.forEach {d->
            Card(onClick={open(d)},colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){ListItem(headlineContent={Text(d.title)},supportingContent={val j=JSONObject(d.json);Text(if(available(d))"Guardado offline · continuar página ${j.optInt("lastPage",0)+1}" else "Archivo no disponible — reimportar PDF")})}
            TextButton(onClick={save(d.copy(json=JSONObject(d.json).put("favorite",!JSONObject(d.json).optBoolean("favorite")).toString()))}){Text("Alternar favorito")}
            TextButton(onClick={remove=d}){Text("Archivar documento")}
        }
        if(filter in setOf("MINEDU","Currículo","Favoritos")) sources.filter {s->(s.title.contains(query,true)||s.author.contains(query,true)||s.area.contains(query,true)) && (filter!="Currículo" || s.kind=="marco_curricular") && (filter!="Favoritos" || all.any {it.id=="source-favorite:${s.id}" && !it.archived})}.forEach {source->
                val local=docs.find {JSONObject(it.json).optString("sourceId")==source.id && available(it)}
            Card(onClick={selected=source.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)){ListItem(headlineContent={Text(source.title)},supportingContent={Text("${source.year} · ${if(local==null)"Solo referencia — importar PDF" else "Guardado offline"}")})}
            Row {TextButton(onClick={importPdf(source.id)}){Text("Importar PDF de esta referencia")};if(local!=null)TextButton(onClick={open(local)}){Text("Leer")}}
            TextButton(onClick={val id="source-favorite:${source.id}";if(all.any {it.id==id && !it.archived})archive(id) else save(WorkRecord(id,"source_favorite",source.title,"{}",System.currentTimeMillis()))}){Text("Alternar favorito de referencia")}
        }
        val recent=docs.sortedByDescending {JSONObject(it.json).optLong("lastOpened")}.take(4)
        if(recent.isNotEmpty()){Text("Continuar leyendo",style=MaterialTheme.typography.titleMedium);recent.forEach {d->TextButton(onClick={open(d)}){Text(d.title)}}}
        if(docs.isEmpty())InfoCard("Tus documentos","Las referencias no incluyen archivos PDF. Puedes importar los obtenidos legítimamente; la consulta de metadatos funciona sin conexión.")
    }
    sources.find {it.id==selected}?.let {s->AlertDialog(onDismissRequest={selected=null},title={Text(s.title)},text={Text("${s.author}\n${s.description}\n${s.rightsNote}\nAbrir fuente utiliza el navegador y puede requerir internet.")},confirmButton={TextButton(onClick={openSource(s.officialUrl)}){Text("Abrir fuente oficial")}},dismissButton={TextButton(onClick={selected=null}){Text("Cerrar")}})}
    remove?.let {document->AlertDialog(onDismissRequest={remove=null},title={Text("¿Archivar documento?")},text={Text("Se conservan el archivo privado, marcas y referencias de tus trabajos. Puedes recuperarlo en Perfil → Datos y contenidos.")},confirmButton={TextButton(onClick={archive(document.id);remove=null}){Text("Archivar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
