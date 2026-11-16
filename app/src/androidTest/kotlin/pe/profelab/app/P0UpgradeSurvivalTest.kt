package pe.profelab.app

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Process
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import pe.profelab.core.database.*
import pe.profelab.core.model.*
import java.io.File
import java.security.MessageDigest

/**
 * Cross-APK laboratory only. BEFORE uses the existing 3.2 API, not new protection contract types.
 * No Activity is launched, database/fixture is never deleted, and no key alias is reset or replaced.
 * Use the same test APK with a compatible debug-signed old target and then adb install -r new target.
 */
@RunWith(AndroidJUnit4::class)
class P0UpgradeSurvivalTest {
    private val ledgerName="qa-p0-upgrade-ledger-v1.json"
    private val format="PFLB_P0_UPGRADE_LAB_V1"
    private fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
    private fun digest(value:String)=hash(value.toByteArray(Charsets.UTF_8))
    private fun domainHash(record:WorkRecord)=digest(JSONArray().put(record.id).put(record.kind).put(record.title).put(record.json).put(record.updatedAt).put(record.revision).put(record.archived).toString())
    private fun rawHash(record:WorkspaceEntity)=digest(JSONArray().put(record.id).put(record.kind).put(record.title).put(record.json).put(record.updatedAt).put(record.revision).put(record.archived).toString())
    private fun encrypted(json:String):Boolean {val j=JSONObject(json);return j.optInt("encryptedEducation",0)==1 && j.has("iv") && j.has("ciphertext")}

    @Suppress("DEPRECATION")
    private fun identity(context:Context):JSONObject {
        val info=context.packageManager.getPackageInfo(context.packageName,PackageManager.GET_SIGNATURES)
        val version=if(Build.VERSION.SDK_INT>=28)info.longVersionCode else info.versionCode.toLong()
        val certificates=info.signatures.orEmpty().map {hash(it.toByteArray())}.sorted()
        require(certificates.isNotEmpty()){"Laboratory signing identity is unavailable"}
        return JSONObject().put("package",context.packageName).put("uid",Process.myUid()).put("versionCode",version).put("certificates",JSONArray(certificates))
    }

    /** Count/hash only; no SQL cell value is exported to the ledger. */
    private fun tableSnapshot(db:ProfeDatabase):JSONObject {
        val sql=db.openHelper.writableDatabase;val tables=mutableListOf<String>()
        sql.query("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name").use {cursor->while(cursor.moveToNext()){
            val name=cursor.getString(0)
            if(!name.startsWith("sqlite_") && name !in setOf("android_metadata","room_master_table")){
                require(name.matches(Regex("[A-Za-z_][A-Za-z0-9_]*")));tables+=name
            }
        }}
        return JSONObject().also {out->tables.forEach {table->
            val rows=mutableListOf<String>()
            sql.query("SELECT * FROM \"$table\"").use {cursor->while(cursor.moveToNext()){
                val row=JSONArray()
                for(column in 0 until cursor.columnCount)when(cursor.getType(column)){
                    android.database.Cursor.FIELD_TYPE_NULL->row.put(JSONObject.NULL)
                    android.database.Cursor.FIELD_TYPE_INTEGER->row.put(cursor.getLong(column))
                    android.database.Cursor.FIELD_TYPE_FLOAT->row.put(cursor.getDouble(column))
                    android.database.Cursor.FIELD_TYPE_BLOB->row.put(hash(cursor.getBlob(column)))
                    else->row.put(cursor.getString(column))
                }
                rows+=row.toString()
            }}
            out.put(table,JSONObject().put("count",rows.size).put("sha256",digest(rows.sorted().joinToString("\n"))))
        }}
    }

