package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test

class AssistantSessionGateTest {
    @Test fun tabChangesAndRecreationSharingTheProcessDoNotRepeatGreeting(){
        val session=AssistantSessionGate()
        assertFalse(session.isClaimed("greeting"))
        assertTrue(session.claim("greeting"))
        assertTrue(session.isClaimed("greeting"))
        repeat(5){assertFalse(session.claim("greeting"))}
        assertTrue(AssistantSessionGate().claim("greeting"))
        val returning=AssistantSessionGate();returning.welcome()
        assertTrue(returning.providedByWelcome);assertFalse(returning.claim("greeting"))
    }
    @Test fun executionsAreIndependentButNormalStepsCannotTriggerPopups(){
        val session=AssistantSessionGate()
        assertTrue(session.claim("preparation","run-a"));assertFalse(session.claim("preparation","run-a"))
        assertTrue(session.claim("preparation","run-b"));assertTrue(session.claim("evaluation","run-a"))
        try{session.claim("step","run-a");fail("No automatic step event is allowed")}catch(_:IllegalArgumentException){}
    }
    @Test fun failureNeverCelebratesAndRetrySuccessIsClaimedOnlyOnce(){
        val session=AssistantSessionGate()
        assertFalse(session.saved("save-1",false));assertTrue(session.saved("save-1",true))
        assertFalse(session.saved("save-1",true));assertTrue(session.saved("save-2",true))
    }
}
