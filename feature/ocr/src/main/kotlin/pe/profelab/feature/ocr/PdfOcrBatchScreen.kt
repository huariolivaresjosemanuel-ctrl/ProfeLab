package pe.profelab.feature.ocr

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.*
import pe.profelab.core.model.newId
import pe.profelab.core.ui.*
import java.io.File
import pe.profelab.core.model.DraftRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

data class ReviewedOcrPage(val page:Int,val filePath:String,val raw:String,val corrected:String,val reviewed:Boolean=false)
@Composable
fun PdfOcrBatchScreen(documentId:String,drafts:DraftRepository,pageCount:Int,render:suspend(Int)->Bitmap?,complete:suspend(List<ReviewedOcrPage>)->Boolean,back:()->Unit) {
    val context=LocalContext.current;val scope=rememberCoroutineScope();val engine=remember {LatinOcrEngine()}
    var first by rememberSaveable {mutableStateOf("1")};var last by rememberSaveable {mutableStateOf("1")}
    var results by remember {mutableStateOf<List<ReviewedOcrPage>>(emptyList())};var index by remember {mutableIntStateOf(0)}
    var job by remember {mutableStateOf<Job?>(null)};var busy by remember {mutableStateOf(false)};var progress by remember {mutableStateOf("")}
    var error by remember {mutableStateOf<String?>(null)}
    val model:BatchOcrDraftModel=viewModel(key=documentId,factory=modelFactory {BatchOcrDraftModel(drafts,documentId,File(context.filesDir,"ocr-drafts"))})
    val draftError by model.error.collectAsStateWithLifecycle();var loaded by remember {mutableStateOf(false)}
    LaunchedEffect(documentId){results=model.load();loaded=true}
    LaunchedEffect(results,loaded){if(loaded)model.save(results)}
    DisposableEffect(Unit){onDispose {job?.cancel();engine.close()}}
    Page {
        PageTitle("OCR de páginas escaneadas","Procesamiento local incremental, máximo 10 páginas por lote",back)
        OutlinedTextField(first,{first=it},label={Text("Desde página (1 a $pageCount)")})
        OutlinedTextField(last,{last=it},label={Text("Hasta página")})
        error?.let {ErrorNotice(it)}
        draftError?.let {ErrorNotice(it)}
        Text("El lote en revisión se conserva localmente al salir. Reabre este documento y su herramienta OCR para retomarlo.",style=MaterialTheme.typography.bodySmall)
        Button(enabled=!busy && loaded,onClick={
            val a=first.toIntOrNull();val b=last.toIntOrNull()
            if(a==null||b==null||a !in 1..pageCount||b !in a..pageCount||b-a>=10)error="Elige entre 1 y 10 páginas válidas." else {
                results.forEach {File(it.filePath).delete()};results=emptyList();index=0;busy=true;error=null
                job=scope.launch {try {
                    for(page in a-1 until b){ensureActive();progress="Reconociendo página ${page+1}";val bitmap=render(page) ?: throw IllegalStateException("No se pudo renderizar")
                        try {val raw=engine.recognize(bitmap);val folder=File(context.filesDir,"ocr-drafts").apply {mkdirs()};val file=File(folder,"${newId()}.png");withContext(Dispatchers.IO){file.outputStream().use {bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}};results=results+ReviewedOcrPage(page,file.absolutePath,raw,raw)}finally{bitmap.recycle()}
                    }
                }catch(e:CancellationException){throw e}catch(_:Exception){error="Se interrumpió el lote. Las páginas ya reconocidas pueden revisarse."}finally{busy=false}}
            }
        }){Text("Reconocer lote sin conexión")}
        if(busy){CircularProgressIndicator();Text(progress);TextButton(onClick={job?.cancel();busy=false}){Text("Cancelar lote")}}
        if(results.isNotEmpty()){
            index=index.coerceIn(0,results.lastIndex);val current=results[index]
            Text("Revisión ${index+1} / ${results.size} · página ${current.page+1}")
            val bitmap by produceState<Bitmap?>(null,current.filePath){value=withContext(Dispatchers.IO){BitmapFactory.decodeFile(current.filePath)}}
            bitmap?.let {Image(it.asImageBitmap(),"Página original ${current.page+1}",Modifier.fillMaxWidth().heightIn(max=360.dp))}
            OutlinedTextField(current.corrected,{text->results=results.mapIndexed {i,r->if(i==index)r.copy(corrected=text,reviewed=false)else r}},minLines=5,label={Text("Texto revisable de la página")},modifier=Modifier.fillMaxWidth())
            Row {Checkbox(current.reviewed,{checked->results=results.mapIndexed {i,r->if(i==index)r.copy(reviewed=checked)else r}});Text("He cotejado esta página con el original")}
            Row {TextButton(enabled=index>0,onClick={index--}){Text("Anterior")};TextButton(enabled=index<results.lastIndex,onClick={index++}){Text("Siguiente")}}
            Button(enabled=!busy && results.all {it.reviewed},onClick={busy=true;scope.launch {try{model.finish(results){complete(results)}}finally{busy=false}}}){Text("Guardar lote revisado en Cuaderno")}
        }
    }
}
