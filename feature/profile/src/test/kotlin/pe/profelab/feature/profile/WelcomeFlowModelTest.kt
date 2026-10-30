package pe.profelab.feature.profile

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.*
import org.junit.Assert.*
import pe.profelab.core.model.*

@OptIn(ExperimentalCoroutinesApi::class)
class WelcomeFlowModelTest {
    @Before fun main(){Dispatchers.setMain(StandardTestDispatcher())}
    @After fun reset(){Dispatchers.resetMain()}
    private class Prefs(initial:UserPreferences):PreferencesRepository {
        override val preferences=MutableStateFlow(initial);var writes=0;var fail=false
        override suspend fun setShowCompleted(enabled:Boolean){preferences.value=preferences.value.copy(showCompleted=enabled)}
        override suspend fun setPersonalization(name:String,role:LocalRole){preferences.value=preferences.value.copy(displayName=name,role=role,onboardingCompleted=true)}
        override suspend fun setMascotPreferences(visible:Boolean,reduceMotion:Boolean,personalizedGreeting:Boolean){preferences.value=preferences.value.copy(mascotVisible=visible,reduceMotion=reduceMotion,personalizedGreetings=personalizedGreeting)}
        override suspend fun completeWelcomeRegistration(name:String,role:LocalRole){if(fail)throw java.io.IOException("fixture");writes++;preferences.value=preferences.value.copy(displayName=PersonalizationRules.name(name),role=role,onboardingCompleted=true,welcomeIntroSeen=true)}
        override suspend fun markWelcomeIntroSeen(){if(fail)throw java.io.IOException("fixture");writes++;preferences.value=preferences.value.copy(welcomeIntroSeen=true)}
    }
    private fun profiles(value:Profile?=null)=object:ProfileRepository {
        override fun observeProfile()=MutableStateFlow(value)
        override suspend fun saveProfile(profile:Profile){error("Welcome must not overwrite a Room profile")}
    }
    @Test fun existingNameAndRoleSurviveIntroduction():Unit=runTest {
        val p=Prefs(UserPreferences(displayName="María",role=LocalRole.INTERN,onboardingCompleted=true))
        val vm=WelcomeFlowModel(p,profiles(Profile("old","María",6)),{});runCurrent();assertEquals(WelcomeMode.EXISTING_INTRO,vm.state.value.mode)
        vm.continueWelcome();runCurrent();assertEquals("María",p.preferences.value.displayName);assertEquals(LocalRole.INTERN,p.preferences.value.role);assertTrue(p.preferences.value.welcomeIntroSeen);assertTrue(vm.sessionHandled)
    }
    @Test fun failedRegistrationCanRetryWithoutReplacingPreviousValues():Unit=runTest {
        val p=Prefs(UserPreferences(displayName="Dato anterior"));p.fail=true
        val vm=WelcomeFlowModel(p,profiles(),{});runCurrent();vm.continueWelcome("Ana",LocalRole.STUDENT);runCurrent()
        assertNotNull(vm.state.value.error);assertEquals("Dato anterior",p.preferences.value.displayName);assertEquals(0,p.writes)
        p.fail=false;vm.continueWelcome("Ana",LocalRole.STUDENT);runCurrent();assertEquals("Ana",p.preferences.value.displayName);assertEquals(1,p.writes)
    }
    @Test fun externalPdfDoesNotRegisterOrMarkIntroSeen():Unit=runTest {
        val p=Prefs(UserPreferences());val vm=WelcomeFlowModel(p,profiles(),{},externalLaunch=true);runCurrent();assertEquals(WelcomeMode.NONE,vm.state.value.mode);assertEquals(0,p.writes)
    }
    @Test fun returnTimerBelongsToSessionAndNeverWritesProfile():Unit=runTest {
        val p=Prefs(UserPreferences(onboardingCompleted=true,welcomeIntroSeen=true));val vm=WelcomeFlowModel(p,profiles(),{});runCurrent()
        assertEquals(WelcomeMode.RETURNING,vm.state.value.mode);advanceTimeBy(1300);runCurrent();assertEquals(WelcomeMode.NONE,vm.state.value.mode);assertTrue(vm.sessionHandled);assertEquals(0,p.writes)
        vm.retry();runCurrent();assertEquals(WelcomeMode.NONE,vm.state.value.mode)
    }
    @Test fun talkBackReturnDoesNotAutoDismiss():Unit=runTest {
        val p=Prefs(UserPreferences(onboardingCompleted=true,welcomeIntroSeen=true));val vm=WelcomeFlowModel(p,profiles(),{},autoDismissReturn=false);runCurrent();advanceTimeBy(3000);runCurrent();assertEquals(WelcomeMode.RETURNING,vm.state.value.mode);vm.continueWelcome();runCurrent();assertEquals(WelcomeMode.NONE,vm.state.value.mode)
    }
    @Test fun startupFailureAllowsReadOnlySessionWithoutClearingPreferences():Unit=runTest {
        val p=Prefs(UserPreferences(displayName="Conservar"));val vm=WelcomeFlowModel(p,profiles(),{throw java.io.IOException("fixture")});runCurrent();assertTrue(vm.state.value.startupError);assertFalse(vm.state.value.loading);vm.dismissForSession();assertEquals("Conservar",p.preferences.value.displayName);assertEquals(0,p.writes)
    }
    @Test fun restoredSameProcessSessionDoesNotReplayWelcome():Unit=runTest {
        val p=Prefs(UserPreferences(onboardingCompleted=true));val vm=WelcomeFlowModel(p,profiles(),{},initiallyHandled=true);runCurrent();assertEquals(WelcomeMode.NONE,vm.state.value.mode);assertEquals(0,p.writes)
    }
    @Test fun pdfArrivingDuringRegistrationDismissesWithoutWriting():Unit=runTest {
        val p=Prefs(UserPreferences());val vm=WelcomeFlowModel(p,profiles(),{});runCurrent();assertEquals(WelcomeMode.REGISTER,vm.state.value.mode);vm.externalContentRequested();assertEquals(WelcomeMode.NONE,vm.state.value.mode);assertEquals(0,p.writes)
    }
    @Test fun failedPreferencesCannotOverlayAnExternalPdf():Unit=runTest {
        val p=Prefs(UserPreferences());val vm=WelcomeFlowModel(p,profiles(),{throw java.io.IOException("fixture")},externalLaunch=true);runCurrent();assertFalse(vm.state.value.loading);assertFalse(vm.state.value.startupError);assertEquals(WelcomeMode.NONE,vm.state.value.mode)
    }
}
