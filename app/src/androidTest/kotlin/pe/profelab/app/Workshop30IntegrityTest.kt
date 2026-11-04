package pe.profelab.app

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.WorkshopFlowAdapter
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import pe.profelab.feature.taller.ActivityResultsData
import java.io.File
import java.security.MessageDigest

/** Real Room/backup cases. These require an actual Android run; source inspection is not a PASS. */
@RunWith(AndroidJUnit4::class)
class Workshop30IntegrityTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private val historicalKeys=listOf("results","analysis","limitations","conclusion","records","attachments","teacherGuide","privateExtension")
    private fun fixture(count:Int=4,fileId:String?=null):WorkRecord {
        val steps=JSONArray();repeat(count){steps.put(JSONObject().put("id","stable-$it").put("title","Acción ${it+1}").put("done",false).put("notes","Nota previa ${it+1}"))}
        val attachment=JSONArray();fileId?.let {attachment.put(JSONObject().put("id",it).put("mime","image/png").put("at","2026-10-30T12:00:00Z"))}
        val observation=JSONObject().put("id","observation-before").put("date","2026-10-30").put("moment","Durante").put("observation","Observación anterior, sin medición prefabricada").put("privateExtra","conservar")
        fileId?.let {observation.put("photoAttachmentIds",JSONArray(listOf(it)))}
        val j=JSONObject().put("activityId","pack:qa-integrity:activity").put("contentVersion","1.0.0").put("steps",steps)
            .put("results","Producto anterior").put("analysis","Análisis anterior").put("limitations","Límites anteriores").put("conclusion","Reflexión anterior")
            .put("records",JSONArray().put(observation)).put("attachments",attachment)
            .put("teacherGuide",JSONObject().put("version",1).put("introShort","Guion propio anterior"))
            .put("privateExtension",JSONObject().put("keep",true))
        return WorkRecord("qa-personal-run","project","Actividad personal QA",j.toString(),1)
    }
    private fun historical(record:WorkRecord)=JSONObject(record.json).let {j->historicalKeys.associateWith {j.get(it).toString()}}
    private fun hash(file:File)=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString(""){"%02x".format(it)}
    private class PreferencesFixture(context:Context){
        val file=File(context.cacheDir,"qa-integrity-${newId()}.preferences_pb");val job=SupervisorJob()
        val local=LocalPreferences(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file}))
        suspend fun close(){job.cancelAndJoin();file.delete()}
    }
    @Test fun oneFourAndManyStepsHaveIndependentVisitAndCompletionWithoutErasingResults(){
        for(count in listOf(1,4,23,100)){
            val original=fixture(count);val bytes=original.json;val expected=historical(original)
            val visited=original.copy(json=WorkshopFlowAdapter.visit(original,"stable-${count-1}").toString())
            val view=WorkshopFlowAdapter.read(visited)
            assertEquals(count,view.steps.size);assertEquals(count-1,view.resumeIndex);assertEquals(0,view.completedSteps)
            val completed=visited.copy(json=WorkshopFlowAdapter.mark(visited,"stable-${count-1}",true).toString())
            assertEquals(1,WorkshopFlowAdapter.read(completed).completedSteps)
            val undone=completed.copy(json=WorkshopFlowAdapter.mark(completed,"stable-${count-1}",false).toString())
            assertEquals(0,WorkshopFlowAdapter.read(undone).completedSteps);assertEquals(expected,historical(undone));assertEquals(bytes,original.json)
        }
    }
    @Test fun publicDefinitionCannotCreateRunOrProgressByReadingAndUnknownVisitsAreRejected(){
        val template=fixture().copy(kind="catalog_project");val original=template.json
        repeat(3){assertFalse(WorkshopFlowAdapter.read(template).personal);assertNull(WorkshopFlowAdapter.read(template).reference().activityRunId)}
        for(record in listOf(template,fixture())){
            var rejected=false
            try {WorkshopFlowAdapter.visit(record,"absent-step")}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected)
        }
        assertEquals(original,template.json)
    }
    @Test fun editingAnyResultFieldPreservesAllOtherHistoricalValuesAndPrivateExtensions(){
        for(kind in listOf("project","experiment")){
            val original=fixture().copy(kind=kind);val before=historical(original);val bytes=original.json
            for(field in ActivityResultsData.fields){
                val next=original.copy(json=ActivityResultsData.update(original,field,"Texto personal nuevo").toString())
                val after=historical(next)
                assertEquals("Texto personal nuevo",after.getValue(field))
                before.filterKeys {it!=field}.forEach {(key,value)->assertEquals(key,value,after.getValue(key))}
                assertEquals(bytes,original.json)
            }
        }
    }
    @Test fun resultsRejectPublicOrArchivedCopiesUnknownFieldsAndOverlongText(){
        val cases=listOf(
            Triple(fixture().copy(kind="project_template"),"results","Texto"),
            Triple(fixture().copy(kind="catalog_experiment"),"analysis","Texto"),
            Triple(fixture().copy(archived=true),"conclusion","Texto"),
            Triple(fixture(),"attachments","No reemplazar evidencias"),
            Triple(fixture(),"results","x".repeat(ActivityResultsData.MAX_TEXT+1)))
        cases.forEach {(record,field,value)->
            val before=record.json;var rejected=false
            try{ActivityResultsData.update(record,field,value)}catch(_:IllegalArgumentException){rejected=true}
            assertTrue("$field / ${record.kind}",rejected);assertEquals(before,record.json)
        }
    }
    @Test fun intentionallyEmptyConclusionIsSavedWithoutRestoringAnEditorialDefault(){
        val original=fixture();val next=ActivityResultsData.update(original,"conclusion","")
        assertTrue(next.has("conclusion"));assertEquals("",next.getString("conclusion"));assertEquals("Análisis anterior",next.getString("analysis"))
    }
    @Test fun reorderedStepsPreserveTheLastVisitedIdentityAndHistoricalNotes(){
        val original=fixture(23);val j=WorkshopFlowAdapter.visit(original,"stable-19")
        val old=j.getJSONArray("steps");val reordered=JSONArray();for(i in old.length()-1 downTo 0)reordered.put(old.get(i));j.put("steps",reordered)
        val actual=original.copy(json=j.toString());val view=WorkshopFlowAdapter.read(actual)
        assertEquals("stable-19",view.steps[view.resumeIndex].id);assertEquals("Nota previa 20",view.steps[view.resumeIndex].notes)
        assertEquals(historical(original),historical(actual))
    }
    @Test fun closeAndReopenKeepsResultsEvidenceAndTheIndependentVisit():Unit=runBlocking {
        val name="qa-integrity-reopen-${newId()}.db";var db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build()
        val original=fixture(23)
        try {
            val visited=original.copy(json=WorkshopFlowAdapter.visit(original,"stable-19").toString())
            WorkspaceRepository(db).saveRecord(visited);db.close()
            db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build()
            val saved=requireNotNull(WorkspaceRepository(db).record(original.id))
            assertEquals(historical(original),historical(saved));assertEquals("stable-19",WorkshopFlowAdapter.read(saved).lastVisitedStepId);assertEquals(0,WorkshopFlowAdapter.read(saved).completedSteps)
            assertEquals(3,db.openHelper.writableDatabase.version)
        }finally{db.close();context.deleteDatabase(name)}
    }
    @Test fun fullBackupRestoresResultsEvidenceAndPendingConsumptionAsOneUnchangedAggregate():Unit=runBlocking {
        val source=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val destination=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        val p=PreferencesFixture(context);val q=PreferencesFixture(context);val backup=File(context.cacheDir,"qa-integrity-${newId()}.zip")
        val photo=File(context.filesDir,"documents/${newId()}").apply {parentFile?.mkdirs()}
        try {
            context.assets.open("workshop/images/mat-003.png").use {input->photo.outputStream().use {input.copyTo(it)}}
            val digest=hash(photo);val original=fixture(23,photo.name);val store=WorkspaceRepository(source)
            store.saveRecord(original.copy(json=WorkshopFlowAdapter.visit(original,"stable-19").toString()))
            store.saveRecord(WorkRecord("qa-stock","inventory","Ligas","{\"materialId\":\"ligas\",\"materialKind\":\"CONSUMABLE\",\"quantity\":\"14\",\"reserved\":\"0\",\"unit\":\"unidad\"}",1))
            store.setInventoryReservation("qa-reserve",original.id,"qa-stock","10")
            store.prepareConsumption(ConsumptionRequest("qa-consume",original.id,"Uso real confirmado posteriormente",listOf(ActualUsage("qa-stock","8"))))
            val saved=requireNotNull(store.record(original.id));BackupTransfer(context,source,p.local).exportBackup(Uri.fromFile(backup).toString(),true)
            assertTrue(photo.delete())
            val target=WorkspaceRepository(destination);val transfer=BackupTransfer(context,destination,q.local);val uri=Uri.fromFile(backup).toString()
            transfer.previewBackup(uri);transfer.restoreBackup(uri);transfer.previewBackup(uri);transfer.restoreBackup(uri)
            assertEquals(saved.json,target.record(original.id)?.json);assertEquals(digest,hash(photo));assertEquals("PREPARED",destination.inventory().operation("qa-consume")?.status)
            assertEquals("14",JSONObject(requireNotNull(target.record("qa-stock")).json).getString("quantity"));assertEquals("10",JSONObject(requireNotNull(target.record("qa-stock")).json).getString("reserved"))
            target.confirmInventoryOperation("qa-consume");target.confirmInventoryOperation("qa-consume")
            assertEquals("6",JSONObject(requireNotNull(target.record("qa-stock")).json).getString("quantity"));assertEquals(saved.json,target.record(original.id)?.json)
        }finally{source.close();destination.close();p.close();q.close();backup.delete();photo.delete()}
    }
}
