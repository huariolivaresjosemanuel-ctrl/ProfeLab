package pe.profelab.core.content

import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*

/** Projection only: opening a definition never creates a run or rewrites legacy JSON. */
object WorkshopFlowAdapter {
    const val VERSION=1
    fun read(record:WorkRecord):ActivityViewState {
        val j=JSONObject(record.json);val flow=j.optJSONObject("workshopFlow") ?: JSONObject()
        val personal=record.kind in setOf("project","experiment")
        val activityId=j.optString("activityId").ifBlank {j.optString("forkedFromTemplateId").ifBlank {record.id}}
        val steps=j.optJSONArray("steps") ?: JSONArray();val used=mutableSetOf<String>()
        val views=(0 until steps.length()).map {i->
            val s=steps.optJSONObject(i) ?: JSONObject().put("title",steps.optString(i))
            var id=s.optString("id").ifBlank {"step-$i"};while(!used.add(id))id+="-$i"
            ActivityStepView(id,s.optString("title"),s.optString("instructionNote"),personal && s.optBoolean("done"),WorkshopCodec.strings(s.opt("imageFileIds")),s.optString("notes"))
        }
        val explicit=j.optJSONArray("observationCriteria")
        val criteria=if(explicit!=null)(0 until explicit.length()).mapNotNull {i->
            val c=explicit.optJSONObject(i) ?: return@mapNotNull null;val label=c.optString("label",c.optString("description"))
            if(label.isBlank())null else ObservationCriterion(c.optString("criterionId").ifBlank {criterionId(activityId,label)},label,c.optString("status","EDITORIAL_PROPOSAL"))
        }else WorkshopCodec.strings(TeacherGuides.resolve(record).opt("formativeEvidence")).filter {it.isNotBlank()}.map {ObservationCriterion(criterionId(activityId,it),it)}
        val perspective=WorkshopPerspective.entries.firstOrNull {it.name==flow.optString("perspective")} ?: WorkshopPerspective.PRACTICAL
        return ActivityViewState(activityId,record.id,j.optString("contentVersion").ifBlank {j.optString("packVersion","legacy")},j.optString("categoryId",if(record.kind.contains("experiment"))"experiment"else "didactic"),record.title,
            j.optString("objectiveSuggested").ifBlank {j.optString("researchQuestion")}.ifBlank {j.optJSONObject("pedagogy")?.optJSONObject("practical")?.optString("purpose").orEmpty()},personal,record.kind.contains("experiment"),views,
            flow.optString("lastVisitedStepId").takeIf {it.isNotBlank()},perspective,criteria.distinctBy {it.id})
    }
    private fun criterionId(activityId:String,text:String)="$activityId:criterion:"+UUID.nameUUIDFromBytes(text.toByteArray(Charsets.UTF_8))
    fun visit(record:WorkRecord,stepId:String):JSONObject {
        require(read(record).personal && read(record).steps.any {it.id==stepId})
        return JSONObject(record.json).also {j->val flow=j.optJSONObject("workshopFlow") ?: JSONObject();j.put("workshopFlow",flow.put("version",VERSION).put("lastVisitedStepId",stepId))}
    }
    fun mark(record:WorkRecord,stepId:String,done:Boolean):JSONObject {
        val view=read(record);require(view.personal);val index=view.steps.indexOfFirst {it.id==stepId};require(index>=0)
        val j=JSONObject(record.json);val a=j.getJSONArray("steps");val s=a.optJSONObject(index) ?: JSONObject().put("title",a.optString(index));a.put(index,JSONObject(s.toString()).put("id",stepId).put("done",done));j.put("steps",a)
        j.put("status",if((0 until a.length()).all {a.optJSONObject(it)?.optBoolean("done")==true})"COMPLETADO"else "EN_PROGRESO")
        return j
    }
    /** A sector is active only for content already supported and successfully imported. */
    fun sector(record:WorkRecord):String=when(read(record).categoryId){"experiment"->"experiments";"dance"->"dance";"heritage"->"heritage";else->"taller"}
    fun shoppingText(record:WorkRecord,assessment:WorkshopAssessment):String = buildString {
        append("ProfeLab | Materiales faltantes para: ").append(record.title).append('\n')
        val missing=assessment.coverage.filterNot {it.covered}
        if(missing.isEmpty())append("Sin faltantes calculados.\n")
        missing.forEach {c->val quantity=c.missing;append("• ").append(c.requirement.name).append(" — ").append(if(c.review.isNotBlank() || quantity==null)"por revisar"else quantity.stripTrailingZeros().toPlainString()).append(' ').append(c.requirement.unit).append('\n')}
        append("Verifica cantidades y unidades antes de comprar.")
    }
    /** Optional extension for future content packs; legacy contracts remain accepted. */
    fun validateExtension(j:JSONObject){
        if(!j.has("activityContractVersion"))return
        require(j.getInt("activityContractVersion")==VERSION)
        require(validContentId(j.getString("id")))
        val criteria=j.optJSONArray("observationCriteria") ?: JSONArray();require(criteria.length()<=40)
        val ids=mutableSetOf<String>();for(i in 0 until criteria.length()){val c=criteria.getJSONObject(i);require(validContentId(c.getString("criterionId")) && ids.add(c.getString("criterionId")));require(c.getString("label").isNotBlank() && c.getString("label").length<=2000)}
        // Catalog content must never ship local run state or private future associations.
        require(!j.has("workshopFlow") && !j.has("classroomAssociation") && !j.has("studentId") && !j.has("groupId"))
    }
}
