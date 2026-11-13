package pe.profelab.app

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
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
import pe.profelab.core.content.*
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import pe.profelab.feature.taller.ActivityRunData
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Private fixtures exist only inside isolated test databases; no students are seeded in the app. */
@RunWith(AndroidJUnit4::class)
class EducationPrivateRecordsTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun db()=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
    private class Prefs(context:Context){
        val file=File(context.cacheDir,"edu-${newId()}.preferences_pb");val job=SupervisorJob()
        val local=LocalPreferences(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file}))
        suspend fun close(){job.cancelAndJoin();file.delete()}
    }
    private data class Fixture(val work:WorkRecord,val classroomA:String,val classroomB:String,val studentA:String,val studentB:String,val team:String,val runA:String,val runB:String)
    private suspend fun graph(store:WorkspaceStore,evidenceId:String?=null):Fixture {
        val definition=WorkshopSeed(context).templates().single {it.id=="mat-001"}
        val body=ActivityRunData.freshPayload(JSONObject(definition.json)).put("activityId",definition.id).put("forkedFromTemplateId",definition.id)
        evidenceId?.let {body.put("attachments",JSONArray().put(JSONObject().put("id",it).put("mime","image/png")))}
        val work=WorkRecord(newId(),"project","Actividad privada de prueba",body.toString(),1);store.saveRecord(work)
        val repo=EducationRepository(store)
        val a=repo.saveClassroom("Aula QA",4,"A",2026);val b=repo.saveClassroom("Aula QA",4,"B",2026)
        val sa=repo.saveStudent(a,"Participante QA A");val sb=repo.saveStudent(b,"Participante QA B")
        val team=repo.saveTeam(a,"Equipo QA",setOf(sa))
        val ra=repo.createRun(work,a,setOf(sa),setOf(team),1);val rb=repo.createRun(work,b,setOf(sb),emptySet(),1)
        return Fixture(work,a,b,sa,sb,team,ra,rb)
    }
    private fun photo():File=File(context.filesDir,"documents/${newId()}").also {file->
        file.parentFile?.mkdirs();val bitmap=Bitmap.createBitmap(8,8,Bitmap.Config.ARGB_8888)
        try{file.outputStream().use {assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG,100,it))}}finally{bitmap.recycle()}
    }
    private fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private fun corrupt(source:File,destination:File,change:(JSONObject)->Unit){
        val entries=linkedMapOf<String,ByteArray>()
        ZipInputStream(source.inputStream()).use {zip->while(true){val entry=zip.nextEntry ?: break;entries[entry.name]=zip.readBytes()}}
        val data=JSONObject(requireNotNull(entries["data.json"]).toString(Charsets.UTF_8));change(data)
        entries["data.json"]=data.toString().toByteArray(Charsets.UTF_8)
        val manifest=JSONObject(requireNotNull(entries["manifest.json"]).toString(Charsets.UTF_8));manifest.getJSONObject("sha256").put("data.json",hash(requireNotNull(entries["data.json"])))
        entries["manifest.json"]=manifest.toString().toByteArray(Charsets.UTF_8)
        ZipOutputStream(destination.outputStream()).use {zip->entries.forEach {(name,bytes)->zip.putNextEntry(ZipEntry(name));zip.write(bytes);zip.closeEntry()}}
    }

    @Test fun classroomsRunsAndTeamObservationsStaySeparateAndArchivingKeepsHistory():Unit=runBlocking {
        val database=db();val store=WorkspaceRepository(database)
        try {
            val f=graph(store);val repo=EducationRepository(store);val criterion=EduCriterion("qa:compare","Compara dos materiales")
            val before=store.observeRecords().first().associate {it.id to it.json}
            assertTrue(runCatching {repo.saveTeam(f.classroomA,"Equipo inválido",setOf(f.studentB))}.isFailure)
            assertTrue(runCatching {repo.saveObservation(f.runA,EduTarget(EduTargetKind.STUDENT,f.studentB),criterion,EduStatus.AUTONOMOUS,"No debe guardarse")}.isFailure)
            assertEquals(before,store.observeRecords().first().associate {it.id to it.json})
            repo.saveObservation(f.runA,EduTarget(EduTargetKind.TEAM,f.team),criterion,EduStatus.WITH_SUPPORT,"Registro del equipo")
            val all=store.observeRecords().first();val runA=EducationData.runs(all).single {it.id==f.runA};val runB=EducationData.runs(all).single {it.id==f.runB}
            assertEquals(runA.activityId,runB.activityId);assertEquals(runA.contentVersion,runB.contentVersion)
            assertEquals(1,all.count {it.kind=="project"});assertFalse(requireNotNull(store.record(f.runA)).json.contains("assetPath"))
            assertEquals("No hay observaciones registradas para esta selección.",EducationFeedback.summary(runA,EduTarget(EduTargetKind.STUDENT,f.studentA),EducationData.observations(all)))
            assertTrue(EducationFeedback.summary(runA,EduTarget(EduTargetKind.TEAM,f.team),EducationData.observations(all)).contains("no atribuye el mismo desempeño"))
            assertEquals("No hay observaciones registradas para esta selección.",EducationFeedback.summary(runB,EduTarget(EduTargetKind.STUDENT,f.studentB),EducationData.observations(all)))
            repo.archive(f.classroomA)
            val archived=store.observeRecords().first();assertTrue(EducationData.classrooms(archived).single {it.id==f.classroomA}.archived)
            assertNotNull(store.record(f.studentA));assertNotNull(store.record(f.runA));assertEquals(1,EducationData.observations(archived).size)
        }finally{database.close()}
    }

    @Test fun privateBackupRoundTripPreservesRelationsEvidenceAndWelcomeWithoutDuplicatingRuns():Unit=runBlocking {
        val source=db();val target=db();val a=Prefs(context);val b=Prefs(context);val file=photo();val archive=File(context.cacheDir,"edu-${newId()}.zip");val metadataOnly=File(context.cacheDir,"edu-text-${newId()}.zip")
        try {
            val store=WorkspaceRepository(source);val f=graph(store,file.name);val repo=EducationRepository(store)
            val observation=repo.saveObservation(f.runA,EduTarget(EduTargetKind.STUDENT,f.studentA),EduCriterion("qa:compare","Compara materiales"),EduStatus.IN_PROGRESS,"Observación privada",stepId="step-0",evidenceIds=setOf(file.name))
            val recordedAt=EducationData.observations(store.observeRecords().first()).single().at
            val current=requireNotNull(store.record(f.work.id))
            store.saveRecord(current.copy(json=JSONObject(current.json).put("attachments",JSONArray()).toString()))
            repo.saveObservation(f.runA,EduTarget(EduTargetKind.STUDENT,f.studentA),EduCriterion("qa:compare","Compara materiales"),EduStatus.IN_PROGRESS,"Observación privada editada",stepId="step-0",id=observation,evidenceIds=setOf(file.name))
            assertEquals(recordedAt,EducationData.observations(store.observeRecords().first()).single().at)
            repo.selectRun(f.work.id,f.runA)
            EducationProgress.change(store,f.work.id,"step-0",done=true,note="Nota del aula A",visit=true,expectedRunId=f.runA)
            repo.selectRun(f.work.id,f.runB)
            repo.archive(f.classroomA);a.local.markTallerWelcomeSeen();a.local.setPersonalization("Docente origen QA",LocalRole.TEACHER)
            b.local.setPersonalization("Docente destino QA",LocalRole.TEACHER)
            val before=store.observeRecords().first().associate {it.id to Triple(it.kind,it.json,it.archived)};val photoHash=hash(file.readBytes())
            val exporter=BackupTransfer(context,source,a.local)
            exporter.exportBackup(Uri.fromFile(metadataOnly).toString(),false)
            exporter.exportBackup(Uri.fromFile(archive).toString(),true);assertTrue(file.delete())
            val transfer=BackupTransfer(context,target,b.local);val uri=Uri.fromFile(archive).toString()
            assertTrue(transfer.previewBackup(Uri.fromFile(metadataOnly).toString()).contains("Referencias sin archivo en respaldo ni dispositivo: 1"))
            repeat(2){transfer.previewBackup(uri);transfer.restoreBackup(uri)}
            val restored=WorkspaceRepository(target).observeRecords().first()
            assertEquals(before,restored.associate {it.id to Triple(it.kind,it.json,it.archived)})
            assertEquals(photoHash,hash(file.readBytes()));assertEquals(2,EducationData.runs(restored).size)
            assertEquals(f.classroomA,EducationData.observations(restored).single().classroomId)
            assertTrue(JSONObject(requireNotNull(WorkspaceRepository(target).record(f.runA)).json).getJSONObject("stepProgress").getJSONObject("step-0").getBoolean("done"))
            assertTrue(b.local.preferences.first().tallerWelcomeSeen);assertEquals("Docente destino QA",b.local.preferences.first().displayName)
        }finally{source.close();target.close();a.close();b.close();archive.delete();metadataOnly.delete();file.delete()}
    }

    @Test fun classroomProgressIsIndependentAndAnOldVisibleRunCannotWriteIntoTheNewSelection():Unit=runBlocking {
        val database=db();val store=WorkspaceRepository(database)
        try {
            val f=graph(store);val repo=EducationRepository(store)
            repo.selectRun(f.work.id,f.runA)
            EducationProgress.change(store,f.work.id,"step-0",done=true,note="Nota solamente de A",visit=true,expectedRunId=f.runA)
            repo.selectRun(f.work.id,f.runB)
            val rows=store.observeRecords().first();val work=requireNotNull(store.record(f.work.id))
            assertEquals(f.runB,EducationData.selectedRun(work,rows)?.id)
            assertNull(EducationData.selectedRun(work.copy(archived=true),rows))
            val noMode=JSONObject(work.json).also {it.getJSONObject("teacherPreparation").remove("participation")}
            assertNull(EducationData.selectedRun(work.copy(json=noMode.toString()),rows))
            noMode.getJSONObject("teacherPreparation").put("participation","UNKNOWN")
            assertNull(EducationData.selectedRun(work.copy(json=noMode.toString()),rows))
            val viewB=EducationProgress.view(work,rows)
            assertFalse(viewB.steps.first().done);assertEquals("",viewB.steps.first().notes)
            val before=rows.associate {it.id to it.json}
            assertTrue(runCatching {EducationProgress.change(store,f.work.id,"step-0",done=true,expectedRunId=f.runA)}.isFailure)
            assertEquals(before,store.observeRecords().first().associate {it.id to it.json})
            repo.selectRun(f.work.id,f.runA)
            val viewA=EducationProgress.view(requireNotNull(store.record(f.work.id)),store.observeRecords().first())
            assertTrue(viewA.steps.first().done);assertEquals("Nota solamente de A",viewA.steps.first().notes)
            assertFalse(JSONObject(requireNotNull(store.record(f.work.id)).json).getJSONArray("steps").getJSONObject(0).getBoolean("done"))
        }finally{database.close()}
    }

    @Test fun aChecksumValidBackupWithCrossClassObservationIsRejectedBeforeAnyWrite():Unit=runBlocking {
        val source=db();val target=db();val a=Prefs(context);val b=Prefs(context);val file=photo();val archive=File(context.cacheDir,"edu-${newId()}.zip");val altered=File(context.cacheDir,"edu-altered-${newId()}.zip")
        try {
            val store=WorkspaceRepository(source);val f=graph(store,file.name)
            EducationRepository(store).saveObservation(f.runA,EduTarget(EduTargetKind.STUDENT,f.studentA),EduCriterion("qa:observe","Describe lo que hizo"),EduStatus.NOT_OBSERVED,"",evidenceIds=setOf(file.name))
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(archive).toString(),true)
            corrupt(archive,altered){data->val rows=data.getJSONArray("workspace_records");for(i in 0 until rows.length()){
                val row=rows.getJSONObject(i);if(row.getString("kind")=="edu_observation")row.put("json",JSONObject(row.getString("json")).put("targetId",f.studentB).toString())
            }}
            assertTrue(file.delete());val transfer=BackupTransfer(context,target,b.local)
            transfer.previewBackup(Uri.fromFile(archive).toString())
            assertTrue(runCatching {transfer.previewBackup(Uri.fromFile(altered).toString())}.isFailure)
            assertTrue("A failed preview must invalidate the previous approval",runCatching {transfer.restoreBackup(Uri.fromFile(archive).toString())}.isFailure)
            assertTrue(WorkspaceRepository(target).observeRecords().first().isEmpty());assertFalse(file.exists())
            corrupt(archive,altered){data->val rows=data.getJSONArray("workspace_records");for(i in 0 until rows.length()){
                val row=rows.getJSONObject(i);if(row.getString("id")==f.runA)row.put("json",JSONObject(row.getString("json")).put("stepProgress",JSONObject().put("foreign-step",JSONObject().put("done",true))).toString())
            }}
            assertTrue(runCatching {transfer.previewBackup(Uri.fromFile(altered).toString())}.isFailure)
            assertTrue(WorkspaceRepository(target).observeRecords().first().isEmpty());assertFalse(file.exists())
            corrupt(archive,altered){data->val rows=data.getJSONArray("workspace_records");for(i in 0 until rows.length()){
                val row=rows.getJSONObject(i);if(row.getString("kind")=="edu_observation")row.put("json",JSONObject(row.getString("json")).put("evidenceOwnerProjectId",newId()).toString())
            }}
            assertTrue(runCatching {transfer.previewBackup(Uri.fromFile(altered).toString())}.isFailure)
            assertTrue(WorkspaceRepository(target).observeRecords().first().isEmpty());assertFalse(file.exists())
        }finally{source.close();target.close();a.close();b.close();archive.delete();altered.delete();file.delete()}
    }

    @Test fun aDestinationIdentityConflictAfterPreviewCannotPartiallyRestore():Unit=runBlocking {
        val source=db();val target=db();val a=Prefs(context);val b=Prefs(context);val archive=File(context.cacheDir,"edu-${newId()}.zip")
        try {
            val sourceStore=WorkspaceRepository(source);val f=graph(sourceStore)
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(archive).toString(),false)
            val transfer=BackupTransfer(context,target,b.local);val uri=Uri.fromFile(archive).toString();transfer.previewBackup(uri)
            val targetStore=WorkspaceRepository(target)
            val student=requireNotNull(sourceStore.record(f.studentA));val conflicting=student.copy(json=JSONObject(student.json).put("classroomId",f.classroomB).toString())
            targetStore.saveBatch(listOf(requireNotNull(sourceStore.record(f.classroomA)),requireNotNull(sourceStore.record(f.classroomB)),conflicting))
            val before=targetStore.observeRecords().first().associate {it.id to it.json}
            assertTrue(runCatching {transfer.restoreBackup(uri)}.isFailure)
            assertEquals(before,targetStore.observeRecords().first().associate {it.id to it.json});assertNull(targetStore.record(f.work.id))
        }finally{source.close();target.close();a.close();b.close();archive.delete()}
    }
}
