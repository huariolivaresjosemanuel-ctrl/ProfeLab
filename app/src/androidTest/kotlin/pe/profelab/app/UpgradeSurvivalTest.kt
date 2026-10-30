package pe.profelab.app

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import java.io.File
import java.security.MessageDigest

/** Calls only APIs present in 43ff3ee so the same test APK can seed the old target before replacement. */
@RunWith(AndroidJUnit4::class)
class UpgradeSurvivalTest {
    private fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    @Suppress("DEPRECATION")
    private fun identity(context:Context):JSONObject {
        val info=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_SIGNATURES)
        val code=if(Build.VERSION.SDK_INT>=28)info.longVersionCode else info.versionCode.toLong()
        return JSONObject().put("uid",Process.myUid()).put("package",context.packageName).put("versionCode",code)
            .put("certificates",JSONArray(info.signatures.orEmpty().map {hash(it.toByteArray())}.sorted()))
    }
    @Test fun installedDataSurvivesApkReplacement():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val phase=InstrumentationRegistry.getArguments().getString("profelab_upgrade_phase")
        require(phase==null || phase in setOf("before","after"))
        val external=phase!=null
        if(external)require(Build.HARDWARE in setOf("ranchu","goldfish") || Build.FINGERPRINT.contains("generic")){"Cross-APK fixture is restricted to a dedicated emulator"}
        val name=if(external)"profelab.db"else "qa-survival-${newId()}.db"
        val db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).addMigrations(ProfeDatabase.MIGRATION_1_2).build()
        val ledger=File(if(external)context.filesDir else context.cacheDir,if(external)"qa-upgrade-ledger.json"else "qa-ledger-${newId()}.json")
        val workspace=WorkspaceRepository(db);val repository=LocalRepository(db);val preferences=LocalPreferences(context)
        var ownedFiles=emptyList<File>()
        try {
            if(phase!="after"){
                require(repository.observeProfile().first()==null && workspace.observeRecords().first().isEmpty()){"Fixture refuses to overwrite a populated database"}
                val id=newId();val image=File(context.filesDir,"documents/${newId()}");image.parentFile?.mkdirs()
                context.assets.open("workshop/images/mat-003.png").use {input->image.outputStream().use {input.copyTo(it)}}
                val pdf=File(context.filesDir,"documents/${newId()}")
                InstrumentationRegistry.getInstrumentation().context.assets.open("qa/EJEMPLO_PROFELAB_TEXTO_SELECCIONABLE_2P.pdf").use {input->pdf.outputStream().use {input.copyTo(it)}}
                ownedFiles=listOf(image,pdf)
                repository.saveProfile(Profile("profile-$id","Perfil antes de actualizar",6,"Institución QA"))
                repository.saveTask(Task("task-$id","Tarea previa",notes="Conservar",createdAt=1,updatedAt=1))
                val project=JSONObject().put("teacherGuide",JSONObject().put("version",1).put("introShort","Mi guion previo"))
                    .put("steps",JSONArray().put(JSONObject().put("id","step-stable").put("title","Preparar").put("done",true)))
                    .put("attachments",JSONArray().put(JSONObject().put("id",image.name).put("mime","image/png")))
                    .put("records",JSONArray().put(JSONObject().put("date","2026-10-30").put("observation","Dato registrado antes").put("photoAttachmentIds",JSONArray().put(image.name))))
                val records=listOf(
                    WorkRecord("project-$id","project","Creación previa",project.toString(),1),
                    WorkRecord("note-$id","note","Apunte previo",JSONObject().put("body","Texto privado QA").toString(),1),
                    WorkRecord("stock-$id","inventory","Papel",JSONObject().put("quantity","7").put("unit","hoja").put("reserved","2").toString(),1),
                    WorkRecord("shopping-$id","shopping_v2","Compras",JSONObject().put("receipts",JSONArray().put(JSONObject().put("id","receipt-stable"))).toString(),1),
                    WorkRecord("document-$id","document","PDF previo",JSONObject().put("fileId",pdf.name).put("lastPage",1).put("favorite",true).toString(),1),
                    WorkRecord("annotation-$id","annotation","Trazo previo",JSONObject().put("documentId","document-$id").put("page",0).put("left",.1).put("right",.2).put("top",.1).put("bottom",.2).toString(),1)
                )
                records.forEach {workspace.saveRecord(it)}
                val initialPreference=preferences.preferences.first().showCompleted
                if(external)preferences.setShowCompleted(true)
                val expected=identity(context).put("lastCheckedVersion",identity(context).getLong("versionCode")).put("showCompleted",if(external)true else initialPreference)
                    .put("profileId","profile-$id").put("taskId","task-$id")
                    .put("records",JSONArray(records.map {JSONObject().put("id",it.id).put("json",it.json).put("title",it.title)}))
                    .put("files",JSONArray(ownedFiles.map {JSONObject().put("id",it.name).put("sha256",hash(it.readBytes()))}))
                ledger.writeText(expected.toString())
            }
            val expected=JSONObject(ledger.readText());val current=identity(context)
            assertEquals(expected.getString("package"),current.getString("package"));assertEquals(expected.getInt("uid"),current.getInt("uid"));assertEquals(expected.getJSONArray("certificates").toString(),current.getJSONArray("certificates").toString())
            if(phase=="after")assertTrue("Each tested replacement must increase versionCode",current.getLong("versionCode")>expected.getLong("lastCheckedVersion"))
            assertEquals(expected.getString("profileId"),repository.observeProfile().first()?.id);assertEquals(6,repository.observeProfile().first()?.academicCycle)
            assertEquals("Conservar",repository.findTask(expected.getString("taskId"))?.notes)
            assertEquals(expected.getBoolean("showCompleted"),preferences.preferences.first().showCompleted)
            val records=expected.getJSONArray("records");for(i in 0 until records.length()){val r=records.getJSONObject(i);val actual=requireNotNull(workspace.record(r.getString("id")));assertEquals(r.getString("json"),actual.json);assertEquals(r.getString("title"),actual.title)}
            val files=expected.getJSONArray("files");for(i in 0 until files.length()){val f=files.getJSONObject(i);assertEquals(f.getString("sha256"),hash(File(context.filesDir,"documents/${f.getString("id")}").readBytes()))}
            assertEquals(2,db.openHelper.writableDatabase.version)
            if(phase=="after"){expected.put("lastCheckedVersion",current.getLong("versionCode"));ledger.writeText(expected.toString())}
        }finally{db.close();if(!external){context.deleteDatabase(name);ledger.delete();ownedFiles.forEach {it.delete()}}}
    }
}
