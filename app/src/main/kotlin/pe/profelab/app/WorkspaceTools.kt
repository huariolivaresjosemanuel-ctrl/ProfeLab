package pe.profelab.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import pe.profelab.core.content.PackData
import kotlinx.coroutines.*

@Composable
fun WorkspaceTools(graph:AppGraph,records:List<WorkRecord>,save:(WorkRecord)->Unit,back:()->Unit,resumeOcr:()->Unit) {
    pe.profelab.feature.students.PrivateEducationSurface()
    val scope=rememberCoroutineScope();var includeFiles by remember {mutableStateOf(false)}
    var message by remember {mutableStateOf<String?>(null)};var preview by remember {mutableStateOf<String?>(null)}
    var source by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    var pack by remember {mutableStateOf<PackData?>(null)}
    var exportPassword by remember {mutableStateOf("")};var confirmation by remember {mutableStateOf("")}
    var restorePassword by remember {mutableStateOf("")}
    var askExport by remember {mutableStateOf(false)};var askRestore by remember {mutableStateOf(false)}
    DisposableEffect(Unit){onDispose {exportPassword="";confirmation="";restorePassword=""}}
    fun operation(block:suspend ()->Unit){scope.launch {busy=true;try{block()}catch(e:CancellationException){throw e}catch(_:Exception){message="No se completó la operación. Los datos anteriores se conservan; revisa contraseña, formato, integridad y espacio."}finally{busy=false}}}
    val export=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")){uri->
        confirmation=""
        if(uri!=null)operation {val secret=exportPassword.toCharArray();exportPassword=""
            try {graph.transfers.exportEncryptedBackup(uri.toString(),includeFiles,secret);message="Respaldo cifrado exportado. Conserva su contraseña en un lugar seguro; no se puede recuperar desde la app."}finally {secret.fill('\u0000')}
        }else exportPassword=""
    }
    val restore=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null){source=uri.toString();restorePassword="";askRestore=true}}
    val packs=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()){uri->if(uri!=null)operation {pack=graph.packs.inspect(uri.toString())}}
    Page {
        PageTitle("Datos y contenidos","Operaciones locales elegidas por ti",back)
        if(busy)CircularProgressIndicator()
        message?.let {InfoCard("Resultado",it)}
        InfoCard("Privacidad del respaldo","El respaldo cifrado incluye tareas, perfil y trabajos locales. Activa archivos solo si autorizas incluir TODOS los documentos privados. La contraseña protege también los archivos; consérvala fuera del dispositivo. Las copias antiguas siguen siendo legibles.")
        Row {Checkbox(includeFiles,{includeFiles=it});Text("Incluir mis archivos privados")}
        Button(enabled=!busy,onClick={exportPassword="";confirmation="";askExport=true}){Text("Exportar respaldo local")}
        OutlinedButton(enabled=!busy,onClick={restore.launch(arrayOf("application/zip","application/octet-stream"))}){Text("Previsualizar restauración")}
        OutlinedButton(enabled=!busy,onClick={packs.launch(arrayOf("application/zip","application/octet-stream"))}){Text("Previsualizar paquete de contenidos")}
        OutlinedButton(onClick=resumeOcr){Text("Retomar último borrador OCR")}
        Text("Archivados",style=MaterialTheme.typography.titleMedium)
        records.filter {it.archived && it.kind!="revision"}.forEach {r->TextButton(onClick={save(r.copy(archived=false))}){Text("Recuperar: ${r.title}")}}
    }
    if(askExport)AlertDialog(onDismissRequest={askExport=false;exportPassword="";confirmation=""},title={Text("Proteger respaldo")},text={Column {
        Text("Usa al menos 12 caracteres. Si pierdes la contraseña, no podrás restaurar esta copia. La app no la guarda.")
        OutlinedTextField(exportPassword,{exportPassword=it.take(1024)},label={Text("Contraseña")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
        OutlinedTextField(confirmation,{confirmation=it.take(1024)},label={Text("Repetir contraseña")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
    }},confirmButton={TextButton(enabled=exportPassword.length>=12 && exportPassword==confirmation,onClick={askExport=false;export.launch("ProfeLab-respaldo-cifrado.pflb")}){Text("Elegir destino")}},dismissButton={TextButton(onClick={askExport=false;exportPassword="";confirmation=""}){Text("Cancelar")}})
    if(askRestore)AlertDialog(onDismissRequest={askRestore=false;restorePassword="";source=null},title={Text("Abrir respaldo")},text={Column {
        Text("Introduce la contraseña de la copia cifrada. Déjala vacía sólo para un ZIP antiguo sin cifrar.")
        OutlinedTextField(restorePassword,{restorePassword=it.take(1024)},label={Text("Contraseña del respaldo")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password))
    }},confirmButton={TextButton(onClick={askRestore=false;val uri=source;if(uri!=null)operation {val secret=restorePassword.toCharArray()
        try {preview=if(secret.isEmpty())graph.transfers.previewBackup(uri)else graph.transfers.previewEncryptedBackup(uri,secret)}catch(error:Exception){restorePassword="";throw error}finally {secret.fill('\u0000')}
    }}){Text("Revisar contenido")}},dismissButton={TextButton(onClick={askRestore=false;restorePassword="";source=null}){Text("Cancelar")}})
    preview?.let {text->AlertDialog(onDismissRequest={preview=null;restorePassword=""},title={Text("Fusionar respaldo")},text={Text(text)},confirmButton={TextButton(onClick={val uri=source;preview=null;if(uri!=null)operation {val secret=restorePassword.toCharArray();restorePassword=""
        try {if(secret.isEmpty())graph.transfers.restoreBackup(uri)else graph.transfers.restoreEncryptedBackup(uri,secret);message="Fusión terminada; los IDs existentes no se sobrescribieron."}finally {secret.fill('\u0000')}
    }}){Text("Confirmar fusión")}},dismissButton={TextButton(onClick={preview=null;restorePassword=""}){Text("Cancelar")}})}
    pack?.let {p->AlertDialog(onDismissRequest={pack=null},title={Text("Instalar contenido")},text={Text("${p.manifest.optString("packId")} · ${p.manifest.optString("version")}\n${p.records.count {it.kind in setOf("catalog_project","catalog_experiment")}} proyectos, ${p.records.count {it.kind=="catalog_material"}} materiales, ${p.records.count {it.kind=="catalog_layout"}} diseños y ${p.resources.size} imágenes. Revisa los derechos declarados por el autor. Las copias personales permanecen separadas; revisión docente pendiente.")},confirmButton={TextButton(onClick={pack=null;operation {graph.packs.install(p);message="Paquete aplicado sin modificar creaciones personales."}}){Text("Instalar")}},dismissButton={TextButton(onClick={pack=null}){Text("Cancelar")}})}
}
