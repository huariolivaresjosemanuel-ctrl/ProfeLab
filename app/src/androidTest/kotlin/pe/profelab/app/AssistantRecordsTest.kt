package pe.profelab.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.WorkRecord

@RunWith(AndroidJUnit4::class)
class AssistantRecordsTest {
    @Test fun reviewedSessionsAndCompletedProjectsAreNotAdvertisedAsPending(){
        val reviewed=WorkRecord("session","session","Revisada","{\"status\":\"REVISADA\"}",2)
        val complete=WorkRecord("project","project","Listo","{\"status\":\"COMPLETADO\"}",3)
        assertNull(AssistantRecords.pendingWork(listOf(reviewed,complete)))
        assertTrue(AssistantRecords.facts(listOf(reviewed,complete),false).hasProjects)
        assertTrue(AssistantRecords.facts(listOf(complete.copy(archived=true)),false).hasProjects)
        assertNull(AssistantRecords.pendingWork(listOf(complete.copy(archived=true))))
    }
    @Test fun latestKnownDraftIsSelectedWithoutChangingAnyRecord(){
        val a=WorkRecord("a","session","Primera","{\"status\":\"BORRADOR\"}",1)
        val b=WorkRecord("b","project","Segunda","{\"status\":\"EN_PROGRESO\"}",2)
        val all=listOf(a,b);assertEquals("b",AssistantRecords.pendingWork(all)?.id);assertEquals("{\"status\":\"BORRADOR\"}",all.first().json)
    }
}
