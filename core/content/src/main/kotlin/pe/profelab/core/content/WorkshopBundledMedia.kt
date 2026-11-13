package pe.profelab.core.content

import android.content.Context
import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.MaterialDefinition
import pe.profelab.core.model.WorkRecord
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Packaged editorial media are a read projection, never personal records or inventory. */
class WorkshopBundledMedia(private val context:Context) {
    private data class AssetFacts(val sha256:String,val bytes:Long,val width:Int,val height:Int,val mime:String)
    private val byContent:Map<String,List<JSONObject>> = runCatching {
        val text=context.assets.open("workshop/media_slots_v1.json").bufferedReader().use {it.readText()}
        require(text.length<=2_000_000)
        val index=JSONObject(text);require(index.getInt("schemaVersion")==1)
        val rows=index.getJSONArray("resources");require(rows.length()<=4000)
        (0 until rows.length()).map {rows.getJSONObject(it)}.groupBy {it.getString("contentId")}
    }.getOrElse {emptyMap()}

    /** A damaged/missing file rejects this slot only; no other image is substituted. */
    fun verifiesAsset(row:JSONObject):Boolean=runCatching {
        val filename=WorkshopAssetPaths.reference(row)
        val canonical=WorkshopAssetPaths.canonical(context,filename)
        val expected=row.getString("sha256").lowercase();require(expected.matches(Regex("[a-f0-9]{64}")))
        val key="$canonical:$expected"
        val facts=verified[key] ?: run {
            val digest=MessageDigest.getInstance("SHA-256");var count=0L
            WorkshopAssetPaths.open(context,filename).use {input->
                val buffer=ByteArray(65536)
                while(true){val n=input.read(buffer);if(n<0)break;count+=n;require(count<=8L*1024*1024);digest.update(buffer,0,n)}
            }
            require(count>0)
            val hash=digest.digest().joinToString(""){"%02x".format(it)};require(hash==expected)
            val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true}
            WorkshopAssetPaths.open(context,filename).use {BitmapFactory.decodeStream(it,null,bounds)}
            require(bounds.outWidth in 1..10000 && bounds.outHeight in 1..10000 && bounds.outWidth.toLong()*bounds.outHeight<=25_000_000)
            AssetFacts(hash,count,bounds.outWidth,bounds.outHeight,requireNotNull(bounds.outMimeType)).also {verified[key]=it}
        }
        facts.sha256==expected && facts.width==row.getInt("width") && facts.height==row.getInt("height") &&
            facts.mime==row.getString("mime") && (!row.has("byteSize") || facts.bytes==row.getLong("byteSize"))
    }.getOrDefault(false)

    fun refs(source:WorkRecord,catalog:List<MaterialDefinition>):JSONArray {
        val id=WorkshopMediaBindings.contentId(source);val version=WorkshopMediaBindings.version(source)
        return JSONArray().also {result->
            byContent[id].orEmpty().forEach {row->runCatching {
                // Metadata only: opening a catalog must not read/hash/decode the whole bank.
                if(row.optString("contentVersion")!=version)return@runCatching
                val reference=WorkshopAssetPaths.reference(row)
                require(WorkshopAssetPaths.validModular(reference) || WorkshopAssetPaths.legacyName(reference))
                val data=JSONObject().put("contentId",id).put("contentVersion",version).put("mediaRoleRefs",JSONArray().put(row))
                val checked=WorkshopMediaBindings.fingerprintedRefs(data,listOf(source),catalog)
                result.put(checked.getJSONObject(0))
            }}
        }
    }

    companion object {
        // APK assets cannot change within this process. Cache facts, not approvals/target decisions.
        private val verified=ConcurrentHashMap<String,AssetFacts>()
        fun overlays(definitions:List<WorkRecord>):List<WorkRecord> = definitions.mapNotNull {record->
            if(record.archived || record.kind !in setOf("project_template","experiment_template","catalog_project","catalog_experiment"))return@mapNotNull null
            runCatching {
                val refs=JSONObject(record.json).optJSONArray("editorialMediaBindings") ?: return@mapNotNull null
                if(refs.length()==0)return@mapNotNull null
                val id=WorkshopMediaBindings.contentId(record);val version=WorkshopMediaBindings.version(record)
                WorkRecord("bundled-media:$id:$version","catalog_media",record.title,
                    JSONObject().put("contentId",id).put("contentVersion",version).put("editorialMediaBindings",JSONArray(refs.toString())).toString(),0)
            }.getOrNull()
        }
    }
}
