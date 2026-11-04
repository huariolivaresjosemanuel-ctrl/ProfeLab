package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.MaterialDefinition
import pe.profelab.core.model.ActivityStepView
import java.security.MessageDigest

data class WorkshopBoundMedia(val mediaId:String,val storageId:String,val asset:Boolean,val alt:String,val caption:String,
    val sha256:String,val revision:Int,val role:String)

/** Editorial slots are read-only overlays. They never become attachments or observations. */
object WorkshopMediaBindings {
    val sectors=setOf("experiments","taller","cooking","dance","heritage","ideas")
    val roles=setOf("catalog_cover","detail_hero","material_icon","step_photo","outcome_reference","teacher_reference","sector_icon")
    fun ratio(role:String):Float=when(role){"catalog_cover","teacher_reference"->3f/2f;"material_icon","sector_icon"->1f;else->16f/9f}
    fun contentId(record:WorkRecord):String {
        val j=JSONObject(record.json)
        if(record.kind !in setOf("project","experiment"))return record.id
        return j.optString("forkedFromTemplateId").ifBlank {j.optString("activityId").ifBlank {record.id}}
    }
    fun version(record:WorkRecord):String {val j=JSONObject(record.json);return j.optString("contentVersion").ifBlank {j.optString("packVersion","legacy")}}
    fun stepFingerprint(step:ActivityStepView):String=MessageDigest.getInstance("SHA-256").digest(JSONArray(listOf(step.title,step.instruction)).toString().toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    fun preferredPrivateCover(record:WorkRecord):String? {
        if(record.kind !in setOf("project","experiment"))return null
        val j=JSONObject(record.json);val selected=j.optJSONObject("workshopFlow")?.optString("preferredCoverFileId").orEmpty()
        return selected.takeIf {it in WorkshopEvidenceData.privateImageIds(j)}
    }
    fun slotKey(row:JSONObject)=listOf(row.getString("role"),row.optString("stepId"),row.optString("materialId")).joinToString("|")
    fun validate(rows:JSONArray,contentId:String,version:String,imageIds:Set<String>?=null){
        require(validContentId(contentId));contentVersion(version);require(rows.length() in 1..160)
        val slots=mutableSetOf<String>();val ids=mutableSetOf<String>()
        for(i in 0 until rows.length()){
            val r=rows.getJSONObject(i);val role=r.getString("role");require(role in roles)
            require(r.getString("contentId")==contentId && r.getString("contentVersion")==version)
            require(validContentId(r.getString("mediaId")) && ids.add(r.getString("mediaId")))
            require(slots.add(slotKey(r))){"Dos imágenes ocupan el mismo slot; revisa la revisión del medio"}
            require(r.getString("reviewStatus")=="APPROVED_BY_USER" && r.getString("safetyReviewStatus") in setOf("APPROVED","APPROVED_BY_USER","NOT_REQUIRED")){"El medio necesita aprobación editorial y de seguridad"}
            if(role=="step_photo")require(validContentId(r.getString("stepId")))else require(r.optString("stepId").isBlank())
            if(role=="material_icon")require(validContentId(r.getString("materialId")))else require(r.optString("materialId").isBlank())
            require(r.getString("altText").isNotBlank() && r.getString("altText").length<=600 && r.optString("caption").length<=1000)
            require(r.getString("sha256").matches(Regex("[a-fA-F0-9]{64}")))
            require(r.optInt("mediaRevision",1) in 1..1000000)
            require(r.getString("sourceType").isNotBlank() && r.getString("licenseOrRights").isNotBlank())
            imageIds?.let {require(r.getString("mediaId") in it)}
        }
    }
    fun validateTarget(data:JSONObject,definitions:List<WorkRecord>,catalog:List<MaterialDefinition> = emptyList()){
        val id=data.getString("contentId");val version=data.getString("contentVersion");val rows=data.getJSONArray("mediaRoleRefs")
        validate(rows,id,version)
        if(id.startsWith("sector:")){
            require(id.removePrefix("sector:") in sectors && version=="1.0.0")
            require((0 until rows.length()).all {rows.getJSONObject(it).getString("role")=="sector_icon"});return
        }
        val source=definitions.singleOrNull {it.kind in setOf("project_template","experiment_template","catalog_project","catalog_experiment") && !it.archived && contentId(it)==id && WorkshopMediaBindings.version(it)==version}
        require(source!=null){"Actividad o versión desconocida; no se vincula por título"}
        val steps=WorkshopFlowAdapter.read(source).steps.associateBy {it.id}
        val materials=WorkshopCodec.materials(JSONObject(source.json),catalog).map {it.materialId}.toSet()
        for(i in 0 until rows.length()){val r=rows.getJSONObject(i)
            when(r.getString("role")){"step_photo"->{
                val step=requireNotNull(steps[r.getString("stepId")]){"El paso no pertenece a la actividad"}
                if(r.has("stepFingerprint"))require(r.getString("stepFingerprint")==stepFingerprint(step)){"El texto del paso cambió; revisa la fotografía antes de vincularla"}
            };"material_icon"->require(r.getString("materialId") in materials){"El material no pertenece a la actividad"};"sector_icon"->error("Un icono de sector no es una foto de actividad")}
        }
    }
    fun fingerprintedRefs(data:JSONObject,definitions:List<WorkRecord>,catalog:List<MaterialDefinition> = emptyList()):JSONArray {
        validateTarget(data,definitions,catalog)
        val id=data.getString("contentId");val version=data.getString("contentVersion")
        val source=definitions.singleOrNull {it.kind in setOf("project_template","experiment_template","catalog_project","catalog_experiment") && !it.archived && contentId(it)==id && WorkshopMediaBindings.version(it)==version}
        val steps=source?.let {WorkshopFlowAdapter.read(it).steps.associateBy {s->s.id}}.orEmpty()
        val rows=data.getJSONArray("mediaRoleRefs")
        return JSONArray().also {a->for(i in 0 until rows.length()){
            val r=JSONObject(rows.getJSONObject(i).toString())
            if(r.getString("role")=="step_photo")r.put("stepFingerprint",stepFingerprint(requireNotNull(steps[r.getString("stepId")])))
            a.put(r)
        }}
    }
    fun resolve(record:WorkRecord,role:String,stepId:String?=null,materialId:String?=null,overlays:List<WorkRecord> = emptyList()):WorkshopBoundMedia? {
        if(role !in roles)return null
        return runCatching {
            val id=contentId(record);val version=version(record);val json=JSONObject(record.json)
            // A changed step list must not receive a photograph for a removed or unrelated step.
            if(role=="step_photo" && WorkshopFlowAdapter.read(record).steps.none {it.id==stepId})return null
            val candidates=mutableListOf<WorkshopBoundMedia>()
            fun add(rows:JSONArray?){
                if(rows==null)return
                for(i in 0 until rows.length()){
                    val r=rows.optJSONObject(i) ?: continue
                    if(r.optString("contentId")!=id || r.optString("contentVersion")!=version || r.optString("role")!=role || r.optString("stepId")!=stepId.orEmpty() || r.optString("materialId")!=materialId.orEmpty())continue
                    if(runCatching {validate(JSONArray().put(r),id,version)}.isFailure)continue
                    if(role=="step_photo"){
                        val step=WorkshopFlowAdapter.read(record).steps.firstOrNull {it.id==stepId} ?: continue
                        if(r.optString("stepFingerprint")!=stepFingerprint(step))continue
                    }
                    val asset=r.optString("assetFilename");val file=r.optString("fileId")
                    if(asset.isNotBlank() && file.isNotBlank())continue
                    if(asset.isNotBlank() && !asset.matches(Regex("[a-zA-Z0-9_-]+\\.(?:png|jpg|jpeg|webp)")))continue
                    if(asset.isBlank() && !file.matches(Regex("[a-zA-Z0-9-]+")))continue
                    candidates+=WorkshopBoundMedia(r.getString("mediaId"),asset.ifBlank {file},asset.isNotBlank(),r.getString("altText"),r.optString("caption"),r.getString("sha256").lowercase(),r.optInt("mediaRevision",1),role)
                }
            }
            add(json.optJSONArray("editorialMediaBindings"))
            overlays.filter {it.kind=="catalog_media" && !it.archived}.forEach {overlay->
                val j=JSONObject(overlay.json)
                if(j.optString("contentId")==id && j.optString("contentVersion")==version)add(j.optJSONArray("editorialMediaBindings"))
            }
            val revision=candidates.maxOfOrNull {it.revision} ?: return null
            val current=candidates.filter {it.revision==revision}
            // Equal revisions with different bytes are ambiguous; do not pick by timestamp or title.
            if(current.map {it.sha256}.distinct().size!=1)null else current.first()
        }.getOrNull()
    }
}
