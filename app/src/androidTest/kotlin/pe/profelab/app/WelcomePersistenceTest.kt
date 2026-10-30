package pe.profelab.app

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.database.LocalPreferences
import pe.profelab.core.model.*
import java.io.File

@RunWith(AndroidJUnit4::class)
class WelcomePersistenceTest {
    @Test fun legacyProfileAndPreferencesSurviveSeparateIntroductionAndReopen():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val file=File(context.cacheDir,"qa-welcome-${newId()}.preferences_pb")
        var job=SupervisorJob()
        fun prefs()=LocalPreferences(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+job),produceFile={file}))
        var local=prefs()
        try {
            local.setPersonalization("María",LocalRole.INTERN);local.setShowCompleted(true);local.setMascotPreferences(false,true,false)
            local.migrateLegacyProfile(Profile("old","No reemplazar",6))
            assertFalse(local.preferences.first().welcomeIntroSeen);local.markWelcomeIntroSeen()
            job.cancelAndJoin();job=SupervisorJob();local=prefs()
            val saved=local.preferences.first();assertEquals("María",saved.displayName);assertEquals(LocalRole.INTERN,saved.role);assertTrue(saved.showCompleted);assertFalse(saved.mascotVisible);assertTrue(saved.reduceMotion);assertFalse(saved.personalizedGreetings);assertTrue(saved.welcomeIntroSeen)
        }finally{job.cancelAndJoin();file.delete()}
    }
    @Test fun portableBackupContractStaysUnchangedAndRestoreNeverReregisters():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val files=List(2){File(context.cacheDir,"qa-welcome-${newId()}.preferences_pb")};val jobs=List(2){SupervisorJob()}
        val locals=files.mapIndexed {i,file->LocalPreferences(PreferenceDataStoreFactory.create(scope=CoroutineScope(Dispatchers.IO+jobs[i]),produceFile={file}))}
        try {
            locals[0].completeWelcomeRegistration("Ana",LocalRole.STUDENT)
            val backup=locals[0].exportSnapshot();assertEquals(1,backup.getInt("version"));assertFalse(backup.has("welcome_intro_seen_v1"))
            locals[1].restoreMissing(backup);val restored=locals[1].preferences.first()
            assertEquals("Ana",restored.displayName);assertEquals(LocalRole.STUDENT,restored.role);assertTrue(restored.onboardingCompleted);assertFalse(restored.welcomeIntroSeen)
            assertEquals(WelcomeMode.EXISTING_INTRO,GreetingPolicy.mode(restored,false,false,false))
        }finally{jobs.forEach {it.cancelAndJoin()};files.forEach {it.delete()}}
    }
}
