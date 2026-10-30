package pe.profelab.core.content

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.files.PrivateFiles
import pe.profelab.core.model.*
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipInputStream

class ContentPacks(private val context:Context,private val store:WorkspaceStore){
    suspend fun inspect(uri:String):PackData=withContext(Dispatchers.IO){
        val entries=linkedMapOf<String,ByteArray>();var total=0
        context.contentResolver.openInputStream(Uri.parse(uri))!!.use {input->ZipInputStream(input).use {zip->
            while(true){val entry=zip.nextEntry ?: break;val name=entry.name
                require(!entry.isDirectory && !name.startsWith('/') && !name.contains('\\') && !name.contains(':') && name.split('/').none {it in setOf("..",".") || it.isBlank()} && name !in entries && entries.size<200)
                val limit=if(name.substringAfterLast('.').lowercase() in setOf("png","jpg","jpeg","webp"))8*1024*1024 else 2*1024*1024
                val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
                while(true){val n=zip.read(buffer);if(n<0)break;require(out.size()+n<=limit && total+n<=40*1024*1024);out.write(buffer,0,n);total+=n}
                entries[name]=out.toByteArray();zip.closeEntry()
            }
        }}
        ContentPackValidator.inspect(entries)
    }
    private fun compatible(previous:WorkRecord?,pack:PackData):Boolean {
        if(previous==null)return true
        val old=JSONObject(previous.json)
        if(old.optString("digest")==pack.digest)return false
        require(compareContentVersion(pack.manifest.getString("version"),old.getString("version"))>0){"Misma versión con contenido distinto o retroceso de paquete"}
        return true
    }
    suspend fun install(pack:PackData)=withContext(Dispatchers.IO){
        val markerId="pack:${pack.manifest.getString("packId")}"
        if(!compatible(store.record(markerId),pack))return@withContext
        val created=mutableListOf<File>();val refs=mutableMapOf<String,String>();val byHash=mutableMapOf<String,String>()
        try {
            for(resource in pack.resources){val existing=byHash[resource.sha256]
                val id=existing ?: PrivateFiles(context).copyFrom(ByteArrayInputStream(resource.bytes),8L*1024*1024).let {created+=it.file;byHash[resource.sha256]=it.id;it.id}
                refs[resource.id]=id
            }
            val installed=pack.records.map {record->val j=JSONObject(record.json)
                if(record.kind in setOf("catalog_project","catalog_experiment")){
                    j.put("imageFileIds",JSONArray(WorkshopCodec.strings(j.opt("images")).map {requireNotNull(refs[it])}))
                    val steps=j.optJSONArray("steps") ?: JSONArray();for(i in 0 until steps.length()){val s=steps.optJSONObject(i) ?: continue;s.put("imageFileIds",JSONArray(WorkshopCodec.strings(s.opt("images")).map {requireNotNull(refs[it])}))}
                    j.put("resourceCredits",JSONArray().also {a->pack.resources.forEach {a.put(JSONObject().put("id",it.id).put("source",it.source).put("license",it.license))}})
                }
                record.copy(json=j.toString())
            }
            val bundled=WorkshopSeed(context).materials();var applied=false
            store.editRecords {current->
                if(!compatible(current.find {it.id==markerId},pack))emptyList()else {
                    val definitions=installed.filter {it.kind=="catalog_material"}.map {WorkshopCodec.definition(JSONObject(it.json))}
                    val before=bundled+current.filter {it.kind in setOf("catalog_material","user_material")}.map {WorkshopCodec.definition(JSONObject(it.json))}+definitions
                    definitions.forEach {d->
                        val same=before.filter {it.id==d.id};same.forEach {old->require(InventoryRules.canonical(old.name)==InventoryRules.canonical(d.name) && old.kind==d.kind && InventoryRules.convert(java.math.BigDecimal.ONE,old.unit,d.unit)!=null){"El ID de material entra en conflicto con su definición existente"}}
                        (listOf(d.name)+d.synonyms).forEach {alias->require(before.none {it.id!=d.id && (listOf(it.name)+it.synonyms).any {name->InventoryRules.canonical(name)==InventoryRules.canonical(alias)}}){"Nombre o sinónimo ambiguo entre materiales"}}
                    }
                    applied=true
                    installed+WorkRecord(markerId,"content_pack",pack.manifest.getString("packId"),JSONObject().put("version",pack.manifest.getString("version")).put("digest",pack.digest).put("resourceFileIds",JSONArray(refs.values.distinct())).toString(),System.currentTimeMillis())
                }
            }
            if(!applied)created.forEach {it.delete()}
        }catch(failure:Throwable){created.forEach {it.delete()};throw failure}
    }
}
