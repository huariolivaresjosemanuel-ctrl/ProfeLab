package pe.profelab.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.ContentPacks
import pe.profelab.core.database.ProfeDatabase
import pe.profelab.core.database.WorkspaceRepository
import pe.profelab.core.model.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class WorkshopContentPackTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun fixture(includeUnlisted:Boolean=false):File {
        val material=JSONObject().put("id","qa-card").put("name","Cartón de prueba único").put("unit","unidad").put("materialKind","CONSUMABLE")
        val layout=JSONObject().put("id","qa-guide").put("title","Guía QA").put("template","CONSTRUCTION").put("sectionOrder",JSONArray(listOf("materials","steps","safety","sources")))
        val project=JSONObject().put("id","qa-project").put("title","Proyecto con galería QA").put("layoutId","qa-guide").put("images",JSONArray().put("photo")).put("materials",JSONArray().put(JSONObject().put("id","req").put("name","Cartón de prueba único").put("materialId","qa-card").put("amount","2").put("unit","unidad"))).put("steps",JSONArray().put(JSONObject().put("id","step").put("title","Observar").put("images",JSONArray().put("photo"))))
        listOf(material,layout,project).forEach {it.put("contentOrigin","USER_AUTHORED").put("distributionApproved",true)}
        val bitmap=Bitmap.createBitmap(24,24,Bitmap.Config.ARGB_8888);val image=ByteArrayOutputStream()
        try{bitmap.eraseColor(Color.BLUE);assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,image))}finally{bitmap.recycle()}
        val entries=linkedMapOf("material.json" to material.toString().toByteArray(),"layout.json" to layout.toString().toByteArray(),"project.json" to project.toString().toByteArray(),"images/photo.png" to image.toByteArray())
        val items=JSONArray();entries.forEach {(path,bytes)->val type=when(path){"material.json"->"material";"layout.json"->"layout";"project.json"->"project";else->"image"};val item=JSONObject().put("path",path).put("type",type).put("sha256",MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)});if(type=="image")item.put("id","photo").put("source","Fixture original QA").put("license","USER_AUTHORED").put("distributionApproved",true);items.put(item)}
        val manifest=JSONObject().put("schemaVersion",2).put("contentContractVersion","2.1").put("minimumAppSchema",2).put("packId","qa-scalable").put("version","1.0.0").put("items",items)
        entries["manifest.json"]=manifest.toString().toByteArray();if(includeUnlisted)entries["unlisted.json"]="{}".toByteArray()
        return File(context.cacheDir,"qa-pack-${newId()}.zip").also {file->ZipOutputStream(file.outputStream()).use {z->entries.forEach {(path,bytes)->z.putNextEntry(ZipEntry(path));z.write(bytes);z.closeEntry()}}}
    }
    @Test fun imageLayoutAndMaterialsInstallOnceAndKeepPersonalSnapshot()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val importer=ContentPacks(context,store);val file=fixture()
        var image:File?=null
        try{
            val personal=WorkRecord(newId(),"project","No sobrescribir","{}",1);store.saveRecord(personal)
            val pack=importer.inspect(Uri.fromFile(file).toString());importer.install(pack)
            val first=store.observeRecords().first();val project=first.single {it.kind=="catalog_project"};val j=JSONObject(project.json);val fileId=j.getJSONArray("imageFileIds").getString(0)
            image=File(context.filesDir,"documents/$fileId");assertTrue(requireNotNull(image).isFile)
            assertEquals(fileId,j.getJSONArray("steps").getJSONObject(0).getJSONArray("imageFileIds").getString(0))
            assertTrue(j.getString("layoutId").startsWith("pack:qa-scalable:layout:"));assertFalse(j.getBoolean("safetyReviewedByUser"))
            importer.install(pack);val after=store.observeRecords().first();assertEquals(first.size,after.size);assertEquals(personal.title,store.record(personal.id)?.title)
            assertEquals(fileId,JSONObject(requireNotNull(store.record(project.id)).json).getJSONArray("imageFileIds").getString(0))
        }finally{image?.delete();file.delete();db.close()}
    }
    @Test fun unlistedFileIsRejectedBeforeMutation()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val f=fixture(true)
        try{var rejected=false;try{ContentPacks(context,store).inspect(Uri.fromFile(f).toString())}catch(_:IllegalArgumentException){rejected=true};assertTrue(rejected);assertTrue(store.observeRecords().first().isEmpty())}finally{f.delete();db.close()}
    }
}
