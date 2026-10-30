package pe.profelab.app

import android.content.Context
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
import pe.profelab.core.content.*
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class TeacherGuideContractTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun fixture(version:String,intro:Any,category:String="creative"):File {
        val body=JSONObject().put("id","guide").put("title","Guía QA propia").put("categoryId",category)
            .put("contentOrigin","USER_AUTHORED").put("distributionApproved",true)
            .put("teacherGuide",JSONObject().put("version",1).put("introShort",intro)).put("attachments",JSONArray().put(JSONObject().put("id","private-not-allowed").put("mime","image/png")))
            .put("steps",JSONArray().put(JSONObject().put("id","observe").put("title","Observar").put("done",false)))
        val bytes=body.toString().toByteArray();val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        val manifest=JSONObject().put("schemaVersion",3).put("contentContractVersion","3.0").put("minimumAppSchema",2).put("minimumAppVersion","1.0.2")
            .put("packId","qa-teaching").put("version",version).put("items",JSONArray().put(JSONObject().put("type","guided_workshop").put("path","guide.json").put("sha256",hash)))
        return File(context.cacheDir,"qa-guide-${newId()}.zip").also {file->ZipOutputStream(file.outputStream()).use {z->
            listOf("manifest.json" to manifest.toString().toByteArray(),"guide.json" to bytes).forEach {(name,value)->z.putNextEntry(ZipEntry(name));z.write(value);z.closeEntry()}
        }}
    }
    @Test fun everyOriginalHasReadableTeachingFallbackAndGeoplanoUsesItsActualId():Unit=runBlocking {
        val originals=WorkshopSeed(context).templates();assertEquals(20,originals.size)
        originals.forEach {r->val guide=TeacherGuides.resolve(r);TeacherGuides.validate(guide);assertTrue(guide.getString("introShort").isNotBlank());assertTrue(TeacherGuides.read(r).blocks.any {it.key=="guidedDemonstration" && it.texts.isNotEmpty()})}
        val geoplano=originals.single {it.id=="mat-003"};assertTrue(geoplano.title.contains("Geoplano"));assertTrue(TeacherGuides.resolve(geoplano).getString("introShort").contains("triángulo"))
        assertFalse(TeacherGuides.resolve(originals.single {it.id=="mat-001"}).getString("introShort").contains("triángulo"))
        val own=geoplano.copy(id=newId(),kind="project",json=JSONObject(geoplano.json).put("teacherGuide",JSONObject().put("version",1).put("introShort","")).toString())
        assertEquals("",TeacherGuides.resolve(own).getString("introShort"))
    }
    @Test fun packUpgradePreservesPersonalTeachingAndStepSnapshot():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val importer=ContentPacks(context,store)
        val first=fixture("1.0.0","Propuesta inicial");val update=fixture("1.0.1","Nueva propuesta editorial")
        try {
            importer.install(importer.inspect(Uri.fromFile(first).toString()))
            val inspected=importer.inspect(Uri.fromFile(first).toString());assertFalse(JSONObject(inspected.records.single().json).has("attachments"))
            val original=requireNotNull(store.record("pack:qa-teaching:guide"))
            val legacy=JSONObject(original.json);legacy.remove("sourcePackId");val template=original.copy(json=legacy.toString());store.saveRecord(template)
            val data=JSONObject(template.json)
            data.getJSONObject("teacherGuide").put("introShort","Mi guion editado");data.getJSONArray("steps").getJSONObject(0).put("done",true)
            val personal=template.copy(id=newId(),kind="project",json=data.toString());store.saveRecord(personal)
            importer.install(importer.inspect(Uri.fromFile(update).toString()))
            assertEquals("Mi guion editado",JSONObject(requireNotNull(store.record(personal.id)).json).getJSONObject("teacherGuide").getString("introShort"))
            assertTrue(JSONObject(requireNotNull(store.record(personal.id)).json).getJSONArray("steps").getJSONObject(0).getBoolean("done"))
            assertEquals("Nueva propuesta editorial",JSONObject(requireNotNull(store.record(template.id)).json).getJSONObject("teacherGuide").getString("introShort"))
        }finally{db.close();first.delete();update.delete()}
    }
    @Test fun malformedTeachingAndUnreviewedFoodCategoryAreRejectedBeforeMutation():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val importer=ContentPacks(context,store)
        val files=listOf(fixture("1.0.0",JSONArray().put("Wrong shape")),fixture("1.0.0","No publicar receta","mini_chef"))
        try {files.forEach {file->var rejected=false;try{importer.inspect(Uri.fromFile(file).toString())}catch(_:IllegalArgumentException){rejected=true};assertTrue(rejected)};assertTrue(store.observeRecords().first().isEmpty())}
        finally{db.close();files.forEach {it.delete()}}
    }
    @Test fun catalogCollisionDoesNotOverwriteAPersonalRecord():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val file=fixture("1.0.0","Guion válido")
        try {
            val personal=WorkRecord("pack:qa-teaching:guide","project","No reemplazar","{}",1);store.saveRecord(personal)
            val importer=ContentPacks(context,store);var rejected=false
            try{importer.install(importer.inspect(Uri.fromFile(file).toString()))}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertEquals(personal.title,store.record(personal.id)?.title);assertNull(store.record("pack:qa-teaching"))
        }finally{db.close();file.delete()}
    }
}
