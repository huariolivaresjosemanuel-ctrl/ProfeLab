package pe.profelab.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class UxJourneyDataTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private suspend fun personal():WorkRecord {
        val source=WorkshopSeed(context).templates().single {it.id=="mat-001"}
        return source.copy(id=newId(),kind="project",json=JSONObject(source.json).put("forkedFromTemplateId",source.id).put("status","BORRADOR").toString())
    }
    @Test fun slashFingerprintMatchesTheApprovedExp005SlotAndStillRejectsChangedText():Unit=runBlocking {
        val source=WorkshopSeed(context).templates().single {it.id=="exp-005"}
        val step=WorkshopFlowAdapter.read(source).steps.first();assertTrue(step.title.contains('/'))
        val row=context.assets.open("workshop/media_slots_v1.json").bufferedReader().use {JSONObject(it.readText()).getJSONArray("resources")}.let {a->(0 until a.length()).map {a.getJSONObject(it)}.single {it.getString("contentId")=="exp-005" && it.optString("stepId")==step.id}}
        assertEquals(row.getString("stepFingerprint"),WorkshopMediaBindings.stepFingerprint(step))
        assertNotNull(WorkshopMediaBindings.resolve(source,"step_photo",step.id))
        assertNotEquals(row.getString("stepFingerprint"),WorkshopMediaBindings.stepFingerprint(step.copy(title=step.title+" Cambiado")))
    }
    @Test fun generalObservationPreservesHistoryUneditedReflectionAndAllOtherActivityFields():Unit=runBlocking {
        val original=personal();val criterion=WorkshopFlowAdapter.read(original).criteria.first()
        val data=JSONObject(original.json).put("workshopFlow",JSONObject().put("observedCriterionIds",JSONArray().put("historical-criterion")).put("teacherReflection","Nota concurrente").put("lastVisitedStepId","step-2"))
        val row=original.copy(json=data.toString());val changed=WorkshopFormativeData.update(row,setOf(criterion.id),setOf(criterion.id),null)
        assertEquals("Nota concurrente",changed.getJSONObject("workshopFlow").getString("teacherReflection"))
        assertEquals("step-2",changed.getJSONObject("workshopFlow").getString("lastVisitedStepId"))
        assertTrue(WorkshopCodec.strings(changed.getJSONObject("workshopFlow").get("observedCriterionIds")).containsAll(listOf("historical-criterion",criterion.id)))
        data.keys().asSequence().filter {it!="workshopFlow"}.forEach {assertEquals(it,data.get(it).toString(),changed.get(it).toString())}
        val noSelection=JSONObject(row.json).put("teacherPreparation",JSONObject().put("participation","STUDENTS").put("educationRunId",newId()))
        try{WorkshopFormativeData.update(row.copy(json=noSelection.toString()),setOf(criterion.id),emptySet(),null);fail("No general write into a classroom execution")}catch(_:IllegalArgumentException){}
    }
    @Test fun closureIsIdempotentDoesNotMarkStepsAndKeepsExecutionsSeparate():Unit=runBlocking {
        val row=personal();val operation=newId();val once=WorkshopClosure.finish(row,operation,100)
        assertEquals(once,WorkshopClosure.finish(once,operation,200));assertEquals(100L,WorkshopClosure.savedAt(once))
        assertEquals(JSONObject(row.json).getJSONArray("steps").toString(),JSONObject(once.json).getJSONArray("steps").toString())
        assertEquals(JSONObject(row.json).optString("status"),JSONObject(once.json).optString("status"))
        val prep=JSONObject().put("participation","STUDENTS").put("educationRunId",newId())
        val other=once.copy(json=JSONObject(once.json).put("teacherPreparation",prep).toString());assertEquals(0L,WorkshopClosure.savedAt(other))
        val finished=WorkshopClosure.finish(other,newId(),300);assertEquals(300L,WorkshopClosure.savedAt(finished))
        val back=finished.copy(json=JSONObject(finished.json).put("teacherPreparation",JSONObject().put("participation","INDIVIDUAL")).toString());assertEquals(100L,WorkshopClosure.savedAt(back))
    }
    @Test fun failedClosureWriteNeverReturnsSuccessOrCreatesAReceipt():Unit=runBlocking {
        val store=ApplicationProvider.getApplicationContext<ProfeApplication>().graph.workspace
        val row=personal();store.saveRecord(row)
        val failing=object:WorkspaceStore by store {
            override suspend fun editRecords(change:(List<WorkRecord>)->List<WorkRecord>){throw java.io.IOException("Fallo de escritura controlado")}
        }
        withContext(Dispatchers.Main){
            val holder=androidx.lifecycle.ViewModelStore()
            val vm=androidx.lifecycle.ViewModelProvider(holder,pe.profelab.core.ui.modelFactory {pe.profelab.feature.taller.WorkshopViewModel(failing)})[pe.profelab.feature.taller.WorkshopViewModel::class.java]
            try{assertFalse(vm.finish(row,newId()));assertNotNull(vm.state.value.error)}finally{holder.clear()}
        }
        assertEquals(0L,WorkshopClosure.savedAt(requireNotNull(store.record(row.id))))
    }
}
