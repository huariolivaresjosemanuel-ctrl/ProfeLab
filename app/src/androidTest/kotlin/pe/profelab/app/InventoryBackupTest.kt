package pe.profelab.app

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import java.io.File
import java.security.MessageDigest
import java.util.zip.*

@RunWith(AndroidJUnit4::class)
class InventoryBackupTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
    private class Prefs(context:Context){
        val file=File(context.cacheDir,"qa-inv-${newId()}.preferences_pb");val job=SupervisorJob()
        val local=LocalPreferences(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file}))
        suspend fun close(){job.cancelAndJoin();file.delete()}
    }
    private suspend fun seed(db:ProfeDatabase):WorkspaceRepository {
        val store=WorkspaceRepository(db);store.saveRecord(WorkRecord("run","project","Mi actividad","{}",1))
        store.saveRecord(WorkRecord("stock","inventory","Ligas",JSONObject().put("quantity","14").put("reserved","0").put("unit","unidad").put("materialKind","CONSUMABLE").put("materialId","ligas").toString(),1))
        store.prepareConsumption(ConsumptionRequest("confirmed-usage","run","Usé ocho",listOf(ActualUsage("stock","8"))))
        return store
    }
    private fun archive()=File(context.cacheDir,"qa-inv-backup-${newId()}.zip")
    private fun hash(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
    private fun rewrite(source:File,destination:File,transform:(JSONObject)->Unit){
        val entries=linkedMapOf<String,ByteArray>()
        ZipInputStream(source.inputStream()).use {z->while(true){val e=z.nextEntry ?: break;entries[e.name]=z.readBytes()}}
        val data=JSONObject(String(requireNotNull(entries["data.json"]),Charsets.UTF_8));transform(data);val bytes=data.toString().toByteArray();entries["data.json"]=bytes
        val manifest=JSONObject(String(requireNotNull(entries["manifest.json"]),Charsets.UTF_8));manifest.put("schema",data.getInt("schema"));manifest.getJSONObject("sha256").put("data.json",hash(bytes));entries["manifest.json"]=manifest.toString().toByteArray()
        ZipOutputStream(destination.outputStream()).use {z->entries.forEach {(n,b)->z.putNextEntry(ZipEntry(n));z.write(b);z.closeEntry()}}
    }
    @Test fun ledgerAndStockRestoreTogetherAndRepeatedRestoreCannotConsumeAgain():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive()
        try {
            val source=seed(a);source.confirmInventoryOperation("confirmed-usage")
            BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            val transfer=BackupTransfer(context,b,q.local);val uri=Uri.fromFile(file).toString()
            transfer.previewBackup(uri);transfer.restoreBackup(uri);transfer.previewBackup(uri);transfer.restoreBackup(uri)
            val target=WorkspaceRepository(b);target.confirmInventoryOperation("confirmed-usage")
            assertEquals("6",JSONObject(requireNotNull(target.record("stock")).json).getString("quantity"));assertEquals("COMMITTED",b.inventory().operation("confirmed-usage")?.status);assertEquals(1,b.inventory().movements("confirmed-usage").size)
        }finally{a.close();b.close();p.close();q.close();file.delete()}
    }
    @Test fun preparedUsageCanResumeAfterBackupWithoutAnEarlyDeduction():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive()
        try {
            seed(a);BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            val transfer=BackupTransfer(context,b,q.local);val uri=Uri.fromFile(file).toString();transfer.previewBackup(uri);transfer.restoreBackup(uri)
            val target=WorkspaceRepository(b);assertEquals("14",JSONObject(requireNotNull(target.record("stock")).json).getString("quantity"))
            target.confirmInventoryOperation("confirmed-usage");target.confirmInventoryOperation("confirmed-usage");assertEquals("6",JSONObject(requireNotNull(target.record("stock")).json).getString("quantity"))
        }finally{a.close();b.close();p.close();q.close();file.delete()}
    }
    @Test fun localStockConflictRejectsTheWholeRestoreBeforeUnrelatedRowsChange():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive()
        try {
            seed(a).confirmInventoryOperation("confirmed-usage");WorkspaceRepository(a).saveRecord(WorkRecord("new-note","note","No importar parcialmente","{}",1))
            WorkspaceRepository(b).saveRecord(WorkRecord("stock","inventory","Mi saldo local",JSONObject().put("quantity","99").put("reserved","0").put("unit","unidad").put("materialKind","CONSUMABLE").toString(),1))
            BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            var rejected=false;try{BackupTransfer(context,b,q.local).previewBackup(Uri.fromFile(file).toString())}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertNull(WorkspaceRepository(b).record("new-note"));assertEquals("99",JSONObject(requireNotNull(WorkspaceRepository(b).record("stock")).json).getString("quantity"))
        }finally{a.close();b.close();p.close();q.close();file.delete()}
    }
    @Test fun missingMovementIsRejectedEvenWithARecomputedZipChecksum():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive();val broken=archive()
        try {
            seed(a).confirmInventoryOperation("confirmed-usage");BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            rewrite(file,broken){it.put("inventory_movements",JSONArray())}
            var rejected=false;try{BackupTransfer(context,b,q.local).previewBackup(Uri.fromFile(broken).toString())}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertNull(WorkspaceRepository(b).record("stock"))
        }finally{a.close();b.close();p.close();q.close();file.delete();broken.delete()}
    }
    @Test fun legacyBackupTwoStillImportsWithoutCreatingConsumptionEvents():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive();val legacy=archive()
        try {
            seed(a);BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            rewrite(file,legacy){data->data.put("schema",2);listOf("inventory_operations","inventory_movements","inventory_reservations").forEach {data.remove(it)}}
            val transfer=BackupTransfer(context,b,q.local);val uri=Uri.fromFile(legacy).toString();transfer.previewBackup(uri);transfer.restoreBackup(uri)
            assertEquals("14",JSONObject(requireNotNull(WorkspaceRepository(b).record("stock")).json).getString("quantity"));assertNull(b.inventory().operation("confirmed-usage"))
        }finally{a.close();b.close();p.close();q.close();file.delete();legacy.delete()}
    }
    @Test fun omittedFirstLedgerEntryIsRejectedEvenWhenTheFinalBalanceMatches():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive();val broken=archive()
        try {
            seed(a).confirmInventoryOperation("confirmed-usage");BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            rewrite(file,broken){data->val source=data.getJSONArray("inventory_movements");val kept=JSONArray();for(i in 0 until source.length())if(source.getJSONObject(i).getLong("sequence")!=1L)kept.put(source.getJSONObject(i));data.put("inventory_movements",kept)}
            var rejected=false;try{BackupTransfer(context,b,q.local).previewBackup(Uri.fromFile(broken).toString())}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertNull(WorkspaceRepository(b).record("stock"))
        }finally{a.close();b.close();p.close();q.close();file.delete();broken.delete()}
    }
    @Test fun reservationProjectionCannotInventAnOwnerInTheBackup():Unit=runBlocking {
        val a=database();val b=database();val p=Prefs(context);val q=Prefs(context);val file=archive();val broken=archive()
        try {
            val store=seed(a);store.setInventoryReservation("reserve","run","stock","10");BackupTransfer(context,a,p.local).exportBackup(Uri.fromFile(file).toString(),false)
            rewrite(file,broken){data->val rows=data.getJSONArray("workspace_records");for(i in 0 until rows.length()){val row=rows.getJSONObject(i);if(row.getString("id")=="stock"){val j=JSONObject(row.getString("json"));j.getJSONArray("inventoryReservations").getJSONObject(0).put("ownerId","invented-owner");row.put("json",j.toString())}}}
            var rejected=false;try{BackupTransfer(context,b,q.local).previewBackup(Uri.fromFile(broken).toString())}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertNull(WorkspaceRepository(b).record("stock"))
        }finally{a.close();b.close();p.close();q.close();file.delete();broken.delete()}
    }
}
