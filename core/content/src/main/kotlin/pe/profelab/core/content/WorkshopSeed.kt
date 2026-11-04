package pe.profelab.core.content

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.WorkRecord

class WorkshopSeed(private val context:Context) {
    private fun index():JSONObject = runCatching {context.assets.open("workshop/catalog_index_v2.json").bufferedReader().use {JSONObject(it.readText())}}.getOrElse {throw IllegalStateException("Índice del Taller no disponible",it)}
    private fun sources(key:String):List<String> = WorkshopCodec.strings(index().getJSONArray(key)).also {paths->require(paths.size<=50 && paths.all {it.matches(Regex("[a-zA-Z0-9_-]+\\.json"))})}
    private fun objects(file:String,key:String):List<JSONObject>{
        val text=context.assets.open("workshop/$file").bufferedReader().use {it.readText()};require(text.length<=4_000_000)
        val root=JSONObject(text);val a=root.getJSONArray(key);require(a.length()<=1000);return (0 until a.length()).map {i->a.getJSONObject(i).also {j->if(key=="projects" && !j.has("contentVersion") && root.has("version"))j.put("contentVersion",root.getString("version"))}}
    }
    suspend fun materials():List<pe.profelab.core.model.MaterialDefinition> = withContext(Dispatchers.IO) {
        sources("materialFiles").flatMap {objects(it,"materials")}.map(WorkshopCodec::definition).also {list->require(list.map {it.id}.distinct().size==list.size)}
    }
    suspend fun layouts():List<WorkRecord> = withContext(Dispatchers.IO){
        sources("layoutFiles").flatMap {objects(it,"layouts")}.map {j->require(j.getString("template") in WorkshopCodec.templates);require(WorkshopCodec.strings(j.opt("sectionOrder")).all {it in WorkshopCodec.blocks});WorkRecord(j.getString("id"),"catalog_layout",j.getString("title"),j.toString(),0)}
    }
    suspend fun templates():List<WorkRecord> = withContext(Dispatchers.IO) {
        val assetMedia=runCatching {
            val text=context.assets.open("workshop/media_slots_v1.json").bufferedReader().use {it.readText()};require(text.length<=2_000_000)
            val index=JSONObject(text);require(index.getInt("schemaVersion")==1);index.getJSONArray("resources")
        }.getOrElse {JSONArray()}
        val guideFiles=if(index().has("teacherGuideFiles"))sources("teacherGuideFiles")else emptyList()
        val guideRows=guideFiles.flatMap {objects(it,"guides")}.also {rows->require(rows.map {it.getString("contentId")}.distinct().size==rows.size)}
        val guides=guideRows.associate {it.getString("contentId") to it.getJSONObject("teacherGuide")}
        val mediaFiles=if(index().has("experimentMediaFiles"))sources("experimentMediaFiles")else emptyList()
        val media=mediaFiles.flatMap {objects(it,"media")}.also {rows->require(rows.map {it.getString("contentId")}.distinct().size==rows.size)}.associateBy {it.getString("contentId")}
        (sources("projectFiles").map {it to "project_template"}+sources("experimentFiles").map {it to "experiment_template"}).flatMap { (path,kind)->
            objects(path,"projects").map {j->
                if(!j.has("teacherGuide"))guides[j.getString("id")]?.let {guide->TeacherGuides.validate(guide);j.put("teacherGuide",JSONObject(guide.toString()))}
                media[j.getString("id")]?.let {m->
                    j.put("imageStatus",m.getString("coverStatus")).put("imageMissingDescription",m.getString("altText"))
                    if(!j.has("assetImages"))j.put("assetImages",m.getJSONArray("assetImages"))
                }
                val bindings=JSONArray()
                for(i in 0 until assetMedia.length())runCatching {
                    val m=assetMedia.getJSONObject(i)
                    if(m.optString("contentId")==j.getString("id") && m.optString("contentVersion")==j.optString("contentVersion")){
                        WorkshopMediaBindings.validate(JSONArray().put(m),j.getString("id"),j.getString("contentVersion"))
                        val filename=m.getString("assetFilename");require(filename.matches(Regex("[a-zA-Z0-9_-]+\\.(?:png|jpg|jpeg|webp)")))
                        val bytes=context.assets.open("workshop/images/$filename").use {it.readBytes()};require(bytes.size<=8*1024*1024 && bytes.contentSha().equals(m.getString("sha256"),true))
                        val source=WorkRecord(j.getString("id"),kind,j.getString("title"),j.toString(),0)
                        val proof=WorkshopMediaBindings.fingerprintedRefs(JSONObject().put("contentId",j.getString("id")).put("contentVersion",j.getString("contentVersion")).put("mediaRoleRefs",JSONArray().put(m)),listOf(source))
                        bindings.put(proof.getJSONObject(0))
                    }
                }
                if(bindings.length()>0)j.put("editorialMediaBindings",bindings)
                WorkRecord(j.getString("id"),kind,j.getString("title"),j.toString(),0)
            }
        }.also {list->require(list.map {it.id}.distinct().size==list.size)}
    }
}
