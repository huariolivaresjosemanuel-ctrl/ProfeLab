package pe.profelab.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import pe.profelab.core.content.PackData
import kotlinx.coroutines.*

@Composable
fun WorkspaceTools(graph:AppGraph,records:List<WorkRecord>,save:(WorkRecord)->Unit,back:()->Unit,resumeOcr:()->Unit) {
    val scope=rememberCoroutineScope();var includeFiles by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)};var preview by remember {mutableStateOf<String?>(null)}
    var source by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    var pack by remember {mutableStateOf<PackData?>(null)}
    fun operation(block:suspend ()->Unit){scope.launch {busy=true;try{block()}catch(e:CancellationException){throw e}catch(_:Exception){message="No se completó la operación. Los datos anteriores se conservan; revisa formato, integridad y espacio."}finally{busy=false}}}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")){uri->if(uri!=null)operation {graph.transfers.exportBackup(uri.toString(),includeFiles);message="Respaldo exportado. Comprueba su integridad y conserva una copia segura."}}
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)operation {source=uri.toString();preview=graph.transfers.previewBackup(uri.toString())}}
    val packs=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)operation {pack=graph.packs.inspect(uri.toString())}}
    Page {
        PageTitle("Datos y contenidos","Operaciones locales elegidas por ti",back)
        if(busy)CircularProgressIndicator()
        message?.let {InfoCard("Resultado",it)}
        InfoCard("Privacidad del respaldo","El ZIP incluye tareas, perfil y trabajos locales. Activa archivos solo si autorizas incluir TODOS los documentos privados guardados en la app. El respaldo no está cifrado; guárdalo en un lugar protegido.")
        Row {Checkbox(includeFiles,{includeFiles=it});Text("Incluir mis archivos privados")}
        Button(enabled=!busy,onClick={export.launch("ProfeLab-respaldo.zip")}){Text("Exportar respaldo local")}
        OutlinedButton(enabled=!busy,onClick={restore.launch(arrayOf("application/zip","application/octet-stream"))}){Text("Previsualizar restauración")}
        OutlinedButton(enabled=!busy,onClick={packs.launch(arrayOf("application/zip","application/octet-stream"))}){Text("Previsualizar paquete de contenidos")}
        OutlinedButton(onClick=resumeOcr){Text("Retomar último borrador OCR")}
        Text("Archivados",style=MaterialTheme.typography.titleMedium)
        records.filter {it.archived && it.kind!="revision"}.forEach {r->TextButton(onClick={save(r.copy(archived=false))}){Text("Recuperar: ${r.title}")}}
    }
    preview?.let {text->AlertDialog(onDismissRequest={preview=null},title={Text("Fusionar respaldo")},text={Text(text)},confirmButton={TextButton(onClick={val uri=source;preview=null;if(uri!=null)operation {graph.transfers.restoreBackup(uri);message="Fusión terminada; los IDs existentes no se sobrescribieron."}}){Text("Confirmar fusión")}},dismissButton={TextButton(onClick={preview=null}){Text("Cancelar")}})}
    pack?.let {p->AlertDialog(onDismissRequest={pack=null},title={Text("Instalar contenido")},text={Text("${p.manifest.optString("packId")} · ${p.manifest.optString("version")}\n${p.records.count {it.kind in setOf("catalog_project","catalog_experiment")}} proyectos, ${p.records.count {it.kind=="catalog_material"}} materiales, ${p.records.count {it.kind=="catalog_layout"}} diseños y ${p.resources.size} imágenes. Revisa los derechos declarados por el autor. Las copias personales permanecen separadas; revisión docente pendiente.")},confirmButton={TextButton(onClick={pack=null;operation {graph.packs.install(p);message="Paquete aplicado sin modificar creaciones personales."}}){Text("Instalar")}},dismissButton={TextButton(onClick={pack=null}){Text("Cancelar")}})}
}
