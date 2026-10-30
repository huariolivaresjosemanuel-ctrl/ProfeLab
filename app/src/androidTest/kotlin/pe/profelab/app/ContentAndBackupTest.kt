package pe.profelab.app

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import pe.profelab.core.database.*
import pe.profelab.core.content.ContentPacks
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class ContentAndBackupTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
    private fun zip(entries:Map<String,ByteArray>):File {
        val file=File(context.cacheDir,"qa-${newId()}.zip")
        ZipOutputStream(file.outputStream()).use {out->entries.forEach {(name,bytes)->out.putNextEntry(ZipEntry(name));out.write(bytes);out.closeEntry()}}
        return file
    }
    @Test fun zipTraversalIsRejectedWithoutMutation():Unit=runBlocking {
        val db=database()
        try {
            val store=WorkspaceRepository(db);val pack=ContentPacks(context,store);val file=File(context.cacheDir,"qa-traversal-${newId()}.zip")
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("qa/path-traversal.zip").use {input->file.outputStream().use {input.copyTo(it)}}
            var rejected=false;try {pack.inspect(Uri.fromFile(file).toString())}catch(_:IllegalArgumentException){rejected=true}catch(_:java.util.zip.ZipException){rejected=true}
            assertTrue(rejected);assertTrue(store.observeRecords().first().isEmpty());assertFalse(File(context.cacheDir,"outside.json").exists());file.delete()
        }finally{db.close()}
    }
    @Test fun packReimportIsIdempotentAndUserCopySurvives():Unit=runBlocking {
        val db=database()
        try {
            val store=WorkspaceRepository(db);val importer=ContentPacks(context,store)
            val bytes=JSONObject().put("id","test-project").put("title","QA original").put("contentOrigin","USER_AUTHORED").put("distributionApproved",true).toString().toByteArray()
            val digest=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
            val manifest=JSONObject().put("schemaVersion",2).put("minimumAppSchema",2).put("packId","qa").put("version","1.0.0").put("items",JSONArray().put(JSONObject().put("path","project.json").put("sha256",digest).put("type","project")))
            val f=zip(mapOf("manifest.json" to manifest.toString().toByteArray(),"project.json" to bytes));val uri=Uri.fromFile(f).toString()
            val personal=WorkRecord(newId(),"project","Mi copia intacta","{}",1);store.saveRecord(personal)
            importer.install(importer.inspect(uri));importer.install(importer.inspect(uri))
            assertEquals(3,store.observeRecords().first().size);assertEquals(personal.title,store.record(personal.id)?.title);f.delete()
        }finally{db.close()}
    }
    @Test fun backupMergePreservesH1AndExistingPersonalEdits()=runBlocking {
        val source=database();val destination=database();val file=File(context.cacheDir,"backup-${newId()}.zip")
        try {
            val old=LocalRepository(source);val id=newId();old.saveTask(Task(id,"Tarea H1",createdAt=1,updatedAt=1));old.saveProfile(Profile(newId(),"Profe",6))
            WorkspaceRepository(source).saveRecord(WorkRecord("same-id","note","Versión del respaldo","{}",1))
            BackupTransfer(context,source).exportBackup(Uri.fromFile(file).toString(),false)
            WorkspaceRepository(destination).saveRecord(WorkRecord("same-id","note","Edición existente", "{}",2))
            val transfer=BackupTransfer(context,destination);val uri=Uri.fromFile(file).toString();assertTrue(transfer.previewBackup(uri).contains("tasks: 1"));transfer.restoreBackup(uri)
            assertEquals("Tarea H1",LocalRepository(destination).findTask(id)?.title)
            assertEquals("Edición existente",WorkspaceRepository(destination).record("same-id")?.title)
            assertEquals(6,LocalRepository(destination).observeProfile().first()?.academicCycle)
        }finally{source.close();destination.close();file.delete()}
    }
}
