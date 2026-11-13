package pe.profelab.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.database.LocalPreferences
import pe.profelab.core.model.newId
import java.io.File

@RunWith(AndroidJUnit4::class)
class TallerWelcomePreferencesTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private class Fixture(context:Context){
        val file=File(context.cacheDir,"taller-welcome-${newId()}.preferences_pb");val job=SupervisorJob()
        val store=PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file});val preferences=LocalPreferences(store)
        suspend fun close(){job.cancelAndJoin();file.delete()}
    }
    @Test fun welcomeSnapshotFillsAnAbsentFlagButNeverOverridesAnExistingChoice():Unit=runBlocking {
        val a=Fixture(context);val b=Fixture(context);val c=Fixture(context)
        try {
            a.preferences.markTallerWelcomeSeen();val snapshot=a.preferences.exportSnapshot()
            assertEquals(true,snapshot.get("taller_welcome_seen"));b.preferences.restoreMissing(snapshot)
            assertTrue(b.preferences.preferences.first().tallerWelcomeSeen)
            c.store.edit {it[booleanPreferencesKey("taller_welcome_seen")]=false}
            c.preferences.restoreMissing(snapshot);assertFalse(c.preferences.preferences.first().tallerWelcomeSeen)
        }finally{a.close();b.close();c.close()}
    }
    @Test fun olderSnapshotsKeepTheDefaultAndTheNewFlagRequiresAnActualBoolean():Unit=runBlocking {
        val fixture=Fixture(context)
        try {
            fixture.preferences.restoreMissing(JSONObject().put("version",1).put("show_completed_tasks",true))
            assertFalse(fixture.preferences.preferences.first().tallerWelcomeSeen);assertTrue(fixture.preferences.preferences.first().showCompleted)
            assertTrue(runCatching {fixture.preferences.validateSnapshot(JSONObject().put("version",1).put("taller_welcome_seen","true"))}.isFailure)
            assertTrue(runCatching {fixture.preferences.validateSnapshot(JSONObject().put("version",1).put("taller_welcome_seen",1))}.isFailure)
        }finally{fixture.close()}
    }
}
