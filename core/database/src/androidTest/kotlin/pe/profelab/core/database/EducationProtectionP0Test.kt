package pe.profelab.core.database

import android.content.Context
import android.database.sqlite.SQLiteException
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
import pe.profelab.core.model.education.EducationProtectionException
import pe.profelab.core.model.education.EducationProtectionFailureCode
import pe.profelab.core.model.education.EducationProtectionStage
import java.security.MessageDigest
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

/** Characterizes the exact failing HEAD call on Android; it never uses a user's database. */
@RunWith(AndroidJUnit4::class)
class EducationProtectionP0Test {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun memoryDb()=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
    private fun legacy(id:String=newId(),text:String="Synthetic legacy note")=WorkspaceEntity(id,"edu_phrase","Synthetic legacy title",JSONObject().put("schemaVersion",1).put("text",text).toString(),123,7,true)
    private val syntheticKey=SecretKeySpec(ByteArray(32){(it+1).toByte()},"AES")
    private class Keys(var value:SecretKey?,var failure:Exception?=null):EducationKeyAccess {
        var creates=0
        override fun existing():SecretKey? {failure?.let {throw it};return value}
        override fun createForNewData():SecretKey {creates++;return SecretKeySpec(ByteArray(32){(it+1).toByte()},"AES").also {value=it}}
    }
    private suspend fun fingerprint(db:ProfeDatabase):String {
        val digest=MessageDigest.getInstance("SHA-256")
        db.workspace().all().sortedBy {it.id}.forEach {row->
            listOf(row.id,row.kind,row.title,row.json,row.updatedAt.toString(),row.revision.toString(),row.archived.toString()).forEach {value->
                val bytes=value.toByteArray(Charsets.UTF_8);digest.update(bytes.size.toString().toByteArray());digest.update(0);digest.update(bytes)
            }
        }
        return digest.digest().joinToString(""){"%02x".format(it)}
    }
    private fun expectedFailure(error:Throwable?,code:EducationProtectionFailureCode,stage:EducationProtectionStage?=null){
        assertTrue("Failure is typed and carries no private cause",error is EducationProtectionException && error.cause==null && error.message==null)
        val typed=error as EducationProtectionException;assertEquals(code,typed.code);stage?.let {assertEquals(it,typed.stage)}
        assertTrue(typed.exceptionType.matches(Regex("[A-Za-z_$][A-Za-z0-9_$]{0,79}")))
    }
    @Test fun pragmaExecSqlBaselineReproducesCursorError(){
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
        try {
            val original=runCatching {db.openHelper.writableDatabase.execSQL("PRAGMA secure_delete=ON")}.exceptionOrNull()
            assertTrue("Baseline result-returning PRAGMA must reproduce the Android SQLite API error",original is SQLiteException)
            assertTrue("Expected the query-method error without printing its payload",original?.message.orEmpty().contains("quer",ignoreCase=true))
            db.openHelper.writableDatabase.query("SELECT COUNT(*) FROM workspace_records").use {cursor->assertTrue(cursor.moveToFirst());assertEquals(0,cursor.getInt(0))}
        }finally {db.close()}
    }
    @Test fun newDatabaseVerifiesSecureDeleteWithoutGeneratingAKey():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(null);val phases=mutableListOf<EducationProtectionStage>()
        try {
            WorkspaceRepository(db).protectEducationRecordsWithKeys(keys,phases::add)
            assertEquals(0,keys.creates);assertTrue(db.workspace().all().isEmpty())
            db.openHelper.writableDatabase.query("PRAGMA secure_delete").use {cursor->assertTrue(cursor.moveToFirst());assertEquals(1,cursor.getInt(0))}
            assertEquals(EducationProtectionStage.OPEN_DB,phases.first());assertEquals(EducationProtectionStage.VERIFY_EDU,phases.last())
            assertTrue(phases.containsAll(listOf(EducationProtectionStage.SET_SECURE_DELETE,EducationProtectionStage.READ_LEGACY,EducationProtectionStage.ENCRYPT_EDU)))
        }finally {db.close()}
    }
    @Test fun legacyAndEducationalHistoryPreserveIdentityRevisionAndContentAndAreIdempotent():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(null)
        try {
            val original=legacy();val history=original.copy(id="history:${original.id}:1794709999000",kind="revision",revision=20)
            val generic=WorkspaceEntity(newId(),"note","Synthetic generic note","{}",9,3,false)
            listOf(original,history,generic).forEach {db.workspace().put(it)}
            val expected=original.domain();val expectedHistory=history.domain();val store=WorkspaceRepository(db)
            store.protectEducationRecordsWithKeys(keys)
            assertEquals(1,keys.creates);assertEquals(3,db.workspace().all().size)
            assertTrue("Original domain fields and revision survive",requireNotNull(db.workspace().find(original.id)).domain(keys)==expected)
            assertTrue("History metadata survives",requireNotNull(db.workspace().find(history.id)).domain(keys)==expectedHistory)
            assertTrue("Unrelated rows stay byte-for-byte unchanged",db.workspace().find(generic.id)==generic)
            val after=fingerprint(db);store.protectEducationRecordsWithKeys(keys);assertEquals(after,fingerprint(db));assertEquals(1,keys.creates)
        }finally {db.close()}
    }
    @Test fun existingCiphertextIsAuthenticatedWithoutReplacementOrKeyCreation():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(syntheticKey)
        try {
            val encrypted=EducationEncryption.write(legacy().domain(),keys=keys);db.workspace().put(encrypted)
            val before=fingerprint(db);WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)
            assertEquals(before,fingerprint(db));assertEquals(0,keys.creates)
        }finally {db.close()}
    }
    @Test fun missingKeyNeverCreatesAnAliasEvenWithLegacyRowsBeforeCiphertext():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(null)
        try {
            val ciphertext=EducationEncryption.write(legacy().domain(),keys=Keys(syntheticKey))
            db.workspace().put(legacy());db.workspace().put(ciphertext);val before=fingerprint(db)
            val failure=runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull()
            expectedFailure(failure,EducationProtectionFailureCode.KEY_MISSING,EducationProtectionStage.VERIFY_EDU)
            assertEquals(0,keys.creates);assertEquals(before,fingerprint(db))
            val direct=runCatching {EducationEncryption.read(ciphertext,keys)}.exceptionOrNull()
            assertTrue(direct is EducationKeyMissingException);assertEquals(0,keys.creates)
        }finally {db.close()}
    }
    @Test fun corruptCiphertextFailsClosedWithoutRewritingAnyRow():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(syntheticKey)
        try {
            val valid=EducationEncryption.write(legacy().domain(),keys=keys)
            val body=JSONObject(valid.json);val bytes=java.util.Base64.getDecoder().decode(body.getString("ciphertext"));bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte()
            body.put("ciphertext",java.util.Base64.getEncoder().encodeToString(bytes));db.workspace().put(valid.copy(json=body.toString()));db.workspace().put(legacy())
            val before=fingerprint(db);val failure=runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull()
            expectedFailure(failure,EducationProtectionFailureCode.AUTHENTICATION_FAILED,EducationProtectionStage.VERIFY_EDU)
            assertEquals(0,keys.creates);assertEquals(before,fingerprint(db))
        }finally {db.close()}
    }
    @Test fun invalidatedKeyIsTerminalAndTransientKeyAccessCanRetryWithoutReplacingIt():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(syntheticKey)
        try {
            db.workspace().put(EducationEncryption.write(legacy().domain(),keys=keys));val before=fingerprint(db)
            keys.failure=android.security.keystore.KeyPermanentlyInvalidatedException()
            expectedFailure(runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull(),EducationProtectionFailureCode.KEY_INVALIDATED)
            assertEquals(before,fingerprint(db));assertEquals(0,keys.creates)
            keys.failure=java.security.KeyStoreException("Synthetic inaccessible key; must never be logged")
            expectedFailure(runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull(),EducationProtectionFailureCode.KEY_UNAVAILABLE)
            keys.failure=null;WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)
            assertEquals(before,fingerprint(db));assertEquals(0,keys.creates)
        }finally {db.close()}
    }
    @Test fun oversizedPayloadIsRejectedBeforeAnyKeyCreationAndRollsBackLegacyBytes():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(null)
        try {
            val oversized=legacy(text="界".repeat(500_000));db.workspace().put(oversized);val before=fingerprint(db)
            val failure=runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull()
            expectedFailure(failure,EducationProtectionFailureCode.PRIVATE_RECORD_TOO_LARGE,EducationProtectionStage.ENCRYPT_EDU)
            assertEquals(0,keys.creates);assertEquals(before,fingerprint(db))
        }finally {db.close()}
    }
    @Test fun actualSQLitePageLimitRollsBackAndCanRetryAfterCapacityIsRaised():Unit=runBlocking {
        val name="p0-capacity-${newId()}.db"
        val db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).setJournalMode(androidx.room.RoomDatabase.JournalMode.TRUNCATE).build();val keys=Keys(syntheticKey)
        try {
            val payload=buildString {repeat(120_000){append(('A'.code+(it*17)%26).toChar())}}
            db.workspace().put(legacy(text=payload));val sql=db.openHelper.writableDatabase
            val pages=sql.query("PRAGMA page_count").use {cursor->assertTrue(cursor.moveToFirst());cursor.getInt(0)}
            sql.query("PRAGMA max_page_count=$pages").use {cursor->assertTrue(cursor.moveToFirst());assertEquals(pages,cursor.getInt(0))}
            val before=fingerprint(db);val failure=runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull()
            expectedFailure(failure,EducationProtectionFailureCode.STORAGE_UNAVAILABLE,EducationProtectionStage.ENCRYPT_EDU)
            assertEquals(before,fingerprint(db));assertEquals(0,keys.creates)
            sql.query("PRAGMA max_page_count=${pages+200}").use {cursor->assertTrue(cursor.moveToFirst())}
            WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)
            assertTrue(db.workspace().all().all {EducationEncryption.encrypted(it.json)})
        }finally {db.close();context.deleteDatabase(name)}
    }
    @Test fun malformedLegacyAndAuthenticatedMalformedPayloadRemainUnchanged():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(syntheticKey)
        try {
            val malformed=legacy().copy(json="{invalid")
            db.workspace().put(malformed);val legacyBefore=fingerprint(db)
            expectedFailure(runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull(),EducationProtectionFailureCode.INVALID_PRIVATE_RECORD,EducationProtectionStage.VERIFY_EDU)
            assertEquals(legacyBefore,fingerprint(db));assertEquals(0,keys.creates)
            db.workspace().put(EducationEncryption.write(malformed.domain(),keys=keys));val cipherBefore=fingerprint(db)
            expectedFailure(runCatching {WorkspaceRepository(db).protectEducationRecordsWithKeys(keys)}.exceptionOrNull(),EducationProtectionFailureCode.INVALID_PRIVATE_RECORD,EducationProtectionStage.VERIFY_EDU)
            assertEquals(cipherBefore,fingerprint(db));assertEquals(0,keys.creates)
        }finally {db.close()}
    }
    @Test fun newSensitiveWriteCannotGenerateAReplacementForMissingExistingKey():Unit=runBlocking {
        val db=memoryDb();val keys=Keys(null)
        try {
            db.workspace().put(EducationEncryption.write(legacy().domain(),keys=Keys(syntheticKey)));val before=fingerprint(db)
            val newRecord=legacy().domain();val store=WorkspaceRepository(db,keys)
            val failure=runCatching {store.saveRecord(newRecord)}.exceptionOrNull()
            expectedFailure(failure,EducationProtectionFailureCode.KEY_MISSING,EducationProtectionStage.VERIFY_EDU)
            assertEquals(0,keys.creates);assertEquals(before,fingerprint(db))
        }finally {db.close()}
    }
    @Test fun realDeviceKeyPreservesTheExactUtf8PayloadAndExpectedGcmLength(){
        val fixture=legacy(text="界".repeat(100_000)).domain()
        val expected=JSONObject().put("title",fixture.title).put("json",fixture.json).toString().toByteArray(Charsets.UTF_8)
        val encrypted=EducationEncryption.write(fixture)
        val body=JSONObject(encrypted.json)
        assertEquals(expected.size+16,java.util.Base64.getDecoder().decode(body.getString("ciphertext")).size)
        assertTrue("Authenticated domain equals the original bounded UTF-8 fixture",EducationEncryption.read(encrypted)==fixture)
        assertTrue("Stored wrapper stays within the conservative CursorWindow budget",encrypted.json.toByteArray(Charsets.UTF_8).size<EducationRecordLimits.maximumStoredBytes)
    }
}
