package pe.profelab.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import pe.profelab.core.database.*
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class WorkshopFlowContractTest {
    private fun record()=WorkRecord("my-run","project","Mi actividad",JSONObject().put("activityId","mat-003").put("contentVersion","2.0.0").put("privateNote","conservar")
        .put("steps",JSONArray().put(JSONObject().put("id","first").put("title","Primero").put("done",false)).put(JSONObject().put("id","second").put("title","Segundo").put("done",false))).toString(),1)
    @Test fun visitDoesNotCompleteOrMutateItsSource(){val r=record();val before=r.json;val next=r.copy(json=WorkshopFlowAdapter.visit(r,"second").toString());assertEquals(1,WorkshopFlowAdapter.read(next).resumeIndex);assertEquals(0,WorkshopFlowAdapter.read(next).completedSteps);assertEquals(before,r.json);assertEquals("conservar",JSONObject(next.json).getString("privateNote"))}
    @Test fun stableStepSurvivesReordering(){val r=record().let {it.copy(json=WorkshopFlowAdapter.mark(it,"second",true).toString())};val j=WorkshopFlowAdapter.visit(r,"second");val a=j.getJSONArray("steps");j.put("steps",JSONArray().put(a.get(1)).put(a.get(0)));val v=WorkshopFlowAdapter.read(r.copy(json=j.toString()));assertEquals("second",v.steps[v.resumeIndex].id);assertTrue(v.steps[0].done);assertFalse(v.steps[1].done)}
    @Test fun openingTemplatesCannotCreateAVisitOrProgress(){val template=record().copy(kind="project_template");assertNull(WorkshopFlowAdapter.read(template).reference().activityRunId);var rejected=false;try{WorkshopFlowAdapter.visit(template,"first")}catch(_:IllegalArgumentException){rejected=true};assertTrue(rejected)}
    @Test fun criteriaIdsSurviveReordering(){val r=record();val guide=TeacherGuides.resolve(r).put("formativeEvidence",JSONArray(listOf("Observa", "Explica")));val a=r.copy(json=JSONObject(r.json).put("teacherGuide",guide).toString());val ids=WorkshopFlowAdapter.read(a).criteria.associate {it.label to it.id};guide.put("formativeEvidence",JSONArray(listOf("Explica","Observa")));assertEquals(ids,WorkshopFlowAdapter.read(r.copy(json=JSONObject(r.json).put("teacherGuide",guide).toString())).criteria.associate {it.label to it.id})}
    @Test fun legacyStepPayloadRemainsReadableWithoutMigration(){val r=record().copy(json="{\"steps\":[\"Uno\",{\"title\":\"Dos\",\"done\":true}]}");val v=WorkshopFlowAdapter.read(r);assertEquals(listOf("step-0","step-1"),v.steps.map {it.id});assertEquals(1,v.completedSteps);assertEquals("legacy",v.contentVersion)}
    @Test fun existingCulturalCategoriesStayReachable(){assertEquals("dance",WorkshopFlowAdapter.sector(record().copy(json="{\"categoryId\":\"dance\"}")));assertEquals("heritage",WorkshopFlowAdapter.sector(record().copy(json="{\"categoryId\":\"heritage\"}")));assertEquals("taller",WorkshopFlowAdapter.sector(record()))}
    @Test fun publicContractCannotContainLocalRunOrStudentAssociations(){
        for(key in listOf("workshopFlow","studentId","groupId","classroomAssociation")){
            val j=JSONObject().put("id","public-activity").put("activityContractVersion",1).put(key,"forbidden-local-reference")
            var rejected=false;try{WorkshopFlowAdapter.validateExtension(j)}catch(_:IllegalArgumentException){rejected=true};assertTrue(key,rejected)
        }
    }
    @Test fun contentExtensionRejectsDuplicateCriteriaAndUnsupportedVersions(){
        val j=JSONObject().put("id","public-activity").put("activityContractVersion",1).put("observationCriteria",JSONArray().put(JSONObject().put("criterionId","stable").put("label","Observar")).put(JSONObject().put("criterionId","stable").put("label","Explicar")))
        var rejected=false;try{WorkshopFlowAdapter.validateExtension(j)}catch(_:IllegalArgumentException){rejected=true};assertTrue(rejected)
        j.remove("observationCriteria");j.put("activityContractVersion",99);rejected=false;try{WorkshopFlowAdapter.validateExtension(j)}catch(_:IllegalArgumentException){rejected=true};assertTrue(rejected)
    }
    @Test fun shoppingTextUsesTheExactCompatibleMissingAmount(){val need=Supply("Ligas",java.math.BigDecimal("10"),"unidad");val coverage=InventoryAvailability.evaluate(listOf(need),listOf(Stock("Ligas",java.math.BigDecimal("14"),java.math.BigDecimal("10"),"unidad")));val text=WorkshopFlowAdapter.shoppingText(record(),WorkshopAssessment(coverage,SafetyStatus.PENDING));assertTrue(text.contains("Ligas — 6 unidad"))}
    @Test fun preparedProgressReflectionAndStockSurviveReopen():Unit=runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="qa-flow-${newId()}.db";var db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build()
        try {
            val store=WorkspaceRepository(db);val r=record();val j=WorkshopFlowAdapter.mark(r,"first",true);val marked=r.copy(json=j.toString());val visited=WorkshopFlowAdapter.visit(marked,"second");visited.getJSONObject("workshopFlow").put("teacherReflection","Conservar mi reflexión").put("preparedMaterialIds",JSONArray(listOf("ligas")))
            store.saveRecord(marked.copy(json=visited.toString()));store.saveRecord(WorkRecord("stock","inventory","Ligas","{\"quantity\":\"14\",\"reserved\":\"0\",\"unit\":\"unidad\",\"materialKind\":\"CONSUMABLE\"}",1));db.close()
            db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build();val saved=requireNotNull(WorkspaceRepository(db).record(r.id));val view=WorkshopFlowAdapter.read(saved)
            assertEquals("second",view.lastVisitedStepId);assertEquals(1,view.completedSteps);assertEquals("Conservar mi reflexión",JSONObject(saved.json).getJSONObject("workshopFlow").getString("teacherReflection"));assertEquals("14",JSONObject(requireNotNull(WorkspaceRepository(db).record("stock")).json).getString("quantity"))
        }finally{db.close();context.deleteDatabase(name)}
    }
}
