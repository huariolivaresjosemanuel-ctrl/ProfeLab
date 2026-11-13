package pe.profelab.app

import org.junit.Assert.*
import org.junit.Test
import pe.profelab.core.model.AssistantExperience
import pe.profelab.core.model.UserPreferences
import pe.profelab.core.ui.AssistantMascotController
import pe.profelab.core.ui.MascotGesture

class AssistantExperienceTest {
    @Test fun greetingUsesTheProvidedDeviceHourWithoutInventingPersonalFacts(){
        assertTrue(AssistantExperience.tallerGreeting(6).contains("Buenos días"))
        assertTrue(AssistantExperience.tallerGreeting(14).contains("Buenas tardes"))
        assertTrue(AssistantExperience.tallerGreeting(22).contains("Buenas noches"))
    }
    @Test fun completedOrDisabledWelcomeNeverReopensInOtherContexts(){
        val first=UserPreferences()
        assertTrue(AssistantExperience.showTallerWelcome("taller",first))
        assertFalse(AssistantExperience.showTallerWelcome("home",first))
        assertFalse(AssistantExperience.showTallerWelcome("taller",first.copy(tallerWelcomeSeen=true)))
        assertFalse(AssistantExperience.showTallerWelcome("taller",first.copy(mascotVisible=false)))
        assertFalse(AssistantExperience.showTallerWelcome("taller",first.copy(personalizedGreetings=false)))
    }
    @Test fun unsupportedMascotGesturesKeepTheCanonicalStaticPresentation(){
        val controller=AssistantMascotController()
        controller.greet();assertEquals(MascotGesture.WAVE,controller.state);assertEquals(MascotGesture.IDLE,controller.displayedGesture())
        controller.explain();assertEquals(MascotGesture.IDLE,controller.displayedGesture())
        controller.celebrate();assertEquals(MascotGesture.IDLE,controller.displayedGesture())
        controller.rest();assertEquals(MascotGesture.IDLE,controller.state)
    }
}
