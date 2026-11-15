package pe.profelab.core.database

import org.junit.Assert.*
import org.junit.Test

class EducationRecordLimitsTest {
    @Test fun boundedCiphertextRowFitsAndLargerOneIsRejected(){
        EducationRecordLimits.requireFits("id","edu_phrase","Private","x".repeat(1_800_000))
        assertTrue(runCatching {EducationRecordLimits.requireFits("id","edu_phrase","Private","x".repeat(1_900_000))}.isFailure)
    }
    @Test fun byteBudgetCountsUtf8TitleAndIdentifiersRatherThanJustJsonCharacters(){
        val text="界".repeat(640_000)
        assertTrue(text.length<EducationRecordLimits.maximumStoredBytes)
        assertTrue(runCatching {EducationRecordLimits.requireFits("id","edu_phrase","Private",text)}.isFailure)
        assertTrue(runCatching {EducationRecordLimits.requireFits("id","edu_phrase","界".repeat(640_000),"{}")}.isFailure)
    }
}
