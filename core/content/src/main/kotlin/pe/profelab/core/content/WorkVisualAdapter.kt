package pe.profelab.core.content

import android.graphics.BitmapFactory
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*

/** Explicit conversion, not an automatic promise that the Work editorial ZIP is installable. */
object WorkVisualAdapter {
    fun toContentPack(manifest:JSONObject,files:Map<String,ByteArray>,definitions:List<WorkRecord>,packId:String,packVersion:String,materialCatalog:List<MaterialDefinition> = emptyList()):Map<String,ByteArray> {
        require(manifest.getString("schema")=="WORK_VISUAL_DELIVERY_1" && manifest.getString("scope")=="TALLER_ONLY")
        require(manifest.optString("status")!="TEMPLATE_NOT_REAL_OUTPUT"){"Una plantilla no contiene fotografías reales"}
        require(validContentId(packId));contentVersion(packVersion)
        val a=manifest.getJSONArray("media");require(a.length() in 1..150)
        val entries=linkedMapOf<String,ByteArray>();val items=JSONArray();val groups=linkedMapOf<String,JSONArray>();val paths=mutableSetOf<String>();val mediaIds=mutableSetOf<String>()
        for(i in 0 until a.length()){
            val r=JSONObject(a.getJSONObject(i).toString());val id=r.getString("contentId");val version=r.getString("contentVersion")
            WorkshopMediaBindings.validate(JSONArray().put(r),id,version)
            val proof=WorkshopMediaBindings.fingerprintedRefs(JSONObject().put("contentId",id).put("contentVersion",version).put("mediaRoleRefs",JSONArray().put(r)),definitions,materialCatalog).getJSONObject(0)
            if(proof.has("stepFingerprint"))r.put("stepFingerprint",proof.getString("stepFingerprint"))
            require(mediaIds.add(r.getString("mediaId")))
            val path=r.getString("relativePath")
            require(path.length<=240 && !path.startsWith('/') && !path.contains('\\') && !path.contains(':') && path.split('/').none {it.isBlank() || it in setOf(".","..")})
            paths.add(path) // One approved physical file may be explicitly assigned to several roles.
            val ext=path.substringAfterLast('.').lowercase();require(ext in setOf("png","jpg","jpeg","webp"))
            val bytes=requireNotNull(files[path]){"Falta el archivo físico declarado"};require(bytes.isNotEmpty() && bytes.size<=8*1024*1024 && bytes.contentSha().equals(r.getString("sha256"),true))
            val expectedMime=when(ext){"png"->"image/png";"webp"->"image/webp";else->"image/jpeg"}
            val bounds=BitmapFactory.Options().apply {inJustDecodeBounds=true};BitmapFactory.decodeByteArray(bytes,0,bytes.size,bounds)
            require(r.getString("mime")==expectedMime && bounds.outMimeType==expectedMime && r.getInt("width")==bounds.outWidth && r.getInt("height")==bounds.outHeight)
            val target="images/visual-$i.$ext";entries[target]=bytes
            items.put(JSONObject().put("path",target).put("type","image").put("id",r.getString("mediaId")).put("sha256",bytes.contentSha()).put("source",r.getString("sourceType")).put("license",r.getString("licenseOrRights")).put("alt",r.getString("altText")).put("caption",r.optString("caption")).put("distributionApproved",true))
            // No fileId from an external ZIP may impersonate a file already in private storage.
            r.remove("fileId");r.remove("assetFilename")
            groups.getOrPut(id){JSONArray()}.put(r)
        }
        require(files.keys==paths){"El mapa de archivos de medios contiene entradas no declaradas"}
        require(groups.size+items.length()<=180)
        groups.entries.forEachIndexed {i,(id,rows)->
            val version=rows.getJSONObject(0).getString("contentVersion");WorkshopMediaBindings.validate(rows,id,version)
            val body=JSONObject().put("id",id).put("title",("Medios de "+id).take(160)).put("scope","TALLER_ONLY").put("contentId",id).put("contentVersion",version).put("mediaRoleRefs",rows)
                .put("contentOrigin","EDITORIAL_PROFELAB").put("distributionApproved",true)
            val bytes=body.toString().toByteArray(Charsets.UTF_8);val path="bindings/activity-$i.json";entries[path]=bytes
            items.put(JSONObject().put("path",path).put("type","workshop_media").put("sha256",bytes.contentSha()))
        }
        val result=JSONObject().put("schemaVersion",3).put("contentContractVersion","3.0").put("minimumAppSchema",2).put("minimumAppVersion","1.0.2").put("packId",packId).put("version",packVersion).put("items",items)
        entries["manifest.json"]=result.toString().toByteArray(Charsets.UTF_8)
        require(entries.values.sumOf {it.size.toLong()}<=40L*1024*1024)
        ContentPackValidator.inspect(entries) // Same MIME, size, rights, JSON and image validation as SAF import.
        return entries
    }
}
