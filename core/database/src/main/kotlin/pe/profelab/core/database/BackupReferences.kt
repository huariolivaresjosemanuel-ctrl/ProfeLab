package pe.profelab.core.database

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.JsonLimits

/** Explicit media fields only: a material ID or a WorkRecord ID is not a file path. */
internal object BackupReferences {
    fun ids(data:JSONObject):Set<String> {
        val found=linkedSetOf<String>()
        fun add(id:String){if(id.isNotBlank()){require(id.matches(Regex("[a-zA-Z0-9-]+"))){"Referencia de archivo privado inválida"};found+=id}}
        fun array(a:JSONArray?){if(a!=null)for(i in 0 until a.length())add(a.getString(i))}
        val rows=data.getJSONArray("workspace_records")
        for(i in 0 until rows.length()){
            val row=rows.getJSONObject(i);val text=row.getString("json");JsonLimits.requireBounded(text);val body=JSONObject(text)
            if(row.optString("kind")=="document")add(body.optString("fileId"))
            array(body.optJSONArray("imageFileIds"));array(body.optJSONArray("resourceFileIds"))
            val attachments=body.optJSONArray("attachments") ?: JSONArray()
            for(n in 0 until attachments.length())attachments.optJSONObject(n)?.let {add(it.optString("id"))}
            val steps=body.optJSONArray("steps") ?: JSONArray()
            for(n in 0 until steps.length())steps.optJSONObject(n)?.let {array(it.optJSONArray("imageFileIds"))}
            val observations=body.optJSONArray("records") ?: JSONArray()
            for(n in 0 until observations.length())observations.optJSONObject(n)?.let {array(it.optJSONArray("photoAttachmentIds"))}
        }
        val drafts=data.optJSONArray("drafts") ?: JSONArray()
        for(i in 0 until drafts.length()){
            val row=drafts.getJSONObject(i)
            if(row.optString("key")=="ocr-review"){
                val payload=row.optString("payload")
                // Historic drafts may contain plain text; never discard that text during export.
                val body=try {JsonLimits.requireBounded(payload);JSONObject(payload)}catch(_:Exception){null}
                body?.optString("imageFileId")?.let {add(it)}
            }
        }
        return found
    }
    fun unretainedOcrDrafts(data:JSONObject):Int {
        val drafts=data.optJSONArray("drafts") ?: return 0
        return (0 until drafts.length()).count {i->val row=drafts.getJSONObject(i)
            row.optString("key")=="ocr-review" && try {val text=row.optString("payload");JsonLimits.requireBounded(text);JSONObject(text).optString("imageFileId").isBlank()}catch(_:Exception){true}
        }
    }
    fun batchPages(payload:String):JSONArray?=try {JsonLimits.requireBounded(payload);JSONArray(payload)}catch(_:Exception){null}
    fun batchName(path:String):String?=path.substringAfterLast('/').takeIf {it.matches(Regex("[a-zA-Z0-9-]+\\.png"))}
    fun batchNames(data:JSONObject):Set<String> {
        val result=linkedSetOf<String>();val drafts=data.optJSONArray("drafts") ?: return result
        for(i in 0 until drafts.length()){
            val row=drafts.getJSONObject(i)
            if(row.optString("key").startsWith("pdf-ocr:"))batchPages(row.optString("payload"))?.let {pages->
                for(n in 0 until pages.length())pages.optJSONObject(n)?.let {page->batchName(page.optString("file"))?.let {result+=it}}
            }
        }
        return result
    }
}
