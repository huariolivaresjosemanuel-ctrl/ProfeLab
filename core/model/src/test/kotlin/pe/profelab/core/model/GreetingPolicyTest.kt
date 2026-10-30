package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test

class GreetingPolicyTest {
    @Test fun freshInstallUsesRegistration(){assertEquals(WelcomeMode.REGISTER,GreetingPolicy.mode(UserPreferences(),false,false,false))}
    @Test fun migratedProfileNeverRequiresRegistration(){assertEquals(WelcomeMode.EXISTING_INTRO,GreetingPolicy.mode(UserPreferences(),true,false,false))}
    @Test fun registrationIsNotTheIntroduction(){assertEquals(WelcomeMode.EXISTING_INTRO,GreetingPolicy.mode(UserPreferences(onboardingCompleted=true),false,false,false))}
    @Test fun seenIntroductionUsesReturnGreeting(){assertEquals(WelcomeMode.RETURNING,GreetingPolicy.mode(UserPreferences(onboardingCompleted=true,welcomeIntroSeen=true),true,false,false))}
    @Test fun externalContentAndHandledSessionBypassWelcome(){assertEquals(WelcomeMode.NONE,GreetingPolicy.mode(UserPreferences(),false,false,true));assertEquals(WelcomeMode.NONE,GreetingPolicy.mode(UserPreferences(),false,true,false))}
    @Test fun preferencesHideReturnWithoutErasingRegistration(){assertEquals(WelcomeMode.NONE,GreetingPolicy.mode(UserPreferences(onboardingCompleted=true,mascotVisible=false),true,false,false));assertEquals(WelcomeMode.NONE,GreetingPolicy.mode(UserPreferences(onboardingCompleted=true,welcomeIntroSeen=true,personalizedGreetings=false),true,false,false))}
    @Test fun roleNamesAndOptOutNeverInventTeacherOrNull(){
        for(role in LocalRole.entries){val text=GreetingPolicy.message(9,UserPreferences(displayName="Ana",role=role));assertTrue(text.contains("Ana"));if(role==LocalRole.STUDENT)assertTrue(text.contains("futuro docente"));if(role==LocalRole.INTERN)assertTrue(text.contains("practicante"))}
        assertFalse(GreetingPolicy.message(22,UserPreferences(role=LocalRole.TEACHER)).contains("docente"))
        assertFalse(GreetingPolicy.message(9,UserPreferences(displayName="Ana",personalizedGreetings=false)).contains("Ana"))
        assertFalse(GreetingPolicy.message(9,UserPreferences()).contains("null"))
    }
}
