package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.InventoryRules

/** Private evidence is identified by its stored attachment relationship, never its display label. */
object WorkshopEvidenceData {
    fun privateImageIds(data:JSONObject):List<String> {
        val attachments=data.optJSONArray("attachments") ?: JSONArray()
        return (0 until attachments.length()).mapNotNull {i->
            val a=attachments.optJSONObject(i) ?: return@mapNotNull null
            a.optString("id").takeIf {a.optString("mime").startsWith("image/") && it.matches(Regex("[a-zA-Z0-9-]+"))}
        }.distinct()
    }
    fun validMeasurement(value:String,unit:String):Boolean {
        if(value.isBlank())return true
        val normalized=value.trim()
        return normalized.length<=64 && normalized.matches(Regex("[+-]?(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)")) && unit.isNotBlank() && unit.length<=80 && InventoryRules.decimal(normalized.removePrefix("-"))!=null
    }
    /** Stable IDs tolerate unrelated reordering; legacy rows require the exact original array. */
    fun removeObservation(current:JSONArray,id:String?,rowSnapshot:String,arraySnapshot:String,originalIndex:Int):JSONArray {
        val index=if(!id.isNullOrBlank()){
            val matches=(0 until current.length()).filter {current.optJSONObject(it)?.optString("id")==id}
            require(matches.size==1){"El registro cambió o su identificador no es único"}
            matches.single().also {require(current.get(it).toString()==rowSnapshot){"El registro fue modificado; revisa antes de quitarlo"}}
        }else {
            require(current.toString()==arraySnapshot && originalIndex in 0 until current.length() && current.get(originalIndex).toString()==rowSnapshot){"La lista antigua cambió; vuelve a seleccionar el registro"}
            originalIndex
        }
        return JSONArray().also {result->for(i in 0 until current.length())if(i!=index)result.put(current.get(i))}
    }
}
