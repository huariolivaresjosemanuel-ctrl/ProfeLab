package pe.profelab.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.feature.taller.ActivityRunData

/** Executes production cloning rules against Android's actual org.json implementation. */
@RunWith(AndroidJUnit4::class)
class Workshop30RunIsolationTest {
    @Test fun newExecutionRemovesPrivateEvidenceButKeepsEditorialImagesAndTheOriginal(){
        val source=JSONObject().put("activityId","pack:activity").put("contentVersion","3.0.0")
            .put("imageFileIds",JSONArray(listOf("editorial-01","photo-private")))
            .put("imageMetadataByFileId",JSONObject().put("editorial-01",JSONObject().put("alt","Imagen editorial")).put("photo-private",JSONObject().put("alt","Evidencia privada")))
            .put("attachments",JSONArray().put(JSONObject().put("id","photo-private").put("mime","image/jpeg")))
            .put("records",JSONArray().put(JSONObject().put("id","observation-before").put("photoAttachmentIds",JSONArray(listOf("photo-private")))))
            .put("steps",JSONArray().put(JSONObject().put("id","step-one").put("title","Conservar instrucción").put("done",true).put("notes","Nota privada")
                .put("imageFileIds",JSONArray(listOf("photo-private","editorial-01")))))
            .put("workshopFlow",JSONObject().put("lastVisitedStepId","step-one").put("observedCriteria",JSONArray(listOf("criterion-1"))).put("teacherReflection","Reflexión personal"))
            .put("classroomAssociation",JSONObject().put("groupId","group-before")).put("studentId","student-before").put("groupId","group-before")
            .put("teacherGuide",JSONObject().put("version",1).put("introShort","Guion reutilizable"))
            .put("privateExtension",JSONObject().put("keep",true))
            .put("safetyReviewedByUser",true).put("exportImages",true).put("status","COMPLETADO")
        listOf("results","analysis","limitations","conclusion","hypothesis").forEach {source.put(it,"Valor personal anterior")}
        val original=source.toString();val fresh=ActivityRunData.freshPayload(source)
        assertEquals(original,source.toString())
        assertEquals(listOf("editorial-01"),strings(fresh.getJSONArray("imageFileIds")))
        assertFalse(fresh.getJSONObject("imageMetadataByFileId").has("photo-private"));assertTrue(fresh.getJSONObject("imageMetadataByFileId").has("editorial-01"))
        val step=fresh.getJSONArray("steps").getJSONObject(0)
        assertEquals(listOf("editorial-01"),strings(step.getJSONArray("imageFileIds")));assertFalse(step.getBoolean("done"));assertFalse(step.has("notes"));assertEquals("Conservar instrucción",step.getString("title"))
        listOf("workshopFlow","classroomAssociation","studentId","groupId").forEach {assertFalse(it,fresh.has(it))}
        listOf("results","analysis","limitations","conclusion","hypothesis").forEach {assertEquals(it,"",fresh.getString(it))}
        assertEquals(0,fresh.getJSONArray("records").length());assertEquals(0,fresh.getJSONArray("attachments").length())
        assertFalse(fresh.getBoolean("safetyReviewedByUser"));assertFalse(fresh.getBoolean("exportImages"));assertEquals("BORRADOR",fresh.getString("status"))
        assertEquals(source.getJSONObject("teacherGuide").toString(),fresh.getJSONObject("teacherGuide").toString())
        assertEquals(source.getJSONObject("privateExtension").toString(),fresh.getJSONObject("privateExtension").toString());assertEquals("3.0.0",fresh.getString("contentVersion"))
    }

    @Test fun explicitCreationReturnsToTheCurrentWindowWhileNewAndDuplicateOpenEditor(){
        listOf("steps","results","classroom").forEach {window->
            assertEquals(window,ActivityRunData.initialDestination(true,false,window))
            assertEquals("editor",ActivityRunData.initialDestination(true,true,window))
            assertEquals("editor",ActivityRunData.initialDestination(false,false,window))
        }
        listOf("detail","materials","purpose","guide").forEach {assertEquals("materials",ActivityRunData.initialDestination(true,false,it))}
    }

    private fun strings(array:JSONArray)=(0 until array.length()).map {array.getString(it)}
}
