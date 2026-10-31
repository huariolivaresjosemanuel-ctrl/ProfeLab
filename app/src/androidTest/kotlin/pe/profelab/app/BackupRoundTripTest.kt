package pe.profelab.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class BackupRoundTripTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
    private fun digest(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private class PreferenceFixture(context:Context){
        val file=File(context.cacheDir,"qa-${newId()}.preferences_pb")
        private val job=SupervisorJob()
        val store=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file})
        val local=LocalPreferences(store)
        suspend fun close(){job.cancelAndJoin();file.delete()}
    }
    @Test fun fullBackupPreservesMediaReferencesAndMergesMissingPreferences():Unit=runBlocking {
        val source=db();val destination=db();val a=PreferenceFixture(context);val b=PreferenceFixture(context)
        val archive=File(context.cacheDir,"qa-full-${newId()}.zip")
        val files=List(3){File(context.filesDir,"documents/${newId()}")};files.forEach {it.parentFile?.mkdirs()}
        try {
            val image=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888)
            try{files[0].outputStream().use {assertTrue(image.compress(Bitmap.CompressFormat.PNG,100,it))}}finally{image.recycle()}
            androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().context.assets.open("qa/EJEMPLO_PROFELAB_TEXTO_SELECCIONABLE_2P.pdf").use {input->files[1].outputStream().use {input.copyTo(it)}}
            val pcm=ByteArray(160)
            files[2].writeBytes(ByteBuffer.allocate(44+pcm.size).order(ByteOrder.LITTLE_ENDIAN).apply {
                put("RIFF".toByteArray());putInt(36+pcm.size);put("WAVEfmt ".toByteArray());putInt(16);putShort(1);putShort(1);putInt(8000);putInt(16000);putShort(2);putShort(16);put("data".toByteArray());putInt(pcm.size);put(pcm)
            }.array()) // Valid silent PCM WAV; byte integrity here, playback is a separate scenario.
            val hashes=files.associate {it.name to digest(it.readBytes())}
            a.local.setPersonalization("María QA",LocalRole.INTERN);a.local.setShowCompleted(true);a.local.setMascotPreferences(false,true,true)
            b.local.setShowCompleted(false) // Existing destination choice must win over backup.
            LocalRepository(source).saveProfile(Profile("qa-profile","María QA",4))
            val attachments=JSONArray().put(JSONObject().put("id",files[0].name).put("mime","image/png")).put(JSONObject().put("id",files[2].name).put("mime","audio/wav"))
            val payload=JSONObject().put("teacherGuide",JSONObject().put("version",1).put("introShort","Mi guion propio"))
                .put("attachments",attachments).put("steps",JSONArray().put(JSONObject().put("id","step-1").put("title","Observar").put("done",true).put("imageFileIds",JSONArray().put(files[0].name))))
                .put("records",JSONArray().put(JSONObject().put("date","2026-10-30").put("observation","Dato propio").put("photoAttachmentIds",JSONArray().put(files[0].name))))
            val workspace=WorkspaceRepository(source)
            workspace.saveRecord(WorkRecord("qa-project","project","Conservar guion y avance",payload.toString(),1))
            workspace.saveRecord(WorkRecord("qa-inventory","inventory","Papel",JSONObject().put("quantity","3").put("unit","hoja").toString(),1))
            workspace.saveRecord(WorkRecord("qa-shopping","shopping_v2","Compras",JSONObject().put("receipts",JSONArray().put(JSONObject().put("id","receipt-1"))).toString(),1))
            workspace.saveRecord(WorkRecord("qa-document","document","PDF propio",JSONObject().put("fileId",files[1].name).put("lastPage",1).toString(),1))
            workspace.saveRecord(WorkRecord("qa-annotation","annotation","Marca",JSONObject().put("documentId","qa-document").put("page",0).put("left",.1).put("right",.2).put("top",.1).put("bottom",.2).toString(),1))
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(archive).toString(),true)
            files.forEach {assertTrue(it.delete())}
            val transfer=BackupTransfer(context,destination,b.local);val uri=Uri.fromFile(archive).toString()
            assertTrue(transfer.previewBackup(uri).contains("Referencias sin archivo en respaldo ni dispositivo: 0"));transfer.restoreBackup(uri)
            files.forEach {assertEquals(hashes[it.name],digest(it.readBytes()))}
            val restored=requireNotNull(WorkspaceRepository(destination).record("qa-project"))
            assertEquals(payload.toString(),restored.json)
            assertEquals("3",JSONObject(requireNotNull(WorkspaceRepository(destination).record("qa-inventory")).json).getString("quantity"))
            assertNotNull(WorkspaceRepository(destination).record("qa-annotation"))
            val settings=b.local.preferences.first();assertEquals("María QA",settings.displayName);assertEquals(LocalRole.INTERN,settings.role);assertFalse(settings.showCompleted);assertTrue(settings.reduceMotion);assertFalse(settings.mascotVisible)
            assertEquals(4,LocalRepository(destination).observeProfile().first()?.academicCycle)
        }finally{source.close();destination.close();a.close();b.close();archive.delete();files.forEach {it.delete()}}
    }
    @Test fun interruptedPreferenceRestoreKeepsCommittedMediaAndCanResume():Unit=runBlocking {
        val source=db();val destination=db();val a=PreferenceFixture(context);val b=PreferenceFixture(context)
        val archive=File(context.cacheDir,"qa-journal-${newId()}.zip");val media=File(context.filesDir,"documents/${newId()}")
        try {
            media.parentFile?.mkdirs();media.writeBytes("private evidence fixture".toByteArray())
            val body=JSONObject().put("attachments",JSONArray().put(JSONObject().put("id",media.name).put("mime","image/png")))
            WorkspaceRepository(source).saveRecord(WorkRecord("qa-recovery","project","Recoverable",body.toString(),1));a.local.setPersonalization("Ana",LocalRole.TEACHER)
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(archive).toString(),true);media.delete()
            var fail=true
            val unstable=object:DataStore<Preferences> by b.store {
                override suspend fun updateData(transform:suspend (Preferences)->Preferences):Preferences {
                    if(fail){fail=false;throw IOException("Simulated DataStore interruption")}
                    return b.store.updateData(transform)
                }
            }
            val transfer=BackupTransfer(context,destination,LocalPreferences(unstable));val uri=Uri.fromFile(archive).toString();transfer.previewBackup(uri)
            var rejected=false;try{transfer.restoreBackup(uri)}catch(_:IOException){rejected=true}
            assertTrue(rejected);assertTrue(media.isFile);assertNotNull(WorkspaceRepository(destination).record("qa-recovery"));assertNotNull(destination.dao().draft("restore:preferences:v1"))
            transfer.recoverPendingPreferences();transfer.recoverPendingPreferences()
            assertNull(destination.dao().draft("restore:preferences:v1"));assertEquals("Ana",b.local.preferences.first().displayName)
        }finally{source.close();destination.close();a.close();b.close();archive.delete();media.delete()}
    }
    @Test fun legacySchemaTwoWithoutPreferencesStillRestoresAndAdoptsOldProfile():Unit=runBlocking {
        val source=db();val destination=db();val a=PreferenceFixture(context);val b=PreferenceFixture(context)
        val exported=File(context.cacheDir,"qa-current-${newId()}.zip");val legacy=File(context.cacheDir,"qa-legacy-${newId()}.zip")
        try {
            LocalRepository(source).saveProfile(Profile("legacy-profile","Perfil anterior",6))
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(exported).toString(),false)
            val entries=linkedMapOf<String,ByteArray>()
            ZipInputStream(exported.inputStream()).use {z->while(true){val e=z.nextEntry ?: break;entries[e.name]=z.readBytes()}}
            val data=JSONObject(String(requireNotNull(entries["data.json"]),Charsets.UTF_8));data.remove("preferences");data.put("schema",2);listOf("inventory_operations","inventory_movements","inventory_reservations").forEach {data.remove(it)};val bytes=data.toString().toByteArray();entries["data.json"]=bytes
            val manifest=JSONObject(String(requireNotNull(entries["manifest.json"]),Charsets.UTF_8));manifest.put("schema",2);manifest.remove("preferencesVersion");manifest.getJSONObject("sha256").put("data.json",digest(bytes));entries["manifest.json"]=manifest.toString().toByteArray()
            ZipOutputStream(legacy.outputStream()).use {z->entries.forEach {(name,value)->z.putNextEntry(ZipEntry(name));z.write(value);z.closeEntry()}}
            val transfer=BackupTransfer(context,destination,b.local);val uri=Uri.fromFile(legacy).toString();assertTrue(transfer.previewBackup(uri).contains("respaldo anterior"));transfer.restoreBackup(uri)
            val profile=LocalRepository(destination).observeProfile().first();b.local.migrateLegacyProfile(profile)
            assertEquals("Perfil anterior",b.local.preferences.first().displayName);assertTrue(b.local.preferences.first().onboardingCompleted)
        }finally{source.close();destination.close();a.close();b.close();exported.delete();legacy.delete()}
    }

    @Test fun retainedOcrDraftSurvivesCacheRemovalAndFullBackup():Unit=runBlocking {
        val source=db();val destination=db();val a=PreferenceFixture(context);val b=PreferenceFixture(context)
        val input=File(context.cacheDir,"qa-ocr-${newId()}.png");val archive=File(context.cacheDir,"qa-ocr-${newId()}.zip")
        var retained:File?=null
        try {
            context.assets.open("workshop/images/mat-003.png").use {stream->input.outputStream().use {stream.copyTo(it)}}
            val originalHash=digest(input.readBytes());val repository=LocalRepository(source);val adapter=RetainedOcrDrafts(context,repository)
            val text=JSONObject().put("uri",Uri.fromFile(input).toString()).put("text","Texto revisado propio").put("raw","Texto bruto")
            adapter.writeDraft(Draft("ocr-review",text.toString(),1))
            val first=JSONObject(requireNotNull(adapter.readDraft("ocr-review")).payload);val id=first.getString("imageFileId")
            val retainedFile=File(context.filesDir,"documents/$id");retained=retainedFile;assertEquals(originalHash,digest(retainedFile.readBytes()))
            adapter.writeDraft(Draft("ocr-review",text.put("text","Corrección final").toString(),2))
            assertEquals(id,JSONObject(requireNotNull(adapter.readDraft("ocr-review")).payload).getString("imageFileId"));assertTrue(input.delete())
            val portable=JSONObject(requireNotNull(adapter.readDraft("ocr-review")).payload).put("uri","file:///old-sandbox/documents/$id")
            repository.writeDraft(Draft("ocr-review",portable.toString(),3))
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(archive).toString(),true);assertTrue(retainedFile.delete())
            val transfer=BackupTransfer(context,destination,b.local);val uri=Uri.fromFile(archive).toString()
            assertTrue(transfer.previewBackup(uri).contains("Referencias no incluidas en el ZIP: 0"));transfer.restoreBackup(uri)
            val restored=JSONObject(requireNotNull(RetainedOcrDrafts(context,LocalRepository(destination)).readDraft("ocr-review")).payload)
            assertEquals("Corrección final",restored.getString("text"));assertEquals(Uri.fromFile(retainedFile).toString(),restored.getString("uri"));assertEquals(originalHash,digest(retainedFile.readBytes()))
        }finally{source.close();destination.close();a.close();b.close();input.delete();archive.delete();retained?.delete()}
    }
    @Test fun unavailableOcrImageKeepsTextAndWarnsBeforeTransition():Unit=runBlocking {
        val database=db();val prefs=PreferenceFixture(context);val archive=File(context.cacheDir,"qa-missing-${newId()}.zip")
        try {
            val adapter=RetainedOcrDrafts(context,LocalRepository(database))
            adapter.writeDraft(Draft("ocr-review",JSONObject().put("uri","file:///unavailable-${newId()}.png").put("text","Conservar este texto").toString(),1))
            assertEquals("Conservar este texto",JSONObject(requireNotNull(adapter.readDraft("ocr-review")).payload).getString("text"));assertNotNull(adapter.warning.value)
            val transfer=BackupTransfer(context,database,prefs.local);val uri=Uri.fromFile(archive).toString();transfer.exportBackup(uri,true)
            assertTrue(transfer.previewBackup(uri).contains("Borrador OCR sin imagen privada"))
        }finally{database.close();prefs.close();archive.delete()}
    }

    @Test fun pendingPdfOcrBatchRestoresItsPagesAndRebasesPaths():Unit=runBlocking {
        val source=db();val destination=db();val a=PreferenceFixture(context);val b=PreferenceFixture(context)
        val archive=File(context.cacheDir,"qa-batch-${newId()}.zip");val page=File(context.filesDir,"ocr-drafts/${newId()}.png")
        try {
            page.parentFile?.mkdirs();context.assets.open("workshop/images/mat-003.png").use {input->page.outputStream().use {input.copyTo(it)}}
            val expected=digest(page.readBytes());val payload=JSONArray().put(JSONObject().put("page",0).put("file","/old-sandbox/ocr-drafts/${page.name}").put("raw","Texto bruto").put("corrected","Texto corregido").put("reviewed",true))
            LocalRepository(source).writeDraft(Draft("pdf-ocr:qa-document",payload.toString(),1))
            val export=BackupTransfer(context,source,a.local);export.exportBackup(Uri.fromFile(archive).toString(),false)
            assertTrue(export.previewBackup(Uri.fromFile(archive).toString()).contains("Referencias no incluidas en el ZIP: 1"))
            export.exportBackup(Uri.fromFile(archive).toString(),true);assertTrue(page.delete())
            val restore=BackupTransfer(context,destination,b.local);val uri=Uri.fromFile(archive).toString();assertTrue(restore.previewBackup(uri).contains("Referencias no incluidas en el ZIP: 0"));restore.restoreBackup(uri)
            val result=JSONArray(requireNotNull(LocalRepository(destination).readDraft("pdf-ocr:qa-document")).payload).getJSONObject(0)
            assertEquals(page.absolutePath,result.getString("file"));assertEquals("Texto corregido",result.getString("corrected"));assertTrue(result.getBoolean("reviewed"));assertEquals(expected,digest(page.readBytes()))
        }finally{source.close();destination.close();a.close();b.close();archive.delete();page.delete()}
    }
}
