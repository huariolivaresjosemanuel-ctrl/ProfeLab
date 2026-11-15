package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord

/** General activity checklist already used by Classroom; never an individual assessment. */
object WorkshopFormativeData {
    fun update(record:WorkRecord,criterionIds:Set<String>,observed:Set<String>,note:String?):JSONObject {
        val view=WorkshopFlowAdapter.read(record)
        require(view.personal && !record.archived && !EducationProgress.hasSelection(record)) {"Revisa el contexto de la observación"}
        require((note==null || note.length<=6000) && observed.all {it in criterionIds})
        require(criterionIds.all {id->view.criteria.any {it.id==id}}) {"Los criterios cambiaron; vuelve a abrir la observación"}
        val j=JSONObject(record.json);val flow=j.optJSONObject("workshopFlow") ?: JSONObject()
        val previous=WorkshopCodec.strings(flow.opt("observedCriterionIds")).toSet()
        flow.put("version",1).put("observedCriterionIds",JSONArray((previous-criterionIds+observed).sorted()))
        if(note!=null)flow.put("teacherReflection",note)
        val snapshots=flow.optJSONObject("criteriaById") ?: JSONObject()
        view.criteria.filter {it.id in criterionIds}.forEach {c->snapshots.put(c.id,JSONObject().put("label",c.label).put("status",c.reviewStatus))}
        return j.put("workshopFlow",flow.put("criteriaById",snapshots))
    }
}
