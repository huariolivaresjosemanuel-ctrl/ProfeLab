package pe.profelab.core.model.education

import java.math.BigDecimal

/** Validates the teacher's original scale without converting, averaging, ranking or predicting. */
object EducationAssessmentPolicy {
    fun validOriginalValue(scale:String,value:String):Boolean = when(scale){
        "AD_A_B_C" -> value in setOf("AD","A","B","C")
        "NUMERIC_0_20" -> value.matches(Regex("[0-9]{1,2}(?:[.,][0-9]{1,4})?")) && runCatching {val n=BigDecimal(value.replace(',','.'));n>=BigDecimal.ZERO && n<=BigDecimal("20")}.getOrDefault(false)
        "DESCRIPTIVE","RUBRIC" -> value.isNotBlank() && value.length<=2000
        else -> false
    }
    fun comparable(studentA:String,subjectA:String,competencyA:String,criterionA:String,periodA:String,scaleA:String,studentB:String,subjectB:String,competencyB:String,criterionB:String,periodB:String,scaleB:String):Boolean = studentA==studentB && subjectA==subjectB && competencyA==competencyB && criterionA==criterionB && periodA==periodB && scaleA==scaleB
}
