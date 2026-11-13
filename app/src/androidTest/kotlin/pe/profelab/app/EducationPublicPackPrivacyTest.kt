package pe.profelab.app

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.ContentPacks
import pe.profelab.core.database.ProfeDatabase
import pe.profelab.core.database.WorkspaceRepository
import pe.profelab.core.model.newId
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class EducationPublicPackPrivacyTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun archive(data:JSONObject,type:String="project"):File {
        val bytes=data.toString().toByteArray(Charsets.UTF_8)
        val hash=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
        val manifest=JSONObject().put("schemaVersion",3).put("contentContractVersion","3.0").put("minimumAppSchema",2)
            .put("packId","privacy-qa").put("version","1.0.0").put("items",JSONArray().put(JSONObject().put("path","definition.json").put("type",type).put("sha256",hash)))
        return File(context.cacheDir,"private-pack-${newId()}.zip").also {file->ZipOutputStream(file.outputStream()).use {zip->
            mapOf("manifest.json" to manifest.toString().toByteArray(Charsets.UTF_8),"definition.json" to bytes).forEach {(name,value)->zip.putNextEntry(ZipEntry(name));zip.write(value);zip.closeEntry()}
        }}
    }
    private fun definition()=JSONObject().put("id","public-template").put("title","Plantilla pública").put("contentOrigin","USER_AUTHORED")
        .put("distributionApproved",true).put("classroomUse","Texto pedagógico público conservado")
        .put("steps",JSONArray().put(JSONObject().put("id","step-0").put("title","Comparar materiales")))

    @Test fun publicImportStripsPrivateEducationFieldsAtEveryDepthWithoutRemovingPedagogy():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        val privateKeys=listOf("teacherPreparation","educationRunId","studentId","studentIds","students","classrooms","teamIds","participantIds","participants","evaluations","edu_private","displayName","classroomLabel","evidenceOwnerProjectId")
        val data=definition();privateKeys.forEach {data.put(it,"PRIVATE_SENTINEL")}
        data.put("extension",JSONObject().put("publicNote","Conservar extensión pública").put("children",JSONArray().put(JSONObject().also {row->privateKeys.forEach {row.put(it,"PRIVATE_SENTINEL")}})))
        val file=archive(data)
        try {
            val imported=ContentPacks(context,WorkspaceRepository(db)).inspect(Uri.fromFile(file).toString()).records.single()
            assertEquals("catalog_project",imported.kind)
            assertFalse("A public record must not retain private learner data",imported.json.contains("PRIVATE_SENTINEL"))
            val clean=JSONObject(imported.json)
            assertEquals("Texto pedagógico público conservado",clean.getString("classroomUse"))
            assertEquals("Conservar extensión pública",clean.getJSONObject("extension").getString("publicNote"))
            assertEquals("step-0",clean.getJSONArray("steps").getJSONObject(0).getString("id"))
        } finally {db.close();file.delete()}
    }

    @Test fun aPrivateEducationRecordCannotBecomeAPublicPackItem():Unit=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val file=archive(definition(),"edu_classroom")
        try {
            assertTrue(runCatching {ContentPacks(context,WorkspaceRepository(db)).inspect(Uri.fromFile(file).toString())}.isFailure)
        } finally {db.close();file.delete()}
    }
}
