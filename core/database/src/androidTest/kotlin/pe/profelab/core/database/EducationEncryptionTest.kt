package pe.profelab.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.newId

/** Synthetic fixtures only; validates raw Room bytes as well as repository projections. */
@RunWith(AndroidJUnit4::class)
class EducationEncryptionTest {
    @Test fun eduPayloadAndTitleAreEncryptedAndLegacyRewriteIsIdempotent():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        try {
            val id=newId();val fixture=WorkRecord(id,"edu_phrase","Synthetic private title",JSONObject().put("schemaVersion",1).put("text","Synthetic confidential note").toString(),123,4,true)
            db.workspace().put(WorkspaceEntity(fixture.id,fixture.kind,fixture.title,fixture.json,fixture.updatedAt,fixture.revision,fixture.archived))
            val store=WorkspaceRepository(db);assertEquals(fixture,store.record(id))
            store.protectEducationRecords();store.protectEducationRecords()
            val raw=requireNotNull(db.workspace().find(id));assertEquals("Registro educativo privado",raw.title);assertFalse(raw.json.contains("confidential"));assertTrue(JSONObject(raw.json).has("encryptedEducation"))
            assertEquals(fixture,store.record(id))
            store.saveRecord(fixture.copy(title="Edited synthetic title"));assertEquals("Edited synthetic title",store.record(id)?.title)
            assertFalse(requireNotNull(db.workspace().find(id)).json.contains("Edited synthetic title"))
        }finally {db.close()}
    }
    @Test fun ciphertextIsBoundToIdentityAndTamperingNeverFallsBackToPlaintext(){
        val fixture=WorkRecord(newId(),"edu_phrase","Synthetic private title","{\"text\":\"Synthetic note\"}",123)
        val encrypted=EducationEncryption.write(fixture)
        assertTrue(runCatching {EducationEncryption.read(encrypted.copy(id=newId()))}.isFailure)
        assertTrue(runCatching {EducationEncryption.read(encrypted.copy(kind="edu_student"))}.isFailure)
        val j=JSONObject(encrypted.json);val encoded=j.getString("ciphertext");j.put("ciphertext",encoded.dropLast(8)+"AAAAAAAA")
        assertTrue(runCatching {EducationEncryption.read(encrypted.copy(json=j.toString()))}.isFailure)
    }
    @Test fun migrationFailureRollsBackAndKeepsEveryLegacyRow():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        try {
            val legacy=WorkspaceEntity(newId(),"edu_phrase","Synthetic legacy title","{\"schemaVersion\":1,\"text\":\"Synthetic legacy note\"}",1,3,false)
            val corrupt=WorkspaceEntity(newId(),"edu_phrase","Synthetic damaged row","{\"encryptedEducation\":99}",2,4,false)
            db.workspace().put(legacy);db.workspace().put(corrupt)
            assertTrue(runCatching {WorkspaceRepository(db).protectEducationRecords()}.isFailure)
            assertEquals(legacy,db.workspace().find(legacy.id));assertEquals(corrupt,db.workspace().find(corrupt.id))
        }finally {db.close()}
    }
    @Test fun publicContentWithAnUnrelatedMarkerCannotInvokePrivateKeyDecoding(){
        val publicRow=WorkspaceEntity("catalog-qa","catalog_project","Synthetic public content","{\"encryptedEducation\":99}",1,1,false)
        assertEquals(publicRow.json,EducationEncryption.read(publicRow).json)
        assertEquals(publicRow.title,EducationEncryption.read(publicRow).title)
    }
    @Test fun largeGenericRevisionIsNotExpandedByAnEducationalMigration():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        try {
            val parent=newId();val json="{\"text\":\""+"x".repeat(1_950_000)+"\"}"
            val source=WorkspaceEntity(parent,"document","Synthetic document","{}",1,20,false)
            val generic=WorkspaceEntity("history:$parent:20","revision","Synthetic large generic history",json,1,20,true)
            db.workspace().put(source);db.workspace().put(generic)
            val store=WorkspaceRepository(db);store.protectEducationRecords()
            assertEquals(generic,db.workspace().find(generic.id));assertEquals(json,store.record(generic.id)?.json)
            store.saveRecord(requireNotNull(store.record(generic.id)).copy(updatedAt=2))
            assertEquals(json,db.workspace().find(generic.id)?.json)
            assertFalse(EducationEncryption.encrypted(requireNotNull(db.workspace().find(generic.id)).json))
        }finally {db.close()}
    }
    @Test fun educationalRevisionIsProtectedAcrossEveryRepositoryWritePath():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        try {
            val parent=newId();val store=WorkspaceRepository(db)
            val source=WorkRecord(parent,"edu_phrase","Synthetic educational source","{\"schemaVersion\":1,\"text\":\"Synthetic source\"}",1)
            val revision=WorkRecord("history:$parent:20","revision","Synthetic educational history","{\"schemaVersion\":1,\"text\":\"Synthetic historical note\"}",1,20,true)
            store.saveBatch(listOf(source,revision))
            assertTrue(EducationEncryption.encrypted(requireNotNull(db.workspace().find(revision.id)).json))
            store.editRecords {all->listOf(all.single {it.id==revision.id}.copy(title="Edited synthetic history"))}
            store.saveRecord(requireNotNull(store.record(revision.id)).copy(updatedAt=3))
            assertTrue(EducationEncryption.encrypted(requireNotNull(db.workspace().find(revision.id)).json))
            assertEquals("Edited synthetic history",store.record(revision.id)?.title)
            val timestamp=1_794_709_999_000L
            assertEquals(parent,EducationEncryption.privateParentId("history:$parent:$timestamp"))
            val legacy=WorkspaceEntity("history:$parent:$timestamp","revision",revision.title,revision.json,1,40,true);db.workspace().put(legacy)
            store.protectEducationRecords()
            assertTrue(EducationEncryption.encrypted(requireNotNull(db.workspace().find(legacy.id)).json))
            assertEquals(revision.json,store.record(legacy.id)?.json)
        }finally {db.close()}
    }
    @Test fun anOversizedEducationalCiphertextFailsBeforeAnyReplacementIsCommitted():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        try {
            val id=newId();val old=WorkspaceEntity(id,"edu_phrase","Synthetic legacy","{\"schemaVersion\":1,\"text\":\"Small legacy note\"}",1,2,false);db.workspace().put(old)
            val oversized=WorkRecord(id,"edu_phrase","Synthetic too large",JSONObject().put("schemaVersion",1).put("text","界".repeat(500_000)).toString(),2)
            assertTrue(runCatching {WorkspaceRepository(db).saveRecord(oversized)}.isFailure)
            assertEquals(old,db.workspace().find(id))
            val legacyOversized=old.copy(json=oversized.json)
            db.workspace().put(legacyOversized)
            assertTrue(runCatching {WorkspaceRepository(db).protectEducationRecords()}.isFailure)
            assertEquals(legacyOversized,db.workspace().find(id))
        }finally {db.close()}
    }
}
