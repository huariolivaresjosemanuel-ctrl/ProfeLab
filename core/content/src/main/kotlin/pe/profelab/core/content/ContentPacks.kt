package pe.profelab.core.content

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import pe.profelab.core.model.*
import java.security.MessageDigest
import java.util.zip.ZipInputStream

data class PackData(val manifest:JSONObject,val records:List<WorkRecord>,val digest:String)
class ContentPacks(private val context:Context,private val store:WorkspaceStore) {
    suspend fun inspect(uri:String):PackData=withContext(Dispatchers.IO){
        val entries=linkedMapOf<String,ByteArray>();var total=0
        context.contentResolver.openInputStream(Uri.parse(uri))!!.use {input->ZipInputStream(input).use {zip->
            while(true){val entry=zip.nextEntry ?: break;val name=entry.name
                require(!entry.isDirectory && !name.startsWith('/') && !name.contains('\\') && !name.contains(':') && name.split('/').none {it==".."||it=="."||it.isBlank()} && name !in entries && entries.size<200)
                val bytes=zip.readBytesLimited(2*1024*1024);total+=bytes.size;require(total<=20*1024*1024);entries[name]=bytes;zip.closeEntry()
            }
        }}
        val manifest=JSONObject(String(requireNotNull(entries["manifest.json"]),Charsets.UTF_8))
        require(manifest.getInt("schemaVersion")==2 && manifest.getInt("minimumAppSchema")<=2 && manifest.getString("version").matches(Regex("\\d+\\.\\d+\\.\\d+")))
        val items=manifest.getJSONArray("items");require(items.length()<=100)
        val records=(0 until items.length()).map {i->val item=items.getJSONObject(i);val path=item.getString("path");val bytes=requireNotNull(entries[path]);require(bytes.sha()==item.getString("sha256"));val data=JSONObject(String(bytes,Charsets.UTF_8))
            require(data.getString("contentOrigin")=="EDITORIAL_PROFELAB" || data.getString("contentOrigin")=="USER_AUTHORED")
            require(data.getBoolean("distributionApproved"));val type=item.getString("type");require(type in listOf("project","experiment"))
            val id="pack:${manifest.getString("packId")}:${data.getString("id")}"
            WorkRecord(id,"catalog_$type",data.getString("title"),data.put("packVersion",manifest.getString("version")).put("reviewStatus","PENDING").put("safetyReviewedByUser",false).toString(),System.currentTimeMillis())
        }
        require(records.map {it.id}.distinct().size==records.size && entries.keys==((0 until items.length()).map {items.getJSONObject(it).getString("path")}.toSet()+"manifest.json"))
        PackData(manifest,records,entries["manifest.json"]!!.sha())
    }
    suspend fun install(pack:PackData){
        val markerId="pack:${pack.manifest.getString("packId")}";val previous=store.record(markerId)
        if(previous!=null && JSONObject(previous.json).optString("digest")==pack.digest)return
        if(previous!=null){
            val old=JSONObject(previous.json).getString("version").split('.').map {it.toInt()}
            val next=pack.manifest.getString("version").split('.').map {it.toInt()}
            val comparison=(0..2).firstOrNull {old[it]!=next[it]}?.let {next[it].compareTo(old[it])} ?: 0
            require(comparison>0){"La misma versión difiere o se intentó retroceder el paquete"}
        }
        store.saveBatch(pack.records+WorkRecord(markerId,"content_pack",pack.manifest.getString("packId"),JSONObject().put("version",pack.manifest.getString("version")).put("digest",pack.digest).toString(),System.currentTimeMillis()))
    }
}
private fun ByteArray.sha()=MessageDigest.getInstance("SHA-256").digest(this).joinToString(""){"%02x".format(it)}
private fun java.io.InputStream.readBytesLimited(max:Int):ByteArray {val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192);while(true){val n=read(buffer);if(n<0)break;require(out.size()+n<=max);out.write(buffer,0,n)};return out.toByteArray()}
