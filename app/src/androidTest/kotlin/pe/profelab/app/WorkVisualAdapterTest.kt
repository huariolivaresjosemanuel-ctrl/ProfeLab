package pe.profelab.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Synthetic binary fixtures only. Their approvals are test data, never approval of Work output. */
@RunWith(AndroidJUnit4::class)
class WorkVisualAdapterTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun image(color:Int=Color.GREEN,format:Bitmap.CompressFormat=Bitmap.CompressFormat.PNG):ByteArray {
        val bitmap=Bitmap.createBitmap(32,18,Bitmap.Config.ARGB_8888);val out=ByteArrayOutputStream()
        try{bitmap.eraseColor(color);assertTrue(bitmap.compress(format,90,out))}finally{bitmap.recycle()}
        return out.toByteArray()
    }
    private fun hash(b:ByteArray)=MessageDigest.getInstance("SHA-256").digest(b).joinToString(""){"%02x".format(it)}
    private val seedDefinition by lazy {runBlocking {WorkshopSeed(context).templates().single {it.id=="exp-001"}.let {source->
        // Synthetic overlay tests isolate their own revisions; retain the real procedure fingerprint.
        source.copy(json=JSONObject(source.json).also {it.remove("editorialMediaBindings")}.toString())
    }}}
    private fun definition(id:String="exp-001",count:Int=4):WorkRecord = if(id=="exp-001" && count==4)seedDefinition else WorkRecord(id,"experiment_template","Fixture de actividad",JSONObject().put("contentVersion","1.0.0").put("steps",JSONArray().also {a->repeat(count){a.put(JSONObject().put("id","step-$it").put("title","Acción $it").put("done",false))}}).toString(),0)
    private fun row(bytes:ByteArray,role:String="step_photo",step:String="step-0",id:String="exp-001",revision:Int=1,extension:String="png")=JSONObject()
        .put("contentId",id).put("contentVersion","1.0.0").put("role",role).put("stepId",if(role=="step_photo")step else "")
        .put("mediaId","visual-$role-$step-$revision").put("mediaRevision",revision).put("relativePath","photos/fixture.$extension").put("mime",if(extension=="jpg")"image/jpeg"else "image/$extension")
        .put("width",32).put("height",18).put("sha256",hash(bytes)).put("altText","Fixture editorial del paso; no es una observación real").put("caption","Ilustración de prueba")
        .put("sourceType","ORIGINAL_QA_FIXTURE").put("licenseOrRights","USER_AUTHORED_QA").put("reviewStatus","APPROVED_BY_USER").put("safetyReviewStatus","APPROVED")
    private fun manifest(vararg rows:JSONObject)=JSONObject().put("schema","WORK_VISUAL_DELIVERY_1").put("scope","TALLER_ONLY").put("status","GENERATED_TEST_FIXTURE").put("media",JSONArray(rows.toList()))
    private fun convert(r:JSONObject,bytes:ByteArray,version:String="1.0.0",defs:List<WorkRecord> = listOf(definition()))=WorkVisualAdapter.toContentPack(manifest(r),mapOf(r.getString("relativePath") to bytes),defs,"qa-work-media",version)
    private fun zip(entries:Map<String,ByteArray>):File=File(context.cacheDir,"qa-work-${newId()}.zip").also {file->ZipOutputStream(file.outputStream()).use {z->entries.forEach {(n,b)->z.putNextEntry(ZipEntry(n));z.write(b);z.closeEntry()}}}
    private fun rejected(block:()->Unit){var rejected=false;try{block()}catch(_:IllegalArgumentException){rejected=true};assertTrue("Expected an explained rejection",rejected)}
    private fun bound(row:JSONObject,file:String="qa-private-file"):WorkRecord {
        val ref=JSONObject(row.toString()).put("fileId",file)
        if(ref.optString("role")=="step_photo")ref.put("stepFingerprint",WorkshopMediaBindings.stepFingerprint(WorkshopFlowAdapter.read(definition()).steps.first {it.id==ref.getString("stepId")}))
        return WorkRecord("overlay","catalog_media","Medios QA",JSONObject().put("contentId",row.getString("contentId")).put("contentVersion","1.0.0").put("editorialMediaBindings",JSONArray().put(ref)).toString(),1)
    }
    private class Prefs(context:Context){
        val file=File(context.cacheDir,"qa-work-${newId()}.preferences_pb");val job=SupervisorJob()
        val local=LocalPreferences(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file}))
        suspend fun close(){job.cancelAndJoin();file.delete()}
    }

    @Test fun approvedPngJpegAndWebpConvertThroughTheExistingPackValidator():Unit=runBlocking {
        for((format,ext) in listOf(Bitmap.CompressFormat.PNG to "png",Bitmap.CompressFormat.JPEG to "jpg",Bitmap.CompressFormat.WEBP to "webp")){
            val bytes=image(format=format);val source=row(bytes,extension=ext);val f=zip(convert(source,bytes));val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
            try{val pack=ContentPacks(context,WorkspaceRepository(db)).inspect(Uri.fromFile(f).toString());assertEquals(1,pack.resources.size);assertEquals("catalog_workshop_media",pack.records.single().kind)}finally{db.close();f.delete()}
        }
    }
    @Test fun pendingApprovalAndTheWorkTemplateAreNotImportable(){
        val bytes=image();val r=row(bytes);r.put("reviewStatus","PENDING_HUMAN_APPROVAL");rejected {convert(r,bytes)}
        r.put("reviewStatus","APPROVED_BY_USER");rejected {WorkVisualAdapter.toContentPack(manifest(r).put("status","TEMPLATE_NOT_REAL_OUTPUT"),mapOf(r.getString("relativePath") to bytes),listOf(definition()),"qa","1.0.0")}
        r.put("safetyReviewStatus","PENDING_REVIEW");rejected {convert(r,bytes)}
    }
    @Test fun wrongContentVersionOrUnknownStepCannotBorrowTheCover(){
        val bytes=image();val r=row(bytes);r.put("contentVersion","2.0.0");rejected {convert(r,bytes)}
        r.put("contentVersion","1.0.0").put("stepId","step-99");rejected {convert(r,bytes)}
        val cover=bound(row(bytes,role="detail_hero"));assertNull(WorkshopMediaBindings.resolve(definition(),"step_photo","step-0",overlays=listOf(cover)))
    }
    @Test fun wrongHashMimeDimensionsAndUnsafePathsAreRejected(){
        val bytes=image()
        listOf("sha256" to "0".repeat(64),"mime" to "image/jpeg","width" to 999,"relativePath" to "../secret.png").forEach {(key,value)->val r=row(bytes).put(key,value);rejected {convert(r,bytes)}}
    }
    @Test fun missingCorruptAndUnlistedFilesDoNotCreateAUsablePack(){
        val bytes=image();val r=row(bytes)
        rejected {WorkVisualAdapter.toContentPack(manifest(r),emptyMap(),listOf(definition()),"qa","1.0.0")}
        rejected {WorkVisualAdapter.toContentPack(manifest(r),mapOf(r.getString("relativePath") to bytes,"unlisted.png" to bytes),listOf(definition()),"qa","1.0.0")}
        val corrupt="not an image".toByteArray();rejected {convert(row(corrupt),corrupt)}
    }
    @Test fun samePhysicalPhotoRequiresExplicitRoleBindingsButCanBeReused(){
        val bytes=image();val cover=row(bytes,role="catalog_cover");val hero=row(bytes,role="detail_hero")
        val entries=WorkVisualAdapter.toContentPack(manifest(cover,hero),mapOf(cover.getString("relativePath") to bytes),listOf(definition()),"qa","1.0.0")
        assertNotNull(entries["manifest.json"])
        rejected {WorkVisualAdapter.toContentPack(manifest(cover,JSONObject(cover.toString()).put("mediaId","different-id")),mapOf(cover.getString("relativePath") to bytes),listOf(definition()),"qa","1.0.0")}
    }
    @Test fun resolverUsesIdVersionAndRevisionWithoutMutatingAnyRun(){
        val bytes=image();val old=row(bytes);val next=row(image(Color.BLUE),revision=2);val r=definition().copy(kind="experiment",id="run",json=JSONObject(definition().json).put("activityId","exp-001").put("results","Dato propio").toString());val before=r.json
        assertEquals(next.getString("mediaId"),WorkshopMediaBindings.resolve(r,"step_photo","step-0",overlays=listOf(bound(old),bound(next)))?.mediaId)
        assertEquals(before,r.json)
        assertNull(WorkshopMediaBindings.resolve(r.copy(json=JSONObject(r.json).put("contentVersion","legacy").toString()),"step_photo","step-0",overlays=listOf(bound(next))))
        assertNull(WorkshopMediaBindings.resolve(r,"step_photo","step-0",overlays=listOf(bound(old),bound(JSONObject(old.toString()).put("sha256",hash(image(Color.RED)))))))
    }
    @Test fun aPublicPackCannotAliasAnotherActivityWithAnArbitraryActivityId(){
        val bytes=image();val foreign=definition("pack:other:activity").copy(json=JSONObject(definition().json).put("activityId","exp-001").toString())
        assertEquals(foreign.id,WorkshopMediaBindings.contentId(foreign));assertEquals(foreign.id,WorkshopFlowAdapter.read(foreign).activityId)
        rejected {convert(row(bytes),bytes,defs=listOf(foreign))}
        val personal=foreign.copy(id="my-copy",kind="experiment",json=JSONObject(foreign.json).put("forkedFromTemplateId",foreign.id).toString())
        assertEquals(foreign.id,WorkshopMediaBindings.contentId(personal))
    }
    @Test fun editedPersonalProcedureDoesNotReceiveTheOldEditorialStepPhoto(){
        val r=definition().copy(id="run",kind="experiment",json=JSONObject(definition().json).put("activityId","exp-001").toString());val overlay=bound(row(image()))
        assertNotNull(WorkshopMediaBindings.resolve(r,"step_photo","step-0",overlays=listOf(overlay)))
        val changed=JSONObject(r.json);changed.getJSONArray("steps").getJSONObject(0).put("title","Otra acción personal distinta")
        assertNull(WorkshopMediaBindings.resolve(r.copy(json=changed.toString()),"step_photo","step-0",overlays=listOf(overlay)))
        val onlyNotes=JSONObject(r.json);onlyNotes.getJSONArray("steps").getJSONObject(0).put("notes","Mi observación").put("done",true)
        assertNotNull(WorkshopMediaBindings.resolve(r.copy(json=onlyNotes.toString()),"step_photo","step-0",overlays=listOf(overlay)))
    }
    @Test fun stepFingerprintPreservesTheBoundaryBetweenTitleAndInstruction(){
        val step=WorkshopFlowAdapter.read(definition()).steps.first()
        val first=step.copy(title="A\u0000B",instruction="C")
        val second=step.copy(title="A",instruction="B\u0000C")
        assertNotEquals(WorkshopMediaBindings.stepFingerprint(first),WorkshopMediaBindings.stepFingerprint(second))
        assertEquals(WorkshopMediaBindings.stepFingerprint(step),WorkshopMediaBindings.stepFingerprint(step.copy(done=!step.done,notes="Observación privada")))
    }
    @Test fun evidenceNeedsExplicitSelectionBeforeItCanBecomeAPrivateCover(){
        val j=JSONObject(definition().json).put("attachments",JSONArray().put(JSONObject().put("id","own-photo").put("mime","image/png")))
        val r=definition().copy(id="run",kind="experiment",json=j.toString());assertNull(WorkshopMediaBindings.preferredPrivateCover(r))
        j.put("workshopFlow",JSONObject().put("preferredCoverFileId","editorial-or-unknown"));assertNull(WorkshopMediaBindings.preferredPrivateCover(r.copy(json=j.toString())))
        j.getJSONObject("workshopFlow").put("preferredCoverFileId","own-photo");assertEquals("own-photo",WorkshopMediaBindings.preferredPrivateCover(r.copy(json=j.toString())))
        assertNull(WorkshopMediaBindings.preferredPrivateCover(r.copy(kind="experiment_template",json=j.toString())))
    }
    @Test fun mediaOnlyInstallAndRevisionKeepPersonalRecordsStockAndPrivateEvidence():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val imported=mutableListOf<File>();val files=mutableListOf<File>()
        try {
            val own=File(context.filesDir,"documents/${newId()}");own.parentFile?.mkdirs();own.writeBytes(image(Color.RED));files+=own
            val body=JSONObject(definition().json).put("activityId","exp-001").put("results","Mis resultados").put("conclusion","Mi explicación").put("attachments",JSONArray().put(JSONObject().put("id",own.name).put("mime","image/png"))).put("workshopFlow",JSONObject().put("version",1).put("lastVisitedStepId","step-2"))
            store.saveRecord(WorkRecord("run","experiment","Mi copia",body.toString(),1));store.saveRecord(WorkRecord("stock","inventory","Semillas","{\"quantity\":\"14\",\"reserved\":\"0\",\"unit\":\"unidad\"}",1))
            val beforeRun=store.record("run");val beforeStock=store.record("stock");val importer=ContentPacks(context,store)
            for(revision in 1..2){
                val bytes=image(if(revision==1)Color.GREEN else Color.BLUE);val archive=zip(convert(row(bytes,revision=revision),bytes,version="$revision.0.0"));imported+=archive
                val pack=importer.inspect(Uri.fromFile(archive).toString());importer.install(pack);importer.install(pack)
                val rows=store.observeRecords().first();assertTrue(rows.none {it.kind in setOf("catalog_project","catalog_experiment")});assertEquals(1,rows.count {it.kind=="catalog_media"})
                val media=requireNotNull(WorkshopMediaBindings.resolve(requireNotNull(store.record("run")),"step_photo","step-0",overlays=rows));val file=File(context.filesDir,"documents/${media.storageId}");files+=file;assertEquals(hash(bytes),hash(file.readBytes()))
            }
            assertEquals(beforeRun,store.record("run"));assertEquals(beforeStock,store.record("stock"));assertEquals(hash(image(Color.RED)),hash(own.readBytes()))
        }finally{db.close();imported.forEach {it.delete()};files.distinct().forEach {it.delete()}}
    }
    @Test fun newCompatibleActivityCanReceiveMediaWithoutAnotherScreen():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db);val importer=ContentPacks(context,store);val archives=mutableListOf<File>();var imageFile:File?=null
        try {
            val data=JSONObject().put("id","future-compatible").put("title","Actividad compatible QA").put("contentVersion","1.0.0").put("contentOrigin","USER_AUTHORED").put("distributionApproved",true).put("steps",JSONArray().also {a->repeat(7){a.put(JSONObject().put("id","step-$it").put("title","Acción $it"))}})
            val bytes=data.toString().toByteArray();val manifest=JSONObject().put("schemaVersion",3).put("contentContractVersion","3.0").put("minimumAppSchema",2).put("packId","qa-new-content").put("version","1.0.0").put("items",JSONArray().put(JSONObject().put("path","activity.json").put("type","project").put("sha256",hash(bytes))))
            val content=zip(mapOf("manifest.json" to manifest.toString().toByteArray(),"activity.json" to bytes));archives+=content;importer.install(importer.inspect(Uri.fromFile(content).toString()))
            val definition=store.observeRecords().first().single {it.kind=="catalog_project"};assertEquals(7,WorkshopFlowAdapter.read(definition).steps.size)
            val png=image();val ref=row(png,id=definition.id,step="step-6");val media=zip(convert(ref,png,defs=listOf(definition)));archives+=media;importer.install(importer.inspect(Uri.fromFile(media).toString()))
            val resolved=requireNotNull(WorkshopMediaBindings.resolve(definition,"step_photo","step-6",overlays=store.observeRecords().first()));imageFile=File(context.filesDir,"documents/${resolved.storageId}");assertTrue(requireNotNull(imageFile).isFile)
            assertTrue(store.observeRecords().first().none {it.kind in setOf("project","experiment")})
        }finally{db.close();archives.forEach {it.delete()};imageFile?.delete()}
    }
    @Test fun editorialRoleBindingsAndTheirFilesSurviveFullBackup():Unit=runBlocking {
        val source=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val target=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        val a=Prefs(context);val b=Prefs(context);val archive=File(context.cacheDir,"qa-work-backup-${newId()}.zip");val bytes=image();val mediaZip=zip(convert(row(bytes),bytes));var mediaFile:File?=null
        try {
            val store=WorkspaceRepository(source);val importer=ContentPacks(context,store);importer.install(importer.inspect(Uri.fromFile(mediaZip).toString()))
            val activity=definition()
            val binding=requireNotNull(WorkshopMediaBindings.resolve(activity,"step_photo","step-0",overlays=store.observeRecords().first()))
            val file=File(context.filesDir,"documents/${binding.storageId}");mediaFile=file;assertTrue(file.isFile)
            BackupTransfer(context,source,a.local).exportBackup(Uri.fromFile(archive).toString(),true);assertTrue(file.delete())
            val restore=BackupTransfer(context,target,b.local);val uri=Uri.fromFile(archive).toString();restore.previewBackup(uri);restore.restoreBackup(uri)
            val resolved=requireNotNull(WorkshopMediaBindings.resolve(activity,"step_photo","step-0",overlays=WorkspaceRepository(target).observeRecords().first()))
            assertEquals(binding.storageId,resolved.storageId);assertEquals(hash(bytes),hash(file.readBytes()))
            assertTrue(WorkspaceRepository(target).observeRecords().first().none {it.kind in setOf("project","experiment","inventory")})
        }finally{source.close();target.close();a.close();b.close();archive.delete();mediaZip.delete();mediaFile?.delete()}
    }
}
