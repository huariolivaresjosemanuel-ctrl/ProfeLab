package pe.profelab.feature.pdf

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.net.Uri
import android.graphics.Bitmap
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import java.io.File

@Composable
fun PdfReaderScreen(document:WorkRecord,uri:Uri,all:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,
    exported:(File)->Unit,ocrPage:(Bitmap,Int)->Unit,back:()->Unit,saveCopy:(()->Unit)?=null,ocrBatch:(String,Int)->Unit={_,_->},initialPage:Int=-1) {
    val context=LocalContext.current;val scope=rememberCoroutineScope()
    var handle by remember(document.id){mutableStateOf<PdfDocumentHandle?>(null)}
    var password by remember(document.id){mutableStateOf("")};var unlock by remember {mutableIntStateOf(0)}
    var page by rememberSaveable(document.id,initialPage){mutableIntStateOf(if(initialPage>=0)initialPage else JSONObject(document.json).optInt("lastPage",0))}
    var zoom by rememberSaveable(document.id){mutableFloatStateOf(JSONObject(document.json).optDouble("zoom",1.0).toFloat())}
    var panX by remember {mutableFloatStateOf(0f)};var panY by remember {mutableFloatStateOf(0f)}
    var bitmap by remember {mutableStateOf<Bitmap?>(null)};var boxes by remember {mutableStateOf<List<TextBox>>(emptyList())}
    var error by remember {mutableStateOf<String?>(null)};var busy by remember {mutableStateOf(false)}
    var tool by rememberSaveable {mutableStateOf("Texto: resaltar")};var comment by rememberSaveable {mutableStateOf("")}
    var query by rememberSaveable {mutableStateOf("")};var jump by rememberSaveable {mutableStateOf("")}
    var color by rememberSaveable {mutableLongStateOf(0xFFFFD54FL)};var width by rememberSaveable {mutableFloatStateOf(.004f)}
    var stroke by remember {mutableStateOf<List<Offset>>(emptyList())}
    var undo by remember {mutableStateOf<List<List<WorkRecord>>>(emptyList())};var redo by remember {mutableStateOf<List<List<WorkRecord>>>(emptyList())}
    var searchJob by remember {mutableStateOf<Job?>(null)};var hits by remember {mutableStateOf<List<Pair<Int,String>>>(emptyList())}
    var linkPage by remember {mutableStateOf(false)}
    val temporary=document.kind=="document_temp"
    val marks=all.filter {it.kind=="annotation" && !it.archived && JSONObject(it.json).optString("documentId")==document.id && JSONObject(it.json).optString("sha256")==JSONObject(document.json).optString("sha256")}
    val current=marks.filter {JSONObject(it.json).optInt("page")==page}
    LaunchedEffect(document.id,unlock){
        busy=true;error=null
        try {handle=withContext(Dispatchers.IO){PdfDocumentHandle(context,uri,password.takeIf {it.isNotEmpty()})};val count=handle!!.pageCount;page=page.coerceIn(0,(count-1).coerceAtLeast(0))}
        catch(e:CancellationException){throw e}catch(_:Exception){error="No se puede abrir: revisa permisos, integridad o contraseña del PDF."};busy=false
    }
    DisposableEffect(handle){val h=handle;onDispose {h?.close()}}
    LaunchedEffect(handle,page){val h=handle ?: return@LaunchedEffect
        busy=true
        try{bitmap=withContext(Dispatchers.IO){h.render(page,1200)};boxes=PdfEngine.text(context,uri,page,password)}
        catch(e:CancellationException){throw e}catch(_:Exception){boxes=emptyList();error="No se pudo extraer texto; las marcas manuales siguen disponibles."};busy=false
        if(!temporary)save(document.copy(json=JSONObject(document.json).put("lastPage",page).put("zoom",zoom).put("lastOpened",System.currentTimeMillis()).toString()))
    }
    LaunchedEffect(zoom){if(!temporary && handle!=null){delay(500);save(document.copy(json=JSONObject(document.json).put("lastPage",page).put("zoom",zoom).toString()))}}
    fun apply(next:List<WorkRecord>){undo=undo+listOf(marks);redo=emptyList();marks.filter {old->next.none {it.id==old.id}}.forEach {archive(it.id)};next.forEach(save)}
    fun add(a:JSONObject){if(temporary){error="Guarda una copia privada antes de conservar anotaciones.";return}
        a.put("documentId",document.id).put("sha256",JSONObject(document.json).optString("sha256")).put("page",page).put("color",color).put("width",width)
        apply(marks+WorkRecord(newId(),"annotation","Marca PDF",a.toString(),System.currentTimeMillis()))
    }
    Page {
        PageTitle(document.title,"${page+1} / ${handle?.pageCount ?: "?"}",back)
        if(temporary)InfoCard("Lectura temporal","La URI puede caducar. Las marcas duraderas requieren guardar en Biblioteca.")
        saveCopy?.let {Button(onClick=it){Text("Guardar copia privada en Biblioteca")}}
        error?.let {ErrorNotice(it)}
        if(handle==null){OutlinedTextField(password,{password=it},label={Text("Contraseña PDF (solo en memoria)")},visualTransformation=androidx.compose.ui.text.input.PasswordVisualTransformation());Button(onClick={unlock++}){Text("Abrir / desbloquear")}}
        if(busy)CircularProgressIndicator()
        handle?.let {h->
            Row {TextButton(onClick={if(page>0)page--},enabled=page>0){Text("Anterior")};TextButton(onClick={if(page<h.pageCount-1)page++},enabled=page<h.pageCount-1){Text("Siguiente")}}
            LazyRow {items(((page-4).coerceAtLeast(0)..(page+4).coerceAtMost(h.pageCount-1)).toList()){index->
                val thumb by produceState<Bitmap?>(null,document.id,index){value=withContext(Dispatchers.IO){h.render(index,200)}}
                Column(Modifier.width(70.dp).clickable {page=index}){thumb?.let {Image(it.asImageBitmap(),"Miniatura página ${index+1}",Modifier.height(90.dp))};Text("${index+1}")}
            }}
            OutlinedTextField(jump,{jump=it},label={Text("Ir a página")});TextButton(onClick={jump.toIntOrNull()?.takeIf {it in 1..h.pageCount}?.let {page=it-1}}){Text("Ir")}
            Text("Zoom ${"%.1f".format(zoom)}×");Slider(zoom,{zoom=it},valueRange=1f..3f,onValueChangeFinished={if(!temporary)save(document.copy(json=JSONObject(document.json).put("lastPage",page).put("zoom",zoom).toString()))})
            ChoiceRow(listOf("Mover / zoom","Texto: resaltar","Texto: subrayar","Lápiz","Marca manual","Borrador","Nota"),tool){tool=it}
            Row {listOf(0xFFFFD54FL,0xFF1E88E5L,0xFFE53935L,0xFF175C47L).forEach {c->TextButton(onClick={color=c}){Text("●",color=Color(c.toInt()))}}}
            Text("Grosor");Slider(width,{width=it},valueRange=.001f..0.02f)
            OutlinedTextField(comment,{comment=it},label={Text("Comentario de la marca / nota")},modifier=Modifier.fillMaxWidth())
            Text(if(boxes.isEmpty())"Sin capa de texto extraída: usa marcas manuales u OCR revisado." else "Selecciona texto arrastrando sobre sus caracteres. Las marcas manuales son una herramienta distinta.")
            bitmap?.let {bmp->
                val pageView: @Composable (Modifier)->Unit = {container ->
                Box(container.fillMaxWidth().aspectRatio(bmp.width.toFloat()/bmp.height).graphicsLayer(scaleX=zoom,scaleY=zoom,translationX=panX,translationY=panY).pointerInput(tool){if(tool=="Mover / zoom")detectTransformGestures {_,pan,factor,_->zoom=(zoom*factor).coerceIn(1f,3f);panX+=pan.x;panY+=pan.y}}.pointerInput(tool,page,boxes,marks,color,width,comment){
                    if(tool!="Mover / zoom")detectDragGestures(onDragStart={p->stroke=listOf(Offset((p.x/size.width).coerceIn(0f,1f),(p.y/size.height).coerceIn(0f,1f)))},onDragCancel={stroke=emptyList()},onDragEnd={
                        if(stroke.isNotEmpty()) {
                            val left=stroke.minOf {it.x};val right=stroke.maxOf {it.x};val top=stroke.minOf {it.y};val bottom=stroke.maxOf {it.y}
                            when(tool){
                                "Texto: resaltar","Texto: subrayar"->{
                                    val selected=boxes.filter {it.right>=left && it.left<=right && it.bottom>=top && it.top<=bottom}
                                    if(selected.isEmpty()) error="No se seleccionó texto real; usa la herramienta manual para escaneos."
                                    else {val next=selected.map {b->val a=JSONObject().put("type",if(tool=="Texto: subrayar")"underline" else "highlight").put("origin","TEXT_LAYER").put("quote",b.text).put("comment",comment).put("left",b.left).put("right",b.right).put("top",b.top).put("bottom",b.bottom).put("documentId",document.id).put("sha256",JSONObject(document.json).optString("sha256")).put("page",page).put("color",color).put("width",width);WorkRecord(newId(),"annotation","Texto PDF",a.toString(),System.currentTimeMillis())};if(!temporary)apply(marks+next) else error="Guarda una copia antes de anotar."}
                                }
                                "Borrador"->{apply(marks.filterNot {r->val a=JSONObject(r.json);a.optInt("page")==page && a.optDouble("left",-1.0)<=right && a.optDouble("right",-1.0)>=left && a.optDouble("top",-1.0)<=bottom && a.optDouble("bottom",-1.0)>=top})}
                                else->add(JSONObject().put("type",when(tool){"Lápiz"->"pen";"Nota"->"note";else->"highlight"}).put("origin","MANUAL").put("left",left).put("right",right).put("top",top).put("bottom",bottom).put("comment",comment).put("points",JSONArray(stroke.flatMap {listOf(it.x,it.y)})))
                            }
                        };stroke=emptyList()
                    }){change,drag->change.consume();stroke=stroke+Offset((change.position.x/size.width).coerceIn(0f,1f),(change.position.y/size.height).coerceIn(0f,1f))}
                }) {
                    Image(bmp.asImageBitmap(),"Página ${page+1}",Modifier.fillMaxSize())
                    Canvas(Modifier.fillMaxSize()){
                        current.forEach {r->val a=JSONObject(r.json);val c=Color(a.optLong("color",0xFFFFD54F).toInt());val type=a.optString("type")
                            if(type=="pen"){val p=a.optJSONArray("points") ?: JSONArray();for(i in 2 until p.length()-1 step 2)drawLine(c,Offset(p.getDouble(i-2).toFloat()*size.width,p.getDouble(i-1).toFloat()*size.height),Offset(p.getDouble(i).toFloat()*size.width,p.getDouble(i+1).toFloat()*size.height),a.optDouble("width",.004).toFloat()*size.width)}
                            else {val l=a.optDouble("left").toFloat()*size.width;val t=a.optDouble("top").toFloat()*size.height;val rgt=a.optDouble("right").toFloat()*size.width;val btm=a.optDouble("bottom").toFloat()*size.height;if(type=="underline")drawLine(c,Offset(l,btm),Offset(rgt,btm),2f) else drawRect(c.copy(alpha=.3f),Offset(l,t),androidx.compose.ui.geometry.Size((rgt-l).coerceAtLeast(1f),(btm-t).coerceAtLeast(1f)))}
                        }
                        stroke.zipWithNext().forEach {(a,b)->drawLine(Color(color.toInt()),Offset(a.x*size.width,a.y*size.height),Offset(b.x*size.width,b.y*size.height),width*size.width)}
                    }
                }
                }
                BoxWithConstraints {
                    if(maxWidth>=700.dp)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){
                        Box(Modifier.weight(2f).clipToBounds()){pageView(Modifier)}
                        Column(Modifier.weight(1f)){
                            Text("Notas de esta página",style=MaterialTheme.typography.titleMedium)
                            OutlinedTextField(comment,{comment=it},label={Text("Comentario para anclar")},modifier=Modifier.fillMaxWidth())
                            current.forEach {mark->Text(JSONObject(mark.json).optString("comment").ifBlank {JSONObject(mark.json).optString("quote","Trazo manual")})}
                        }
                    }else Box(Modifier.fillMaxWidth().clipToBounds()){pageView(Modifier)}
                }
            }
            Row {TextButton(onClick={if(undo.isNotEmpty()){val snapshot=undo.last();redo=redo+listOf(marks);undo=undo.dropLast(1);marks.forEach {archive(it.id)};snapshot.forEach {save(it.copy(archived=false))}}},enabled=undo.isNotEmpty()){Text("Deshacer")};TextButton(onClick={if(redo.isNotEmpty()){val snapshot=redo.last();undo=undo+listOf(marks);redo=redo.dropLast(1);marks.forEach {archive(it.id)};snapshot.forEach {save(it.copy(archived=false))}}},enabled=redo.isNotEmpty()){Text("Rehacer")}}
            current.forEach {a->TextButton(onClick={archive(a.id)}){Text("Eliminar marca: ${JSONObject(a.json).optString("comment").ifBlank {JSONObject(a.json).optString("quote","Trazo")}}")}}
            TextButton(onClick={if(!temporary)save(WorkRecord("bookmark:${document.id}:$page","bookmark","${document.title}: página ${page+1}",JSONObject().put("documentId",document.id).put("page",page).toString(),System.currentTimeMillis()))}){Text("Añadir marcapáginas")}
            all.filter {it.kind=="bookmark" && !it.archived && JSONObject(it.json).optString("documentId")==document.id}.forEach {b->TextButton(onClick={page=JSONObject(b.json).getInt("page")}){Text(b.title)}}
            OutlinedTextField(query,{query=it},label={Text("Buscar texto en el documento")})
            TextButton(onClick={searchJob?.cancel();hits=emptyList();searchJob=scope.launch {try {
                if(query.isNotBlank()){
                    all.filter {it.kind=="note" && !it.archived}.forEach {note->val data=JSONObject(note.json);if(data.optBoolean("ocrConfirmed")){val pages=data.optJSONArray("ocrPages") ?: JSONArray();for(i in 0 until pages.length()){val p=pages.getJSONObject(i);val index=p.optInt("pageIndex",-1);val text=p.optString("text");if(p.optString("documentId")==document.id && index in 0 until h.pageCount && text.contains(query,true) && hits.size<100)hits=hits+(index to "OCR revisado: ${text.take(200)}")}}}
                    for(p in 0 until h.pageCount){ensureActive();val text=PdfEngine.text(context,uri,p,password).joinToString(""){it.text};if(text.contains(query,true))hits=hits+(p to text.take(200));if(hits.size>=100)break}
                }
            }catch(e:CancellationException){throw e}catch(_:Exception){error="Búsqueda interrumpida; los resultados OCR revisados disponibles se conservan."}}}){Text("Buscar texto y OCR revisado")}
            TextButton(onClick={searchJob?.cancel()}){Text("Cancelar búsqueda")}
            hits.forEach {(p,text)->TextButton(onClick={page=p}){Text("Página ${p+1}: $text")}}
            TextButton(onClick={if(temporary)error="Guarda el documento en Biblioteca antes de crear una evidencia OCR." else bitmap?.let {ocrPage(it,page)}}){Text("OCR de esta página (revisión obligatoria)")}
            TextButton(enabled=!temporary,onClick={ocrBatch(password,h.pageCount)}){Text("OCR por lote de páginas escaneadas")}
            TextButton(onClick={linkPage=!linkPage},enabled=!temporary){Text("Vincular página y marcas a un trabajo")}
            if(linkPage)all.filter {it.kind in setOf("project","experiment","session","note") && !it.archived}.forEach {target->
                TextButton(onClick={val data=JSONObject(target.json);val links=data.optJSONArray("links") ?: JSONArray();val ids=(0 until links.length()).map {links.getString(it)};if(document.id !in ids)data.put("links",JSONArray(ids+document.id));val citations=data.optJSONArray("citations") ?: JSONArray();data.put("citations",JSONArray(citations.toString()).put(JSONObject().put("documentId",document.id).put("pageIndex",page).put("annotationIds",JSONArray(current.map {it.id}))));save(target.copy(json=data.toString()));linkPage=false}){Text("Vincular a ${target.title}")}
            }
            Button(onClick={scope.launch {busy=true;try{exported(PdfEngine.export(context,uri,password,marks.map {JSONObject(it.json)}))}catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo exportar; las marcas locales se conservan."};busy=false}}){Text("Vista previa de copia PDF anotada")}
            Text("La copia exportada aplana marcas al contenido; el original se conserva. Revisa la copia en otro lector antes de compartir.",style=MaterialTheme.typography.bodySmall)
        }
    }
}