    private suspend fun seedBefore(context:Context,db:ProfeDatabase,store:WorkspaceRepository,ledger:File) {
        require(!ledger.exists()){"Laboratory fixture already exists; BEFORE never overwrites it"}
        val empty=tableSnapshot(db)
        require(empty.keys().asSequence().all {empty.getJSONObject(it).getInt("count")==0}){"BEFORE refuses a populated database"}
        require(store.observeRecords().first().isEmpty()){"BEFORE refuses existing app records"}
        require(!File(context.filesDir,"documents").exists() || File(context.filesDir,"documents").listFiles().orEmpty().isEmpty()){"BEFORE refuses existing private documents"}

        val fixtureId=newId();val old=EducationRepository(store);val educational=Education32Repository(store)
        val room=old.saveClassroom("Aula laboratorio",2,"A",2026)
        val next=old.saveClassroom("Aula laboratorio",3,"A",2027)
        val students=(1..6).map {old.saveStudent(room,"Alias laboratorio $it")}
        val enrollments=students.map {educational.enrollStudent(it,room)}
        educational.enrollStudent(students.first(),next)
        val subject=educational.saveOffering(room,"Matemática")
        educational.saveOffering(room,"Comunicación");educational.saveOffering(room,"Ciencia y Tecnología")
        educational.saveAcademicAssessment(students.first(),enrollments.first(),subject,"Propósito docente de laboratorio","Explica su procedimiento","Periodo de laboratorio",EduAcademicTaskKind.PRACTICE,EduOriginalScale.NUMERIC_0_20,"17.5","Producción sintética revisada","Observación sintética","Practicar otra representación","Docente laboratorio")
        val now=System.currentTimeMillis()
        educational.saveGuardian(students.first(),"Contacto de laboratorio","Tutor","","guardian@example.invalid",EduFamilyChannel.EMAIL,true,"LEARNING_REPORT",0,now+365L*24*60*60*1000,true)

        val image=File(context.filesDir,"documents/${newId()}")
        require(image.parentFile!!.mkdirs() || image.parentFile!!.isDirectory)
        require(image.createNewFile()){"Laboratory file must be new"}
        context.assets.open("workshop/images/mat-003.png").use {input->image.outputStream().use {input.copyTo(it)}}
        val project=WorkRecord(newId(),"project","Actividad de laboratorio",JSONObject().put("activityId","mat-001").put("contentVersion","qa-upgrade-v1")
            .put("steps",JSONArray().put(JSONObject().put("id","step-0").put("title","Explora")))
            .put("observationCriteria",JSONArray().put(JSONObject().put("criterionId","qa-upgrade:criterion").put("label","Explica su procedimiento")))
            .put("attachments",JSONArray().put(JSONObject().put("id",image.name).put("mime","image/png"))).toString(),now)
        store.saveRecord(project)
        val team=old.saveTeam(room,"Equipo laboratorio",students.take(3).toSet())
        val run=old.createRun(project,room,students.toSet(),setOf(team),6)
        old.saveObservation(run,EduTarget(EduTargetKind.STUDENT,students.first()),EduCriterion("qa-upgrade:criterion","Explica su procedimiento"),EduStatus.AUTONOMOUS,"Registro individual sintético",stepId="step-0",evidenceIds=setOf(image.name))
        val encryptedPhrase=old.savePhrase("Frase cifrada de laboratorio")
        old.archive(encryptedPhrase)
        store.saveRecord(WorkRecord(newId(),"inventory","Papel laboratorio",JSONObject().put("quantity","8").put("unit","hoja").put("reserved","0").toString(),now))
        val owner=newId()
        store.saveRecord(WorkRecord(owner,"note","Fixture laboratorio P0",JSONObject().put("p0UpgradeFixture",fixtureId).toString(),now))
        val repository=LocalRepository(db)
        repository.saveProfile(Profile(newId(),"Docente laboratorio",6,"Centro sintético"))
        repository.saveTask(Task(newId(),"Tarea de laboratorio",notes="Conservar actualización",createdAt=now,updatedAt=now))
        val preferences=LocalPreferences(context)
        preferences.completeWelcomeRegistration("Docente laboratorio",LocalRole.TEACHER)
        preferences.markTallerWelcomeSeen()

        // A valid plaintext legacy row is inserted only into this proven-empty synthetic DB.
        val legacy=newId()
        db.workspace().put(WorkspaceEntity(legacy,"edu_phrase","Frase legado laboratorio",JSONObject().put("schemaVersion",1).put("text","Frase legado de laboratorio").toString(),now,7,true))
        val raw=db.workspace().all().associateBy {it.id};val domain=store.observeRecords().first().sortedBy {it.id}
        require(raw.values.count {it.kind.startsWith("edu_") && encrypted(it.json)}>0)
        require(raw.values.count {it.kind.startsWith("edu_") && !encrypted(it.json)}==1)
        val manifest=JSONObject().put("format",format).put("fixtureId",fixtureId).put("ownerRecordId",owner).put("identity",identity(context)).put("legacyId",legacy)
            .put("preferenceHash",digest(preferences.exportSnapshot().toString())).put("tables",tableSnapshot(db))
            .put("records",JSONArray(domain.map {record->val stored=requireNotNull(raw[record.id]);JSONObject().put("id",record.id).put("kind",record.kind).put("revision",record.revision).put("archived",record.archived).put("updatedAt",record.updatedAt).put("domainSha256",domainHash(record)).put("rawSha256",rawHash(stored)).put("encryptedBefore",encrypted(stored.json))}))
            .put("files",JSONArray().put(JSONObject().put("id",image.name).put("sha256",hash(image.readBytes()))))
        require(ledger.createNewFile()){"Laboratory ledger must be new"}
        ledger.writeText(manifest.toString(),Charsets.UTF_8)
    }

