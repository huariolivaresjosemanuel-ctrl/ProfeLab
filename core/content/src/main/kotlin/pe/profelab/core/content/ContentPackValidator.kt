package pe.profelab.core.content

import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*
import java.security.MessageDigest

data class PackResource(val id:String,val bytes:ByteArray,val sha256:String,val source:String,val license:String,val alt:String="",val caption:String="")
data class PackData(val manifest:JSONObject,val records:List<WorkRecord>,val digest:String,val resources:List<PackResource> = emptyList())
internal fun ByteArray.contentSha()=MessageDigest.getInstance("SHA-256").digest(this).joinToString(""){"%02x".format(it)}
internal fun validContentId(value:String)=value.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9._:-]{0,127}"))
internal fun contentVersion(value:String):List<Int>{require(value.matches(Regex("[0-9]{1,6}\\.[0-9]{1,6}\\.[0-9]{1,6}")));return value.split('.').map {it.toInt()}}
internal fun compareContentVersion(a:String,b:String):Int{val x=contentVersion(a);val y=contentVersion(b);return (0..2).firstOrNull {x[it]!=y[it]}?.let {x[it].compareTo(y[it])} ?: 0}
internal object ContentPackValidator {
    private fun json(bytes:ByteArray):JSONObject {
        val text=Charsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes)).toString()
        JsonLimits.requireBounded(text);return JSONObject(text)
    }
    fun inspect(entries:Map<String,ByteArray>):PackData {
        val manifest=json(requireNotNull(entries["manifest.json"]))
        val packId=manifest.getString("packId");require(validContentId(packId));contentVersion(manifest.getString("version"))
        val schema=manifest.getInt("schemaVersion")
        require(schema in 2..3 && manifest.getInt("minimumAppSchema") in 1..2 && manifest.optInt("maxAppSchema",2)>=2)
        require(if(schema==3)manifest.getString("contentContractVersion")=="3.0"else manifest.optString("contentContractVersion","2.0") in setOf("2.0","2.1"))
        if(manifest.has("minimumAppVersion"))require(compareContentVersion(manifest.getString("minimumAppVersion"),"1.0.2")<=0)
        val items=manifest.getJSONArray("items");require(items.length() in 1..180)
        val paths=(0 until items.length()).map {items.getJSONObject(it).getString("path")}
        require(paths.distinct().size==paths.size && entries.keys==paths.toSet()+"manifest.json")
        val resources=mutableListOf<PackResource>();val records=mutableListOf<WorkRecord>();val imageIds=mutableSetOf<String>();val layouts=mutableSetOf<String>()
        for(i in 0 until items.length()){
            val item=items.getJSONObject(i);val bytes=requireNotNull(entries[item.getString("path")]);require(bytes.contentSha()==item.getString("sha256"))
            val type=item.getString("type");require(type in setOf("project","experiment","material","layout","image") || schema==3 && type=="guided_workshop")
            if(type=="image"){
                val id=item.getString("id");require(validContentId(id) && imageIds.add(id));val ext=item.getString("path").substringAfterLast('.').lowercase()
                require(ext in setOf("png","jpg","jpeg","webp") && item.getBoolean("distributionApproved"))
                val source=item.getString("source");val license=item.getString("license");require(source.isNotBlank() && license.isNotBlank())
                val alt=item.optString("alt");val caption=item.optString("caption");require(alt.length<=600 && caption.length<=1000 && (schema<3 || alt.isNotBlank()))
                val b=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,b)
                require(b.outWidth in 1..10000 && b.outHeight in 1..10000 && b.outWidth.toLong()*b.outHeight<=25_000_000)
                require(b.outMimeType==when(ext){"png"->"image/png";"webp"->"image/webp";else->"image/jpeg"})
                var sample=1;while(b.outWidth/sample>400 || b.outHeight/sample>400)sample*=2
                val decoded=BitmapFactory.decodeByteArray(bytes,0,bytes.size,BitmapFactory.Options().apply {inSampleSize=sample})
                require(decoded!=null){"Recurso gráfico ilegible"};decoded.recycle()
                resources+=PackResource(id,bytes,bytes.contentSha(),source,license,alt,caption);continue
            }
            require(bytes.size<=2*1024*1024);val data=json(bytes);val rights=data.optJSONObject("contentRights")
            val origin=data.optString("contentOrigin",rights?.optString("origin").orEmpty())
            require(origin in setOf("EDITORIAL_PROFELAB","USER_AUTHORED") && data.optBoolean("distributionApproved",rights?.optBoolean("approvedForDistribution")==true))
            val id=data.getString("id");require(validContentId(id))
            val key=when(type){"material"->"material:$id";"layout"->"pack:$packId:layout:$id";else->"pack:$packId:$id"}
            val title=if(type=="material")data.getString("name")else data.getString("title")
            require(title.isNotBlank() && title.length<=160 && '|' !in title)
            if(type=="material"){
                require(data.optString("domain","EDUCATIONAL")=="EDUCATIONAL"){"El dominio alimentario requiere una versión con validación específica; no se mezcla con materiales escolares"}
                require(data.optString("materialKind") in MaterialKind.entries.map {it.name});require(data.getString("unit").isNotBlank())
                require(WorkshopCodec.strings(data.opt("synonyms")).size<=30)
            }else if(type=="layout"){
                require(data.getString("template") in WorkshopCodec.templates)
                val order=WorkshopCodec.strings(data.opt("sectionOrder"));require(order.size<=12 && order.all {it in WorkshopCodec.blocks})
                layouts+=id;data.put("id",key)
            }else {
                WorkshopFlowAdapter.validateExtension(data)
                // Imported definitions cannot impersonate personal work or reference private evidence.
                listOf("attachments","records","imageFileIds","imageMetadataByFileId","resourceFileIds","links","citations","selectedAlternatives","originImageId","forkedFromTemplateId","duplicatedFromId","exportImages","results","analysis","conclusion","workshopFlow","classroomAssociation","studentId","groupId").forEach {data.remove(it)}
                if(data.has("teacherGuide"))TeacherGuides.validate(data.getJSONObject("teacherGuide"))
                if(data.has("contentVersion"))contentVersion(data.getString("contentVersion"))
                val category=data.optString("categoryId",if(type=="experiment")"experiment"else "didactic")
                require(category in WorkshopCategories.supported){"Categoría no compatible; MiniChef/FOOD continúa pendiente de revisión específica"}
                require((type=="experiment")== (category=="experiment")){"La categoría de experimento debe usar el tipo experiment"}
                data.put("categoryId",category)
                require(data.optString("visualTemplate",if(type=="experiment")"EXPERIMENT"else "DIDACTIC") in WorkshopCodec.templates)
                if(data.has("grades"))require(WorkshopCodec.strings(data.opt("grades")).all {it.toIntOrNull()?.let {g->g in 1..6}==true})
                val order=WorkshopCodec.strings(data.opt("sectionOrder"));require(order.size<=12 && order.all {it in WorkshopCodec.blocks})
                if(data.has("materials"))require(data.opt("materials") is JSONArray || data.opt("materials") is String)
                val parsed=WorkshopCodec.materials(data,emptyList());require(parsed.size<=100)
                parsed.forEach {line->require('|' !in line.name && '|' !in line.unit);if(line.amount.isNotBlank())require(InventoryRules.decimal(line.amount)?.signum()==1)}
                val steps=data.optJSONArray("steps");if(steps!=null){require(steps.length()<=100);val stepIds=mutableSetOf<String>();for(n in 0 until steps.length()){
                    val raw=steps.get(n)
                    if(raw is JSONObject){require(raw.opt("title") is String && raw.getString("title").length<=4000);if(raw.has("id")){val stepId=raw.getString("id");require(validContentId(stepId) && stepIds.add(stepId))};require(WorkshopCodec.strings(raw.opt("images")).size<=20)
                        if(raw.has("notes") && !raw.has("instructionNote"))raw.put("instructionNote",raw.getString("notes"))
                        require(raw.optString("instructionNote").length<=10000);raw.remove("notes");raw.remove("imageFileIds");raw.put("done",false)
                    }
                    else require(raw is String && raw.length<=4000)
                }}
                if(data.has("assetImages"))require(WorkshopCodec.strings(data.opt("assetImages")).all {it.matches(Regex("[a-zA-Z0-9_-]+\\.(?:png|jpg|jpeg|webp)"))})
                data.put("reviewStatus","PENDING").put("safetyReviewedByUser",false)
                if(data.optString("layoutId").isNotBlank())data.put("layoutId","pack:$packId:layout:${data.getString("layoutId")}")
            }
            data.put("packVersion",manifest.getString("version")).put("sourcePackId",packId)
            records+=WorkRecord(key,if(type=="guided_workshop")"catalog_project"else "catalog_$type",title,data.toString(),System.currentTimeMillis())
        }
        require(records.map {it.id}.distinct().size==records.size)
        records.filter {it.kind in setOf("catalog_project","catalog_experiment")}.forEach {r->val j=JSONObject(r.json)
            val images=WorkshopCodec.strings(j.opt("images"));require(images.size<=20 && images.all {it in imageIds})
            val metadata=j.optJSONObject("imageMetadata")
            metadata?.keys()?.forEach {id->require(id in imageIds);val m=metadata.getJSONObject(id);require(m.optString("alt").length<=600 && m.optString("caption").length<=1000)}
            val layout=j.optString("layoutId");require(layout.isBlank() || records.any {it.kind=="catalog_layout" && it.id==layout})
            val steps=j.optJSONArray("steps") ?: JSONArray();for(i in 0 until steps.length()){val s=steps.optJSONObject(i) ?: continue;require(WorkshopCodec.strings(s.opt("images")).all {it in imageIds})}
        }
        return PackData(manifest,records,requireNotNull(entries["manifest.json"]).contentSha(),resources)
    }
}
