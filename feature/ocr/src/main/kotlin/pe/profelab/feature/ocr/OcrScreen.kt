package pe.profelab.feature.ocr

import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import pe.profelab.core.ui.*
import pe.profelab.core.model.DraftRepository
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun OcrScreen(uri:Uri,drafts:DraftRepository,ownerId:String?,saveReviewed:suspend (String,Bitmap?,String)->Boolean,back:()->Unit,sourceDocumentId:String?=null,sourcePageIndex:Int=0) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var original by remember {mutableStateOf<Bitmap?>(null)};var working by remember {mutableStateOf<Bitmap?>(null)}
    var text by rememberSaveable(uri.toString()) {mutableStateOf("")};var raw by rememberSaveable(uri.toString()){mutableStateOf("")}
    var finished by remember {mutableStateOf(false)}
    var saving by remember {mutableStateOf(false)}
    var crop by remember {mutableFloatStateOf(0f)};var busy by remember {mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)};var reviewed by rememberSaveable {mutableStateOf(false)}
    val draft:ReviewDraftModel=viewModel(factory=modelFactory {ReviewDraftModel(drafts)})
    val draftError by draft.error.collectAsStateWithLifecycle()
    var loaded by remember(uri){mutableStateOf(false)}
    var job by remember {mutableStateOf<Job?>(null)}
    val recognizer=remember {TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)}
    DisposableEffect(Unit){onDispose {recognizer.close();job?.cancel()}}
    LaunchedEffect(uri){try {val saved=draft.load(uri.toString());saved?.let {raw=it.optString("raw");text=it.optString("text");reviewed=false};val imageUri=Uri.parse(saved?.optString("uri").orEmpty().ifBlank {uri.toString()});loaded=true;original=withContext(Dispatchers.IO){
        val options=BitmapFactory.Options().apply {inJustDecodeBounds=true};context.contentResolver.openInputStream(imageUri)!!.use {BitmapFactory.decodeStream(it,null,options)}
        var sample=1;while(options.outWidth/sample>2400||options.outHeight/sample>2400)sample*=2
        context.contentResolver.openInputStream(imageUri)!!.use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})} ?: error("No es imagen")
    };working=original}catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo leer la imagen."}}
    LaunchedEffect(uri,text,raw,loaded,saving,finished){if(loaded && !saving && !finished)draft.save(uri.toString(),raw,text,ownerId,sourceDocumentId,sourcePageIndex)}
    val leave:()->Unit={if(!saving){saving=true;job?.cancel();scope.launch {
        try {if(loaded){draft.save(uri.toString(),raw,text,ownerId,sourceDocumentId,sourcePageIndex);if(!draft.flush())return@launch};finished=true;back()}
        finally {saving=false}
    }}}
    BackHandler {leave()}
    Page {
        PageTitle("Revisión de imagen y OCR","Reconocimiento local sin conexión",leave)
        error?.let {ErrorNotice(it)}
        draftError?.let {ErrorNotice(it)}
        val imagePane: @Composable ()->Unit = {
        working?.let {bmp->
            Image(bmp.asImageBitmap(),"Original para comparar con el texto",Modifier.fillMaxWidth().heightIn(max=380.dp))
            Row {TextButton(enabled=!saving && !busy,onClick={working=Bitmap.createBitmap(bmp,0,0,bmp.width,bmp.height,Matrix().apply {postRotate(90f)},true);reviewed=false}){Text("Girar 90°")};TextButton(enabled=!saving && !busy,onClick={working=original;crop=0f;reviewed=false}){Text("Restaurar imagen")}}
            Text("Recorte de márgenes (0 a 35% por lado)");Slider(crop,{crop=it},enabled=!saving && !busy,valueRange=0f..0.35f)
            TextButton(enabled=!saving && !busy,onClick={val x=(bmp.width*crop).toInt();val y=(bmp.height*crop).toInt();working=Bitmap.createBitmap(bmp,x,y,bmp.width-2*x,bmp.height-2*y);crop=0f;reviewed=false}){Text("Aplicar recorte")}
            Button(enabled=!busy && !saving,onClick={busy=true;error=null;reviewed=false;job=scope.launch {
                try {val result=suspendCancellableCoroutine<String> {continuation->recognizer.process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener {if(continuation.isActive)continuation.resume(it.text)}.addOnFailureListener {if(continuation.isActive)continuation.resumeWithException(it)}};raw=result;text=result;if(result.isBlank())error="No se encontró texto. Repite la foto o escribe manualmente."}
                catch(e:CancellationException){throw e}catch(_:Exception){error="Falló el OCR local. Puedes corregir o escribir manualmente."}finally{busy=false}
            }}){Text("Reconocer texto sin conexión")}
        }
        }
        val reviewPane: @Composable ()->Unit = {
        if(saving){LinearProgressIndicator(Modifier.fillMaxWidth());Text("Guardando borrador local…")}
        if(busy){CircularProgressIndicator();TextButton(onClick={job?.cancel();busy=false}){Text("Cancelar reconocimiento")}}
        OutlinedTextField(text,{text=it;reviewed=false},enabled=!saving,label={Text("Texto reconocido: corrige antes de guardar")},minLines=6,modifier=Modifier.fillMaxWidth())
        Text("Texto OCR original: ${raw.take(600)}",style=MaterialTheme.typography.bodySmall)
        Text("Texto impreso en español; manuscritos, desenfoque y poca luz pueden producir errores.")
        Row {Checkbox(reviewed,{reviewed=it},enabled=!saving);Text("He revisado y corregido el texto")}
        Button(enabled=reviewed && (working!=null || text.isNotBlank()) && !busy && !saving,onClick={saving=true;scope.launch {
            try {draft.save(uri.toString(),raw,text,ownerId,sourceDocumentId,sourcePageIndex);if(draft.flush()){if(saveReviewed(text,working,raw))finished=true else error="El guardado no terminó. Tu borrador se conserva."}}
            finally {saving=false}
        }}){Text(if(working==null)"Guardar texto revisado sin imagen"else "Guardar texto revisado en el destino")}
        }
        BoxWithConstraints {
            if(maxWidth>=700.dp)Row(horizontalArrangement=Arrangement.spacedBy(20.dp)){
                Column(Modifier.weight(1f)){imagePane()}
                Column(Modifier.weight(1f)){reviewPane()}
            }else Column(verticalArrangement=Arrangement.spacedBy(16.dp)){imagePane();reviewPane()}
        }
    }
}
