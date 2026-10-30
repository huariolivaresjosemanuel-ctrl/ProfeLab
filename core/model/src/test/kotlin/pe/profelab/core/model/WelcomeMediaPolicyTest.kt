package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test

class WelcomeMediaPolicyTest {
    @Test fun twoVideosBelongToSeparatePhases(){assertEquals(WelcomeClip.ENTRY,WelcomeMediaPolicy.clip(WelcomePhase.ENTRY));assertEquals(WelcomeClip.GREETING,WelcomeMediaPolicy.clip(WelcomePhase.GREETING));assertNull(WelcomeMediaPolicy.clip(WelcomePhase.FORM))}
    @Test fun existingRegistrationUsesGreetingNotARepeatedForm(){assertEquals(WelcomePhase.GREETING,WelcomeMediaPolicy.initial(WelcomeMode.RETURNING));assertEquals(WelcomePhase.NONE,WelcomeMediaPolicy.initial(WelcomeMode.NONE))}
    @Test fun reducedMotionPowerAndSystemSettingsPreventPlayback(){assertFalse(WelcomeMediaPolicy.canPlay(UserPreferences(reduceMotion=true),false,true));assertFalse(WelcomeMediaPolicy.canPlay(UserPreferences(),true,true));assertFalse(WelcomeMediaPolicy.canPlay(UserPreferences(),false,false));assertFalse(WelcomeMediaPolicy.canPlay(UserPreferences(mascotVisible=false),false,true))}
    @Test fun ordinaryVisibleWelcomeCanPlayOffline(){assertTrue(WelcomeMediaPolicy.canPlay(UserPreferences(),false,true));assertFalse(WelcomeClip.ENTRY.asset==WelcomeClip.GREETING.asset)}
}
