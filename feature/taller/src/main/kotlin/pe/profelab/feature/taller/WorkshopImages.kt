package pe.profelab.feature.taller

import android.graphics.Bitmap
import android.content.Context
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.WorkRecord
import java.io.File

internal data class WorkshopImage(val id:String,val asset:Boolean=false,val label:String="Imagen aportada")
private val bitmapCache=object:LruCache<String,Bitmap>(12*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.byteCount}
private data class PictureState(val bitmap:Bitmap?=null,val loading:Boolean=true)
private fun loadWorkshopBitmap(context:Context,ref:WorkshopImage,max:Int):Bitmap? {
    if(ref.asset && !ref.id.matches(Regex("[a-zA-Z0-9_-]+\\.(?:png|jpg|jpeg|webp)")))return null
    if(!ref.asset && !ref.id.matches(Regex("[a-zA-Z0-9-]+")))return null
    val key="${ref.asset}:${ref.id}:$max"
    return bitmapCache.get(key) ?: runCatching {
        fun stream()=if(ref.asset)context.assets.open("workshop/images/${ref.id}")else File(context.filesDir,"documents/${ref.id}").inputStream()
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};stream().use {BitmapFactory.decodeStream(it,null,bounds)}
        var sample=1;while(bounds.outWidth/sample>max || bounds.outHeight/sample>max)sample*=2
        stream().use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})}?.also {bitmapCache.put(key,it)}
    }.getOrNull()
}
internal fun projectImages(record:WorkRecord):List<WorkshopImage>{
    val j=JSONObject(record.json);val list=mutableListOf<WorkshopImage>()
    val id=j.optString("originImageId").ifBlank {j.optString("forkedFromTemplateId").ifBlank {record.id}}
    if(id.matches(Regex("(?:mat|com|cta|per|arte|tut)-[0-9]{3}")))list+=WorkshopImage("$id.png",true,"Ilustración conceptual original; no foto del procedimiento")
    WorkshopCodec.strings(j.opt("assetImages")).filter {it.matches(Regex("[a-zA-Z0-9_-]+\\.(?:png|jpg|jpeg|webp)"))}.forEach {list+=WorkshopImage(it,true,"Imagen editorial incluida con la aplicación; consulta sus fuentes")}
    WorkshopCodec.strings(j.opt("imageFileIds")).filter {it.matches(Regex("[a-zA-Z0-9-]+"))}.forEach {list+=installedWorkshopImage(j,it)}
    val attachments=j.optJSONArray("attachments")
    if(attachments!=null)for(i in 0 until attachments.length()){val a=attachments.optJSONObject(i) ?: continue;val file=a.optString("id");if(a.optString("mime").startsWith("image/") && file.matches(Regex("[a-zA-Z0-9-]+")))list+=WorkshopImage(file,label="Fotografía o boceto propio; revisa su privacidad")}
    return list.distinctBy {it.asset to it.id}
}
internal fun installedWorkshopImage(data:JSONObject,id:String):WorkshopImage {
    val metadata=data.optJSONObject("imageMetadataByFileId")?.optJSONObject(id)
    val alt=metadata?.optString("alt").orEmpty().ifBlank {"Imagen del paquete; consulta sus fuentes"}
    return WorkshopImage(id,label=listOf(alt,metadata?.optString("caption").orEmpty()).filter {it.isNotBlank()}.joinToString(" · "))
}
@Composable
internal fun WorkshopPicture(ref:WorkshopImage?,modifier:Modifier=Modifier,large:Boolean=false,missingMessage:String="Sin imagen disponible"){
    val context=LocalContext.current;var expanded by remember {mutableStateOf(false)};var retry by remember {mutableIntStateOf(0)}
    val loaded by produceState(PictureState(loading=ref!=null),ref,large,retry){
        value=PictureState(withContext(Dispatchers.IO){ref?.let {loadWorkshopBitmap(context,it,if(large)1200 else 400)}},false)
    }
    val bitmap=loaded.bitmap
    Surface(modifier,color=WorkshopColors.Paper,shape=MaterialTheme.shapes.medium){
        val b=bitmap
        if(b!=null)Image(b.asImageBitmap(),ref?.label ?: "Imagen del proyecto",Modifier.fillMaxSize().clickable {expanded=true},contentScale=ContentScale.Fit)
        else if(loaded.loading)CircularProgressIndicator(Modifier.padding(20.dp))
        else Column(Modifier.padding(12.dp)){Icon(Icons.Outlined.Image,null);Text(if(ref==null)missingMessage else "Imagen no disponible",style=MaterialTheme.typography.bodySmall);if(ref!=null)TextButton(onClick={retry++}){Text("Reintentar")}}
    }
    if(expanded && ref!=null)Dialog(onDismissRequest={expanded=false}){
        Surface(shape=MaterialTheme.shapes.large){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(ref.label,style=MaterialTheme.typography.bodySmall)
            val detailed by produceState(bitmap,ref){value=withContext(Dispatchers.IO){loadWorkshopBitmap(context,ref,1600)} ?: bitmap}
            var scale by remember {mutableFloatStateOf(1f)};var x by remember {mutableFloatStateOf(0f)};var y by remember {mutableFloatStateOf(0f)}
            Box(Modifier.fillMaxWidth().heightIn(min=240.dp,max=520.dp).aspectRatio(.85f).clipToBounds()){
                detailed?.let {Image(it.asImageBitmap(),ref.label,Modifier.fillMaxSize().graphicsLayer(scaleX=scale,scaleY=scale,translationX=x,translationY=y).pointerInput(Unit){detectTransformGestures {_,pan,zoom,_->scale=(scale*zoom).coerceIn(1f,4f);x+=pan.x;y+=pan.y}},contentScale=ContentScale.Fit)}
            }
            TextButton(onClick={scale=1f;x=0f;y=0f}){Text("Restablecer zoom")}
            Button(onClick={expanded=false},modifier=Modifier.fillMaxWidth()){Text("Cerrar imagen")}
        }}
    }
}
