package pe.profelab.core.content

import android.content.Context
import org.json.JSONObject
import java.io.InputStream
import java.io.FileNotFoundException

/** APK-only paths. Offline pack UUIDs and private photographs never pass through this API. */
object WorkshopAssetPaths {
    private data class Index(val aliases:Map<String,String>,val paths:Set<String>)
    @Volatile private var cached:Index?=null
    private val filename=Regex("[a-zA-Z0-9_-]+\\.(?:png|jpg|jpeg|webp)")
    private val segment=Regex("[a-zA-Z0-9][a-zA-Z0-9_-]{0,119}")
    fun legacyName(name:String):Boolean=name.length<=160 && filename.matches(name)
    fun validModular(path:String):Boolean {
        if(path.length>300 || path.contains('\\') || path.contains(':') || path.contains('%'))return false
        val parts=path.split('/')
        if(parts.any {it.isBlank() || it in setOf(".","..")} || !filename.matches(parts.last()))return false
        if(parts.first()!="profelab")return false
        return when {
            parts.size==4 && parts[1]=="shared"->parts[2] in setOf("materials","icons","media")
            parts.size==4 && parts[1]=="taller"->parts[2]=="sectors"
            parts.size==6 && parts[1]=="taller"->parts[2] in setOf("experiments","projects","cooking","dance","heritage","ideas") && segment.matches(parts[3]) && parts[4] in setOf("cover","steps","outcomes","teacher")
            else->false
        }
    }
    private fun index(context:Context):Index=cached ?: synchronized(this){cached ?: runCatching {
        val text=context.assets.open("profelab/media_index_v1.json").bufferedReader().use {it.readText()};require(text.length<=2_000_000)
        val json=JSONObject(text);require(json.getInt("schemaVersion")==1)
        val source=json.getJSONObject("aliases");require(source.length()<=4000)
        val aliases=source.keys().asSequence().associateWith {name->
            require(legacyName(name));source.getString(name).also {require(validModular(it))}
        }
        val resources=json.getJSONArray("resources");require(resources.length()<=4000)
        val paths=(0 until resources.length()).map {resources.getJSONObject(it).getString("assetPath").also {path->require(validModular(path))}}.toSet()
        require(aliases.values.all {it in paths})
        Index(aliases,paths)
    }.getOrElse {Index(emptyMap(),emptySet())}.also {cached=it}}

    /** Translation preserves old saved assetFilename strings without retaining duplicate binaries. */
    fun canonical(context:Context,reference:String):String {
        if(legacyName(reference))return index(context).aliases[reference] ?: "workshop/images/$reference"
        require(validModular(reference) && reference in index(context).paths){"Ruta de recurso APK no autorizada"}
        return reference
    }
    fun open(context:Context,reference:String):InputStream {
        val path=canonical(context,reference)
        return try{context.assets.open(path)}catch(missing:FileNotFoundException){
            // Transitional APK compatibility only: never substitute another activity's photo.
            val old=index(context).aliases.entries.firstOrNull {it.value==path}?.key ?: throw missing
            context.assets.open("workshop/images/$old")
        }
    }
    fun reference(row:JSONObject):String=row.optString("assetPath").ifBlank {row.optString("assetFilename")}
}
