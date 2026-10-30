package pe.profelab.app

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.profelab.core.files.PrivateFiles
import pe.profelab.core.model.Draft
import pe.profelab.core.model.DraftRepository
import java.io.File

/** Storage adapter only. The recognizer, crop/rotation and reviewed-text confirmation are unchanged. */
class RetainedOcrDrafts(private val context:Context,private val delegate:DraftRepository):DraftRepository {
    private val mutex=Mutex();private var retained:Pair<String,String>?=null
    private val _warning=MutableStateFlow<String?>(null);val warning=_warning.asStateFlow()
    private fun file(id:String)=id.takeIf {it.matches(Regex("[a-zA-Z0-9-]+"))}?.let {File(context.filesDir,"documents/$it")}
    override suspend fun readDraft(key:String):Draft? {
        val draft=delegate.readDraft(key) ?: return null
        if(key!="ocr-review")return draft
        val data=JSONObject(draft.payload);val saved=file(data.optString("imageFileId"))
        if(saved?.isFile==true)data.put("uri",Uri.fromFile(saved).toString())
        return draft.copy(payload=data.toString())
    }
    override suspend fun writeDraft(draft:Draft){
        if(draft.key!="ocr-review"){delegate.writeDraft(draft);return}
        mutex.withLock {
            val data=JSONObject(draft.payload);val source=data.getString("uri")
            val before=try {readDraft(draft.key)?.let {JSONObject(it.payload)}}catch(e:CancellationException){throw e}catch(_:Exception){null}
            val previous=before?.takeIf {it.optString("uri")==source || it.optString("sourceUri")==source}
            var id=previous?.optString("imageFileId").orEmpty().takeIf {file(it)?.isFile==true}
                ?: retained?.takeIf {it.first==source && file(it.second)?.isFile==true}?.second
            if(id==null)try {
                val copied=withContext(Dispatchers.IO){
                    val uri=Uri.parse(source);require(uri.scheme in setOf("file","content"))
                    val input=requireNotNull(context.contentResolver.openInputStream(uri))
                    PrivateFiles(context).copyFrom(input,30L*1024*1024)
                }
                id=copied.id;retained=source to copied.id
            }catch(e:CancellationException){throw e}catch(_:Exception){_warning.value="El texto se conserva en borrador, pero no se pudo retener su imagen para el respaldo. Revisa acceso y espacio."}
            if(id!=null){
                data.put("sourceUri",previous?.optString("sourceUri").orEmpty().ifBlank {source})
                    .put("uri",Uri.fromFile(requireNotNull(file(id))).toString()).put("imageFileId",id)
                data.remove("imageRetentionIssue");_warning.value=null
            }else data.put("imageRetentionIssue",true)
            delegate.writeDraft(draft.copy(payload=data.toString()))
        }
    }
    override suspend fun clearDraft(key:String){mutex.withLock {delegate.clearDraft(key);if(key=="ocr-review"){retained=null;_warning.value=null}}}
}
