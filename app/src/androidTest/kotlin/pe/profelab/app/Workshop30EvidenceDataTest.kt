package pe.profelab.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.WorkshopEvidenceData

@RunWith(AndroidJUnit4::class)
class Workshop30EvidenceDataTest {
    @Test fun editorialCaptionNeverBecomesPrivateEvidence(){
        val data=JSONObject().put("imageFileIds",JSONArray(listOf("editorial-photo")))
            .put("imageMetadataByFileId",JSONObject().put("editorial-photo",JSONObject().put("alt","Fotografía editorial del procedimiento")))
            .put("attachments",JSONArray().put(JSONObject().put("id","private-photo").put("mime","image/webp")).put(JSONObject().put("id","document").put("mime","application/pdf")))
        assertEquals(listOf("private-photo"),WorkshopEvidenceData.privateImageIds(data))
    }
    @Test fun stableObservationDeletionDoesNotDeleteAnotherRowAfterReordering(){
        val a=JSONObject().put("id","a").put("observation","Primera propia")
        val b=JSONObject().put("id","b").put("observation","Segunda propia")
        val before=JSONArray().put(a).put(b);val reordered=JSONArray().put(b).put(a)
        val after=WorkshopEvidenceData.removeObservation(reordered,"a",a.toString(),before.toString(),0)
        assertEquals(1,after.length());assertEquals("b",after.getJSONObject(0).getString("id"));assertEquals(2,reordered.length())
    }
    @Test fun changedLegacyArrayRefusesIndexBasedDeletion(){
        val original=JSONArray().put(JSONObject().put("observation","Sin ID"))
        val changed=JSONArray().put(JSONObject().put("observation","Otra observación")).put(original.get(0))
        var rejected=false
        try{WorkshopEvidenceData.removeObservation(changed,null,original.get(0).toString(),original.toString(),0)}catch(_:IllegalArgumentException){rejected=true}
        assertTrue(rejected);assertEquals(2,changed.length())
    }
    @Test fun measurementHasBoundedPrecisionWithoutGuessingAUnit(){
        assertTrue(WorkshopEvidenceData.validMeasurement("1,25","cm"));assertTrue(WorkshopEvidenceData.validMeasurement("-2","°C"))
        assertTrue(WorkshopEvidenceData.validMeasurement("",""));assertFalse(WorkshopEvidenceData.validMeasurement("2",""))
        assertFalse(WorkshopEvidenceData.validMeasurement("1e99999","cm"));assertFalse(WorkshopEvidenceData.validMeasurement("9".repeat(100),"cm"))
        assertFalse(WorkshopEvidenceData.validMeasurement("0.000000001","cm"));assertFalse(WorkshopEvidenceData.validMeasurement("-+2","cm"))
    }
}
