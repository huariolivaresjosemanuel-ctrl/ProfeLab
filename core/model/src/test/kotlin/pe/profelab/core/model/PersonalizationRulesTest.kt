package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test

class PersonalizationRulesTest {
    @Test fun rolesHaveDistinctGreetingsAndDoNotTreatEveryoneAsTeacher(){
        val teacher=PersonalizationRules.greeting(10,UserPreferences(displayName="María",role=LocalRole.TEACHER))
        val student=PersonalizationRules.greeting(10,UserPreferences(displayName="María",role=LocalRole.STUDENT))
        val intern=PersonalizationRules.greeting(10,UserPreferences(displayName="María",role=LocalRole.INTERN))
        assertTrue(teacher.contains("docente María"));assertTrue(student.contains("futuro docente María"));assertTrue(intern.contains("practicante María"))
        assertEquals(3,setOf(teacher,student,intern).size)
    }
    @Test fun blankNameKeepsGreetingNeutralAndDoesNotPrintNull(){
        val text=PersonalizationRules.greeting(21,UserPreferences(role=LocalRole.TEACHER))
        assertFalse(text.contains("docente"));assertFalse(text.contains("null"));assertTrue(text.startsWith(HomeRules.greeting(21)))
    }
    @Test fun greetingOptOutDoesNotExposeEitherStoredName(){
        val text=PersonalizationRules.greeting(8,UserPreferences(displayName="Personal",personalizedGreetings=false),"Anterior")
        assertFalse(text.contains("Personal"));assertFalse(text.contains("Anterior"));assertEquals(HomeRules.greeting(8),text)
    }
    @Test fun legacyNameAndAccentsArePreservedWithoutControlCharacters(){
        val text=PersonalizationRules.greeting(15,UserPreferences(),"  José\n Manuel  ")
        assertTrue(text.contains("José Manuel"));assertFalse(text.contains('\n'));assertEquals(60,PersonalizationRules.name("a".repeat(80)).length)
    }
}
