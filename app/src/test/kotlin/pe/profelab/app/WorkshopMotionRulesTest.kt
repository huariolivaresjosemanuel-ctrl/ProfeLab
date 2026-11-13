package pe.profelab.app

import org.junit.Assert.*
import org.junit.Test
import pe.profelab.feature.taller.WorkshopMotionRules

/** Accessibility policy can be tested on the JVM without changing device-wide settings. */
class WorkshopMotionRulesTest {
    @Test fun eitherDisabledSystemScaleOrTouchExplorationDisablesMotion(){
        assertTrue(WorkshopMotionRules.allowed(1f,1f,false))
        for(scale in listOf(0f,-1f,Float.NaN)){
            assertFalse(WorkshopMotionRules.allowed(scale,1f,false))
            assertFalse(WorkshopMotionRules.allowed(1f,scale,false))
        }
        assertFalse(WorkshopMotionRules.allowed(1f,1f,true))
    }

    @Test fun backwardNavigationReversesDirectionAndResumeKeepsItNeutral(){
        assertEquals(1,WorkshopMotionRules.stepDirection(0,3))
        assertEquals(-1,WorkshopMotionRules.stepDirection(3,0))
        assertEquals(0,WorkshopMotionRules.stepDirection(2,2))
    }
}