    private suspend fun verifyOwnedFixture(context:Context,db:ProfeDatabase,store:WorkspaceRepository,manifest:JSONObject,after:Boolean):JSONObject {
        require(manifest.getString("format")==format){"Unknown laboratory ledger"}
        val owner=requireNotNull(store.record(manifest.getString("ownerRecordId"))){"Laboratory ownership record is missing"}
        require(owner.kind=="note" && JSONObject(owner.json).getString("p0UpgradeFixture")==manifest.getString("fixtureId")){"Database is not this laboratory fixture"}
        val current=identity(context);val baseline=manifest.getJSONObject("identity")
        assertEquals(baseline.getString("package"),current.getString("package"));assertEquals(baseline.getInt("uid"),current.getInt("uid"))
        assertEquals(baseline.getJSONArray("certificates").toString(),current.getJSONArray("certificates").toString())
        assertTrue("Replacement may preserve versionCode in this debug laboratory",current.getLong("versionCode")>=baseline.getLong("versionCode"))
        val expected=manifest.getJSONArray("records");val domain=store.observeRecords().first().associateBy {it.id};val raw=db.workspace().all().associateBy {it.id}
        val expectedIds=(0 until expected.length()).map {expected.getJSONObject(it).getString("id")}.toSet()
        assertEquals(expectedIds,domain.keys);assertEquals(expectedIds,raw.keys)
        val domainHashes=mutableListOf<String>();val retainedCipherHashes=mutableListOf<String>()
        for(index in 0 until expected.length()){
            val row=expected.getJSONObject(index);val id=row.getString("id");val saved=requireNotNull(domain[id]);val disk=requireNotNull(raw[id])
            assertEquals(row.getString("kind"),saved.kind);assertEquals(row.getInt("revision"),saved.revision);assertEquals(row.getBoolean("archived"),saved.archived);assertEquals(row.getLong("updatedAt"),saved.updatedAt)
            assertEquals(row.getString("domainSha256"),domainHash(saved));domainHashes+=domainHash(saved)
            if(row.getBoolean("encryptedBefore")){
                assertTrue(encrypted(disk.json));assertEquals("Existing ciphertext must not be rewritten",row.getString("rawSha256"),rawHash(disk));retainedCipherHashes+=rawHash(disk)
            }else if(id!=manifest.getString("legacyId"))assertEquals(row.getString("rawSha256"),rawHash(disk))
        }
        if(after)assertTrue("The legacy fixture must become authenticated ciphertext",encrypted(requireNotNull(raw[manifest.getString("legacyId")]).json))
        else assertFalse(encrypted(requireNotNull(raw[manifest.getString("legacyId")]).json))
        assertEquals(manifest.getString("preferenceHash"),digest(LocalPreferences(context).exportSnapshot().toString()))
        val tables=tableSnapshot(db);val oldTables=manifest.getJSONObject("tables")
        assertEquals(oldTables.keys().asSequence().toSet(),tables.keys().asSequence().toSet())
        oldTables.keys().forEach {table->
            assertEquals(oldTables.getJSONObject(table).getInt("count"),tables.getJSONObject(table).getInt("count"))
            if(table!="workspace_records")assertEquals(oldTables.getJSONObject(table).getString("sha256"),tables.getJSONObject(table).getString("sha256"))
        }
        val files=manifest.getJSONArray("files");val fileHashes=mutableListOf<String>()
        for(index in 0 until files.length()){
            val entry=files.getJSONObject(index);val id=entry.getString("id");require(id.matches(Regex("[a-zA-Z0-9-]+")))
            val actual=hash(File(context.filesDir,"documents/$id").readBytes());assertEquals(entry.getString("sha256"),actual);fileHashes+=actual
        }
        assertEquals(6,EducationData.students(domain.values.toList()).size)
        assertEquals("17.5",Education32Data.academicAssessments(domain.values.toList()).single().value)
        return JSONObject().put("records",domain.size).put("students",6).put("files",files.length()).put("preservedCiphertexts",retainedCipherHashes.size)
            .put("domainSha256",digest(domainHashes.sorted().joinToString("\n"))).put("preservedCiphertextsSha256",digest(retainedCipherHashes.sorted().joinToString("\n"))).put("filesSha256",digest(fileHashes.sorted().joinToString("\n")))
    }

