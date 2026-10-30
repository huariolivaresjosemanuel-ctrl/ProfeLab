package pe.profelab.app

import org.json.JSONObject
import pe.profelab.core.model.*

internal object AssistantRecords {
    fun pendingWork(records:List<WorkRecord>)=records.filter {r->!r.archived && r.kind in setOf("project","experiment","session") && try {
        val body=JSONObject(r.json);val steps=body.optJSONArray("steps")
        body.optString("status") in setOf("BORRADOR","EN_PROGRESO") && !(steps!=null && steps.length()>0 && (0 until steps.length()).all {steps.optJSONObject(it)?.optBoolean("done")==true})
    }catch(_:Exception){false}}.maxByOrNull {it.updatedAt}
    fun facts(records:List<WorkRecord>,currentProject:Boolean):AssistantFacts {
        val live=records.filterNot {it.archived}
        return AssistantFacts(live.any {it.kind=="inventory"},pendingWork(live)!=null,live.any {it.kind=="shopping_v2"},live.any {it.kind=="document"},currentProject,records.any {it.kind in setOf("project","experiment")})
    }
}
