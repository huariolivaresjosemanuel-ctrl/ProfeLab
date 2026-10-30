package pe.profelab.feature.ocr

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.profelab.core.model.Draft
import pe.profelab.core.model.DraftRepository
import org.json.JSONObject

class ReviewDraftModel(private val drafts:DraftRepository):ViewModel() {
    private sealed interface Operation {
        data class Save(val payload:String):Operation
        data class Barrier(val result:CompletableDeferred<Boolean>):Operation
    }
    private val queue=Channel<Operation>(Channel.UNLIMITED)
    private val _error=MutableStateFlow<String?>(null);val error=_error.asStateFlow()
    init {viewModelScope.launch {for(operation in queue)when(operation){
        is Operation.Save->try{drafts.writeDraft(Draft("ocr-review",operation.payload,System.currentTimeMillis()));_error.value=null}catch(e:CancellationException){throw e}catch(_:Exception){_error.value="No se pudo guardar el borrador OCR. Revisa el almacenamiento antes de salir."}
        is Operation.Barrier->operation.result.complete(_error.value==null)
    }}}
    suspend fun load(uri:String):JSONObject?=try {drafts.readDraft("ocr-review")?.let {JSONObject(it.payload)}?.takeIf {it.optString("uri")==uri || it.optString("sourceUri")==uri}}
        catch(e:CancellationException){throw e}catch(_:Exception){_error.value="No se pudo recuperar el borrador previo; se conserva en almacenamiento hasta confirmar otro guardado.";null}
    fun save(uri:String,raw:String,text:String,owner:String?,documentId:String?,pageIndex:Int){queue.trySend(Operation.Save(JSONObject().put("uri",uri).put("raw",raw).put("text",text).put("owner",owner).put("documentId",documentId).put("pageIndex",pageIndex).toString()))}
    /** Await all prior writes before navigation or clearing the reviewed draft. */
    suspend fun flush():Boolean {val result=CompletableDeferred<Boolean>();queue.send(Operation.Barrier(result));return result.await()}
}
