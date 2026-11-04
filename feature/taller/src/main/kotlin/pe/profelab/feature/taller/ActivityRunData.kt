package pe.profelab.feature.taller

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopEvidenceData

/** A new execution keeps reusable teaching content, never a previous execution's evidence. */
object ActivityRunData {
    fun freshPayload(source:JSONObject):JSONObject {
        val data=JSONObject(source.toString())
        val privateIds=WorkshopEvidenceData.privateImageIds(data).toSet()
        fun clearPrivateImages(container:JSONObject){
            if(container.has("imageFileIds"))container.put("imageFileIds",JSONArray(WorkshopCodec.strings(container.opt("imageFileIds")).filterNot {it in privateIds}))
            container.optJSONObject("imageMetadataByFileId")?.let {metadata->privateIds.forEach {metadata.remove(it)}}
        }
        clearPrivateImages(data)
        val steps=data.optJSONArray("steps") ?: JSONArray()
        for(i in 0 until steps.length())steps.optJSONObject(i)?.apply {
            put("done",false);remove("notes");clearPrivateImages(this)
        }
        listOf("workshopFlow","classroomAssociation","studentId","groupId").forEach {data.remove(it)}
        data.put("status","BORRADOR").put("safetyReviewedByUser",false)
        data.put("exportImages",false)
        data.put("records",JSONArray()).put("attachments",JSONArray())
        listOf("results","analysis","limitations","conclusion","hypothesis").forEach {data.put(it,"")}
        return data
    }

    /** Explicit creation from a window returns there; duplication/new ideas open the editor. */
    fun initialDestination(sourceExists:Boolean,duplicate:Boolean,origin:String):String=when {
        !sourceExists || duplicate->"editor"
        origin in setOf("steps","results","classroom")->origin
        else->"materials"
    }
}
