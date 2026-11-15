package pe.profelab.core.model.education

import org.junit.Assert.*
import org.junit.Test

class EducationAssessmentPolicyTest {
    @Test fun originalScalesAreValidatedIndependentlyWithoutConversion(){
        listOf("AD","A","B","C").forEach {assertTrue(EducationAssessmentPolicy.validOriginalValue("AD_A_B_C",it))}
        assertFalse(EducationAssessmentPolicy.validOriginalValue("AD_A_B_C","18"));assertFalse(EducationAssessmentPolicy.validOriginalValue("AD_A_B_C","NOT_OBSERVED"))
        listOf("0","20","15.50","15,5").forEach {assertTrue(EducationAssessmentPolicy.validOriginalValue("NUMERIC_0_20",it))}
        listOf("20.1","-1","NaN","1e1","A").forEach {assertFalse(EducationAssessmentPolicy.validOriginalValue("NUMERIC_0_20",it))}
        assertTrue(EducationAssessmentPolicy.validOriginalValue("DESCRIPTIVE","Explica su estrategia con apoyo visual"))
        assertFalse(EducationAssessmentPolicy.validOriginalValue("RUBRIC",""))
    }
    @Test fun unrelatedPeriodsSubjectsCriteriaAndScalesAreNotComparable(){
        fun comparable(student:String="s",subject:String="math",competency:String="c",criterion:String="q",period:String="P1",scale:String="AD_A_B_C")=EducationAssessmentPolicy.comparable("s","math","c","q","P1","AD_A_B_C",student,subject,competency,criterion,period,scale)
        assertTrue(comparable());assertFalse(comparable(student="other"));assertFalse(comparable(subject="language"));assertFalse(comparable(competency="other"));assertFalse(comparable(criterion="other"));assertFalse(comparable(period="P2"));assertFalse(comparable(scale="NUMERIC_0_20"))
    }
}
