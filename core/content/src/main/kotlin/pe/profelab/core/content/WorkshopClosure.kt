package pe.profelab.core.content

import org.json.JSONObject
import pe.profelab.core.model.WorkRecord

/** Additive, scoped closure receipt. It never marks steps, invents evidence or changes stock. */
object WorkshopClosure {
    private fun scope(record:WorkRecord)=if(EducationProgress.hasSelection(record))EducationProgress.selectedId(record)else "personal"
    fun savedAt(record:WorkRecord):Long=JSONObject(record.json).optJSONObject("workshopFlow")?.optJSONObject("closuresByRun")?.optJSONObject(scope(record))?.optLong("savedAt",0) ?: 0
    fun finish(record:WorkRecord,operationId:String,at:Long):WorkRecord {
        require(record.kind in setOf("project","experiment") && !record.archived && at>0)
        require(java.util.UUID.fromString(operationId).toString()==operationId)
        val j=JSONObject(record.json);val flow=j.optJSONObject("workshopFlow") ?: JSONObject()
        val receipts=flow.optJSONObject("closuresByRun") ?: JSONObject();val key=scope(record)
        if(receipts.optJSONObject(key)?.optString("operationId")==operationId)return record
        receipts.put(key,JSONObject().put("operationId",operationId).put("savedAt",at))
        return record.copy(json=j.put("workshopFlow",flow.put("version",1).put("closuresByRun",receipts)).toString(),updatedAt=at)
    }
}