    @Test fun laboratoryDataSurvivesDebugApkReplacementWithoutUninstall():Unit=runBlocking {
        val arguments=InstrumentationRegistry.getArguments();val phase=arguments.getString("profelab_p0_upgrade_phase")
        assumeTrue("Cross-APK laboratory runs only with explicit phase",phase!=null)
        require(phase in setOf("before","after"))
        require(arguments.getString("profelab_p0_upgrade_lab")=="fresh-emulator-5580"){"A dedicated fresh laboratory must be explicitly declared"}
        require(Build.HARDWARE in setOf("ranchu","goldfish") || Build.FINGERPRINT.contains("generic")){"Laboratory fixtures are restricted to emulators"}
        val context=ApplicationProvider.getApplicationContext<Context>()
        require(context.packageName=="pe.profelab.app" && context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE!=0){"Only the compatible debug laboratory target is allowed"}
        val ledger=File(context.filesDir,ledgerName)
        val db=ProfeDatabase.create(context);val store=WorkspaceRepository(db)
        try {
            if(phase=="before")seedBefore(context,db,store,ledger)
            require(ledger.isFile){"AFTER requires the existing BEFORE ledger; it never creates an empty fixture"}
            val manifest=JSONObject(ledger.readText(Charsets.UTF_8))
            if(phase=="after"){
                // Validate identity, ownership and all existing domains before the new migration runs.
                // The legacy row may already be protected if the new app opened before instrumentation.
                verifyOwnedFixture(context,db,store,manifest,after=encrypted(requireNotNull(db.workspace().find(manifest.getString("legacyId"))).json))
                store.protectEducationRecords()
            }
            val summary=verifyOwnedFixture(context,db,store,manifest,after=phase=="after")
            val summaryFile=File(context.filesDir,"qa-p0-upgrade-$phase-summary.json")
            if(summaryFile.exists())assertEquals(summaryFile.readText(Charsets.UTF_8),summary.toString())
            else {require(summaryFile.createNewFile());summaryFile.writeText(summary.toString(),Charsets.UTF_8)}
            val safe="P0_UPGRADE_LAB phase=$phase records=${summary.getInt("records")} students=${summary.getInt("students")} files=${summary.getInt("files")} domainSha256=${summary.getString("domainSha256")} retainedCiphertextsSha256=${summary.getString("preservedCiphertextsSha256")} filesSha256=${summary.getString("filesSha256")}\n"
            InstrumentationRegistry.getInstrumentation().sendStatus(0,Bundle().apply {putString("stream",safe)})
        }finally {db.close()}
    }
}
