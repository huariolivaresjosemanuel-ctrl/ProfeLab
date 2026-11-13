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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.content.WorkshopMediaBindings
import pe.profelab.core.content.WorkshopEvidenceData
import pe.profelab.core.content.WorkshopAssetPaths
import pe.profelab.core.model.WorkRecord
import java.io.File
import java.security.MessageDigest

internal data class WorkshopImage(val id:String,val asset:Boolean=false,val label:String="Imagen aportada",val expectedHash:String?=null)
internal val LocalWorkshopMediaRecords=staticCompositionLocalOf<List<WorkRecord>> {emptyList()}
private val bitmapCache=object:LruCache<String,Bitmap>(12*1024*1024){override fun sizeOf(key:String,value:Bitmap)=value.byteCount}
private data class PictureState(val bitmap:Bitmap?=null,val loading:Boolean=true)
private fun loadWorkshopBitmap(context:Context,ref:WorkshopImage,max:Int):Bitmap? {
    if(ref.asset && !(WorkshopAssetPaths.legacyName(ref.id) || WorkshopAssetPaths.validModular(ref.id)))return null
    if(!ref.asset && !ref.id.matches(Regex("[a-zA-Z0-9-]+")))return null
    val resolved=if(ref.asset)runCatching {WorkshopAssetPaths.canonical(context,ref.id)}.getOrNull() ?: return null else ref.id
    val key="${ref.asset}:$resolved:${ref.expectedHash.orEmpty()}:$max"
    return bitmapCache.get(key) ?: runCatching {
        fun stream()=if(ref.asset)WorkshopAssetPaths.open(context,ref.id)else File(context.filesDir,"documents/$resolved").inputStream()
        ref.expectedHash?.let {expected->
            val digest=MessageDigest.getInstance("SHA-256");stream().use {input->val bytes=ByteArray(65536);while(true){val n=input.read(bytes);if(n<0)break;digest.update(bytes,0,n)}}
            require(digest.digest().joinToString(""){"%02x".format(it)}.equals(expected,true))
        }
        val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};stream().use {BitmapFactory.decodeStream(it,null,bounds)}
        require(bounds.outWidth in 1..30000 && bounds.outHeight in 1..30000)
        var sample=1;while(bounds.outWidth/sample>max || bounds.outHeight/sample>max)sample*=2
        stream().use {BitmapFactory.decodeStream(it,null,BitmapFactory.Options().apply {inSampleSize=sample})}?.also {bitmapCache.put(key,it)}
    }.getOrNull()
}
/** Reserved media areas keep their ratio while Work produces the real files. */
@Composable
internal fun WorkshopMediaSlot(record:WorkRecord,role:String,stepId:String?=null,materialId:String?=null,modifier:Modifier=Modifier){
    val overlays=LocalWorkshopMediaRecords.current
    val ref=remember(record.json,record.id,overlays,role,stepId,materialId){
        val j=JSONObject(record.json)
        val own=if(role in setOf("catalog_cover","detail_hero"))WorkshopMediaBindings.preferredPrivateCover(record)else null
        if(own!=null)WorkshopImage(own,label="Fotografía o boceto propio de esta actividad")else {
            val bound=WorkshopMediaBindings.resolve(record,role,stepId,materialId,overlays)
            if(bound!=null)WorkshopImage(bound.storageId,bound.asset,listOf(bound.alt,bound.caption,"Referencia editorial; no es evidencia del usuario").filter {it.isNotBlank()}.joinToString(" · "),bound.sha256)
            else when(role){
                "step_photo"->WorkshopFlowAdapter.read(record).steps.firstOrNull {it.id==stepId}?.imageFileIds?.firstOrNull()?.let {installedWorkshopImage(j,it)}
                "catalog_cover","detail_hero"->projectImages(record).filterNot {!it.asset && it.id in WorkshopEvidenceData.privateImageIds(j)}.let {images->images.firstOrNull {!it.asset} ?: images.firstOrNull()}
                else->null
            }
        }
    }
    val box=modifier.aspectRatio(WorkshopMediaBindings.ratio(role)).testTag("workshop-media:$role:${record.id}:${stepId.orEmpty()}:${materialId.orEmpty()}")
    if(ref!=null)WorkshopPicture(ref,box,large=role!in setOf("catalog_cover","material_icon","sector_icon"),crop=role=="catalog_cover",missingMessage="Imagen específica pendiente o no disponible")
    else {
        val label=when(role){"step_photo"->"Fotografía de este paso pendiente";"outcome_reference"->"Referencia visual pendiente; registra solo tus observaciones";"teacher_reference"->"Apoyo visual de la guía pendiente";"material_icon"->"Imagen del material pendiente";else->"Fotografía específica pendiente"}
        Surface(box,color=WorkshopColors.Mint,shape=MaterialTheme.shapes.large){
            BoxWithConstraints(Modifier.fillMaxSize().semantics {contentDescription=label}){
                val compact=maxWidth<100.dp || maxHeight<80.dp
                Column(Modifier.fillMaxSize().padding(if(compact)6.dp else 16.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                    Icon(Icons.Outlined.Image,null)
                    if(!compact){Spacer(Modifier.height(6.dp));Text(label,style=MaterialTheme.typography.bodySmall)}
                }
            }
        }
    }
}

@Composable
internal fun WorkshopSectorMediaSlot(sectorId:String,modifier:Modifier=Modifier,fallback:@Composable ()->Unit){
    val reference=WorkRecord("sector:$sectorId","catalog_sector","",JSONObject().put("contentVersion","1.0.0").toString(),0)
    if(WorkshopMediaBindings.resolve(reference,"sector_icon",overlays=LocalWorkshopMediaRecords.current)!=null)WorkshopMediaSlot(reference,"sector_icon",modifier=modifier)else fallback()
}
internal fun projectImages(record:WorkRecord):List<WorkshopImage>{
    val j=JSONObject(record.json);val list=mutableListOf<WorkshopImage>()
    val id=j.optString("originImageId").ifBlank {j.optString("forkedFromTemplateId").ifBlank {record.id}}
    if(id.matches(Regex("(?:mat|com|cta|per|arte|tut)-[0-9]{3}")))list+=WorkshopImage("$id.png",true,"Ilustración conceptual original; no foto del procedimiento")
    WorkshopCodec.strings(j.opt("assetImages")).filter {WorkshopAssetPaths.legacyName(it) || WorkshopAssetPaths.validModular(it)}.forEach {list+=WorkshopImage(it,true,"Imagen editorial incluida con la aplicación; consulta sus fuentes")}
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
internal fun WorkshopPicture(ref:WorkshopImage?,modifier:Modifier=Modifier,large:Boolean=false,missingMessage:String="Sin imagen disponible",crop:Boolean=false){
    val context=LocalContext.current;var expanded by remember(ref){mutableStateOf(false)};var retry by remember(ref){mutableIntStateOf(0)}
    val loaded by produceState(PictureState(loading=ref!=null),ref,large,retry){
        value=PictureState(loading=ref!=null)
        value=PictureState(withContext(Dispatchers.IO){ref?.let {loadWorkshopBitmap(context,it,if(large)1200 else 400)}},false)
    }
    val bitmap=loaded.bitmap
    Surface(modifier,color=WorkshopColors.Paper,shape=androidx.compose.foundation.shape.RoundedCornerShape(if(large)24.dp else 14.dp)){
        val b=bitmap
        if(b!=null)Image(b.asImageBitmap(),ref?.label ?: "Imagen del proyecto",Modifier.fillMaxSize().clickable {expanded=true},contentScale=if(crop)ContentScale.Crop else ContentScale.Fit)
        else if(loaded.loading)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator(Modifier.size(24.dp))}
        else BoxWithConstraints(Modifier.fillMaxSize()){
            if(maxWidth<100.dp || maxHeight<80.dp){
                Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){
                    if(ref!=null)IconButton(onClick={retry++}){Icon(Icons.Outlined.Refresh,"Imagen no disponible. Reintentar")}
                    else Icon(Icons.Outlined.Image,missingMessage)
                }
            }else Column(Modifier.fillMaxSize().padding(12.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                Icon(Icons.Outlined.Image,null)
                Text(if(ref==null)missingMessage else "Imagen no disponible",style=MaterialTheme.typography.bodySmall)
                if(ref!=null)TextButton(onClick={retry++}){Text("Reintentar")}
            }
        }
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
