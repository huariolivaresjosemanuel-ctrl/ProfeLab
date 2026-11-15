package pe.profelab.core.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EducationBackupReferencesTest {
    private fun data(evidenceId:String):JSONObject {
        val observation=JSONObject().put("kind","edu_observation").put("json",JSONObject().put("evidenceIds",JSONArray()).toString())
        val snapshot=JSONObject().put("id","synthetic-snapshot").put("evidenceIds",JSONArray().put(evidenceId))
        val judgment=JSONObject().put("kind","edu_judgment").put("json",JSONObject().put("observationSnapshots",JSONArray().put(snapshot)).toString())
        return JSONObject().put("workspace_records",JSONArray().put(observation).put(judgment))
    }
    @Test fun judgmentSnapshotRetainsPhotoAfterTheEditableObservationRemovesItsLink(){
        assertEquals(setOf("synthetic-evidence-photo"),BackupReferences.ids(data("synthetic-evidence-photo")))
    }
    @Test fun snapshotCannotIntroduceAnExternalOrTraversingFileReference(){
        assertTrue(runCatching {BackupReferences.ids(data("../other-photo"))}.isFailure)
        assertTrue(runCatching {BackupReferences.ids(data("https://example.invalid/photo"))}.isFailure)
    }
}
