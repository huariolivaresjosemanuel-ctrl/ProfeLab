package pe.profelab.feature.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.Draft
import pe.profelab.core.model.DraftRepository
import java.io.File

/** Serializes draft writes and the final commit, so a queued autosave cannot recreate a cleared draft. */
class BatchOcrDraftModel(private val drafts:DraftRepository,private val documentId:String,private val folder:File):ViewModel(){
    private val key="pdf-ocr:$documentId"
    private val queue=Channel<suspend ()->Unit>(Channel.UNLIMITED)
    private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
    private var finishing=false
    init{viewModelScope.launch {for(operation in queue)try{operation()}catch(e:CancellationException){throw e}catch(_:Exception){_error.value="No se pudo guardar el borrador del lote. Conserva esta pantalla y reintenta."}}}
    suspend fun load():List<ReviewedOcrPage> = try{
        val payload=drafts.readDraft(key)?.payload
        if(payload==null)emptyList()else {
            val a=JSONArray(payload)
            (0 until a.length()).map {i->val j=a.getJSONObject(i);ReviewedOcrPage(j.getInt("page"),j.getString("file"),j.getString("raw"),j.getString("corrected"),j.optBoolean("reviewed"))}
                .filter {val f=File(it.filePath);f.canonicalPath.startsWith(folder.canonicalPath+File.separator) && f.name.matches(Regex("[a-zA-Z0-9-]+\\.png")) && f.isFile}
        }
    }catch(e:CancellationException){throw e}catch(_:Exception){_error.value="No se pudo leer el lote previo; no se ha borrado.";emptyList()}
    fun save(pages:List<ReviewedOcrPage>){
        if(finishing)return
        val a=JSONArray();pages.forEach {a.put(JSONObject().put("page",it.page).put("file",it.filePath).put("raw",it.raw).put("corrected",it.corrected).put("reviewed",it.reviewed))}
        val payload=a.toString()
        queue.trySend {drafts.writeDraft(Draft(key,payload,System.currentTimeMillis()));_error.value=null}
    }
    suspend fun finish(pages:List<ReviewedOcrPage>,commit:suspend ()->Boolean):Boolean {
        finishing=true;val result=CompletableDeferred<Boolean>()
        queue.send {
            try{
                val saved=commit()
                if(saved){drafts.clearDraft(key);pages.forEach {File(it.filePath).delete()}}else finishing=false
                result.complete(saved)
            }catch(e:CancellationException){result.cancel(e);throw e}catch(_:Exception){finishing=false;_error.value="El lote se conserva para reintentar el guardado.";result.complete(false)}
        }
        return result.await()
    }
}
