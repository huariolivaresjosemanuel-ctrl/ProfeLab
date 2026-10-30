package pe.profelab.core.database

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.LocalTransfer
import pe.profelab.core.model.newId
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupTransfer(private val context:Context,private val db:ProfeDatabase):LocalTransfer {
    private val tables=listOf("tasks","profiles","drafts","activity","workspace_records")
    private var previewHash:String?=null
    private fun hash(data:ByteArray)=MessageDigest.getInstance("SHA-256").digest(data).joinToString(""){"%02x".format(it)}
    override suspend fun exportBackup(destinationUri:String,includeFiles:Boolean)=withContext(Dispatchers.IO){
        val snapshot=JSONObject().put("schema",2)
        db.withTransaction {
            tables.forEach {table->val rows=JSONArray();db.openHelper.writableDatabase.query("SELECT * FROM $table").use {cursor->
                while(cursor.moveToNext()){val row=JSONObject();cursor.columnNames.forEachIndexed {i,name->
                    row.put(name,if(cursor.isNull(i))JSONObject.NULL else when(cursor.getType(i)){android.database.Cursor.FIELD_TYPE_INTEGER->cursor.getLong(i);android.database.Cursor.FIELD_TYPE_FLOAT->cursor.getDouble(i);else->cursor.getString(i)})
                };rows.put(row)}
            };snapshot.put(table,rows)}
        }
        val data=snapshot.toString().toByteArray(Charsets.UTF_8)
        require(data.size<=20*1024*1024){"Los datos superan el límite de restauración de 20 MiB"}
        val manifest=JSONObject().put("format","profelab-backup").put("schema",2).put("filesIncluded",includeFiles)
        val hashes=JSONObject().put("data.json",hash(data));val files=if(includeFiles)File(context.filesDir,"documents").listFiles()?.filter {it.isFile && it.name.matches(Regex("[a-zA-Z0-9-]+"))}.orEmpty() else emptyList()
        var total=data.size.toLong();files.forEach {f->require(f.length()<=100L*1024*1024);total+=f.length();require(total<=512L*1024*1024){"Respaldo demasiado grande para este flujo"};hashes.put("documents/${f.name}",f.inputStream().use {input->val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536);while(true){val n=input.read(buffer);if(n<0)break;digest.update(buffer,0,n)};digest.digest().joinToString(""){"%02x".format(it)}})}
        manifest.put("sha256",hashes)
        context.contentResolver.openOutputStream(Uri.parse(destinationUri),"wt")!!.use {output->ZipOutputStream(output).use {zip->
            fun entry(name:String,bytes:ByteArray){zip.putNextEntry(ZipEntry(name));zip.write(bytes);zip.closeEntry()}
            entry("manifest.json",manifest.toString().toByteArray());entry("data.json",data)
            files.forEach {f->zip.putNextEntry(ZipEntry("documents/${f.name}"));f.inputStream().use {it.copyTo(zip)};zip.closeEntry()}
        }}
    }
    private suspend fun stage(source:String):Pair<File,JSONObject> = withContext(Dispatchers.IO){
        val dir=File(context.cacheDir,"restore-${newId()}").apply {mkdirs()};var total=0L
        try {
            val seen=mutableSetOf<String>()
            context.contentResolver.openInputStream(Uri.parse(source))!!.use {input->ZipInputStream(input).use {zip->
                while(true){val entry=zip.nextEntry ?: break;val name=entry.name
                    require(seen.add(name) && !entry.isDirectory && (name in listOf("manifest.json","data.json") || name.matches(Regex("documents/[a-zA-Z0-9-]+")))){"Entrada de respaldo no válida"}
                    val target=File(dir,name);require(target.canonicalPath.startsWith(dir.canonicalPath+File.separator));target.parentFile?.mkdirs()
                    target.outputStream().use {out->val buffer=ByteArray(65536);var entryBytes=0L;while(true){val n=zip.read(buffer);if(n<0)break;total+=n;entryBytes+=n;require(total<=512L*1024*1024 && entryBytes<=if(name=="manifest.json")2L*1024*1024 else if(name=="data.json")20L*1024*1024 else 100L*1024*1024);out.write(buffer,0,n)}}
                    zip.closeEntry()
                }
            }}
            val manifest=JSONObject(File(dir,"manifest.json").readText());require(manifest.getString("format")=="profelab-backup" && manifest.getInt("schema")==2)
            val expected=manifest.getJSONObject("sha256");require(seen==expected.keys().asSequence().toSet()+"manifest.json")
            expected.keys().forEach {name->val f=File(dir,name);val digest=f.inputStream().use {stream->val md=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(65536);while(true){val n=stream.read(buffer);if(n<0)break;md.update(buffer,0,n)};md.digest().joinToString(""){"%02x".format(it)}};require(digest==expected.getString(name)){"Checksum incorrecto"}}
            val data=JSONObject(File(dir,"data.json").readText());require(data.getInt("schema")==2);tables.forEach {data.getJSONArray(it)}
            dir to data
        }catch(error:Exception){dir.deleteRecursively();throw error}
    }
    override suspend fun previewBackup(sourceUri:String):String {
        val (dir,data)=stage(sourceUri)
        try{previewHash=hash(File(dir,"manifest.json").readBytes())
            return tables.joinToString("\n"){"$it: ${data.getJSONArray(it).length()}"}+"\nArchivos: ${File(dir,"documents").listFiles()?.size ?: 0}\nLa fusión conserva las filas existentes con el mismo ID; no las reemplaza."
        }finally{dir.deleteRecursively()}
    }
    override suspend fun restoreBackup(sourceUri:String)=withContext(Dispatchers.IO){
        val (dir,data)=stage(sourceUri);val copied=mutableListOf<File>()
        try {
            require(previewHash!=null && hash(File(dir,"manifest.json").readBytes())==previewHash){"El contenido cambió; vuelve a previsualizar"}
            val destination=File(context.filesDir,"documents").apply {mkdirs()}
            File(dir,"documents").listFiles()?.forEach {f->val target=File(destination,f.name)
                if(target.exists())require(hash(target.readBytes())==hash(f.readBytes())){"Conflicto de archivo; no se sustituye"}
                else {f.copyTo(target,overwrite=false);copied+=target}
            }
            db.withTransaction {tables.forEach {table->
                val allowed=db.openHelper.writableDatabase.query("SELECT * FROM $table LIMIT 0").use {it.columnNames.toSet()}
                val rows=data.getJSONArray(table)
                for(i in 0 until rows.length()){val row=rows.getJSONObject(i);val columns=row.keys().asSequence().toList();require(columns.toSet()==allowed)
                    if(table=="tasks")require(row.getString("priority") in listOf("LOW","NORMAL","HIGH") && row.getString("title").length<=160)
                    if(table=="profiles" && !row.isNull("academicCycle"))require(row.getInt("academicCycle") in 1..20)
                    if(table=="workspace_records"){
                        val body=row.getString("json");require(body.length<=2_000_000);val json=JSONObject(body)
                        if(row.getString("kind")=="document")require(json.getString("fileId").matches(Regex("[a-zA-Z0-9-]+")))
                        if(row.getString("kind")=="annotation"){
                            require(json.getInt("page")>=0)
                            for(key in listOf("left","right","top","bottom")){val value=json.getDouble(key);require(value.isFinite() && value in 0.0..1.0)}
                            val points=json.optJSONArray("points");if(points!=null){require(points.length()<=20000 && points.length()%2==0);for(n in 0 until points.length()){val value=points.getDouble(n);require(value.isFinite() && value in 0.0..1.0)}}
                        }
                    }
                    val bindings=columns.map {c->row.get(c).let {if(it==JSONObject.NULL)null else it}}.toTypedArray()
                    val quoted=columns.joinToString(","){"`$it`"};val marks=columns.joinToString(","){"?"}
                    db.openHelper.writableDatabase.execSQL("INSERT OR IGNORE INTO $table ($quoted) VALUES ($marks)",bindings)
                }
            }};previewHash=null
        }catch(e:Exception){copied.forEach {it.delete()};throw e}finally{dir.deleteRecursively()}
    }
}
