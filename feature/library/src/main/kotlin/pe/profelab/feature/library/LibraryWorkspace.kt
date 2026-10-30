package pe.profelab.feature.library

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Description
import androidx.compose.ui.Alignment
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import org.json.JSONObject

@Composable
fun LibraryWorkspace(sources:List<SourceReference>,all:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,
    importPdf:(String?)->Unit,open:(WorkRecord)->Unit,openSource:(String)->Unit,capture:()->Unit,available:(WorkRecord)->Boolean) {
    var query by rememberSaveable {mutableStateOf("")};var filter by rememberSaveable {mutableStateOf("MINEDU")}
    var selected by rememberSaveable {mutableStateOf<String?>(null)}
    var remove by remember {mutableStateOf<WorkRecord?>(null)}
    val searchFocus=remember {FocusRequester()};val scope=rememberCoroutineScope()
    BindAssistant("library",mapOf(AssistantAction.LIBRARY_IMPORT to {importPdf(null)},AssistantAction.LIBRARY_SEARCH to {
        filter="PDF propios";scope.launch {withFrameNanos {};searchFocus.requestFocus()}
    }))
    Page {
        SectionHeading("Biblioteca","Guías y recursos oficiales",Icons.AutoMirrored.Outlined.MenuBook)
        OutlinedTextField(query,{query=it},label={Text("Buscar título, autor o área")},modifier=Modifier.fillMaxWidth().focusRequester(searchFocus))
        ChoiceRow(listOf("MINEDU","Currículo","PDF propios","Favoritos"),filter){filter=it}
        Row {Button(onClick={importPdf(null)}){Text("Importar PDF")};TextButton(onClick=capture){Text("Foto / OCR")}}
        val docs=all.filter {it.kind=="document" && !it.archived}
        docs.filter {it.title.contains(query,true) && (filter=="PDF propios" || filter=="Favoritos" && JSONObject(it.json).optBoolean("favorite"))}.forEach {d->
            Card(onClick={open(d)},colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){ListItem(leadingContent={Icon(Icons.Outlined.Description,null,tint=ProfeColors.Green)},headlineContent={Text(d.title)},supportingContent={val j=JSONObject(d.json);Text(if(available(d))"Guardado offline · página ${j.optInt("lastPage",0)+1}" else "Archivo no disponible — reimportar PDF")},colors=ListItemDefaults.colors(containerColor=Color.Transparent))}
            TextButton(onClick={save(d.copy(json=JSONObject(d.json).put("favorite",!JSONObject(d.json).optBoolean("favorite")).toString()))}){Text("Alternar favorito")}
            TextButton(onClick={remove=d}){Text("Archivar documento")}
        }
        val references=if(filter in setOf("MINEDU","Currículo","Favoritos"))sources.filter {s->(s.title.contains(query,true)||s.author.contains(query,true)||s.area.contains(query,true)) && (filter!="Currículo" || s.kind=="marco_curricular") && (filter!="Favoritos" || all.any {it.id=="source-favorite:${s.id}" && !it.archived})}else emptyList()
        if(references.isNotEmpty())Text("Documento destacado",style=MaterialTheme.typography.titleMedium)
        references.forEachIndexed {index,source->
                val local=docs.find {JSONObject(it.json).optString("sourceId")==source.id && available(it)}
            Card(onClick={selected=source.id},colors=CardDefaults.cardColors(containerColor=if(index==0)ProfeColors.Mint else ProfeColors.Cream)){
                Row(Modifier.fillMaxWidth().padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp),verticalAlignment=Alignment.CenterVertically){
                    Surface(color=if(index==0)ProfeColors.Cream else ProfeColors.Blue,shape=MaterialTheme.shapes.medium){Icon(Icons.AutoMirrored.Outlined.MenuBook,null,Modifier.padding(if(index==0)20.dp else 12.dp).size(if(index==0)44.dp else 28.dp),tint=ProfeColors.Green)}
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)){
                        Text(source.title,style=if(index==0)MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium)
                        Text("${source.year} · ${source.author}",style=MaterialTheme.typography.bodySmall)
                        if(index==0)Text(source.description,style=MaterialTheme.typography.bodyMedium)
                        Text(if(local==null)"Solo referencia · importa el PDF obtenido legítimamente"else "Tu archivo está guardado offline",style=MaterialTheme.typography.labelLarge,color=ProfeColors.Green)
                    }
                }
            }
            Row {TextButton(onClick={importPdf(source.id)}){Text("Importar PDF de esta referencia")};if(local!=null)TextButton(onClick={open(local)}){Text("Leer")}}
            TextButton(onClick={val id="source-favorite:${source.id}";if(all.any {it.id==id && !it.archived})archive(id) else save(WorkRecord(id,"source_favorite",source.title,"{}",System.currentTimeMillis()))}){Text("Alternar favorito de referencia")}
        }
        val recent=docs.filter {JSONObject(it.json).optLong("lastOpened")>0}.sortedByDescending {JSONObject(it.json).optLong("lastOpened")}.take(4)
        if(recent.isNotEmpty()){Text("Continuar leyendo",style=MaterialTheme.typography.titleMedium);recent.forEach {d->Card(onClick={open(d)},colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){
            Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){Text(d.title,style=MaterialTheme.typography.titleMedium);Text("Página ${JSONObject(d.json).optInt("lastPage",0)+1} · ${if(available(d))"guardado offline"else "reimportar archivo"}");Text("Abrir y continuar",color=ProfeColors.Green)}
        }}}
        if(docs.isEmpty())InfoCard("Tus documentos","Las referencias no incluyen archivos PDF. Puedes importar los obtenidos legítimamente; la consulta de metadatos funciona sin conexión.")
    }
    sources.find {it.id==selected}?.let {s->AlertDialog(onDismissRequest={selected=null},title={Text(s.title)},text={Text("${s.author}\n${s.description}\n${s.rightsNote}\nAbrir fuente utiliza el navegador y puede requerir internet.")},confirmButton={TextButton(onClick={openSource(s.officialUrl)}){Text("Abrir fuente oficial")}},dismissButton={TextButton(onClick={selected=null}){Text("Cerrar")}})}
    remove?.let {document->AlertDialog(onDismissRequest={remove=null},title={Text("¿Archivar documento?")},text={Text("Se conservan el archivo privado, marcas y referencias de tus trabajos. Puedes recuperarlo en Perfil → Datos y contenidos.")},confirmButton={TextButton(onClick={archive(document.id);remove=null}){Text("Archivar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
