package pe.profelab.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.*
import pe.profelab.core.database.ProfeDatabase
import pe.profelab.core.database.WorkspaceRepository
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.newId
import pe.profelab.feature.taller.ActivityRunData
import pe.profelab.feature.taller.WorkshopViewModel

@RunWith(AndroidJUnit4::class)
class WorkshopPreparationTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private suspend fun personal():WorkRecord {
        val template=WorkshopSeed(context).templates().single {it.id=="mat-001"}
        return template.copy(id=newId(),kind="project",json=ActivityRunData.freshPayload(JSONObject(template.json)).put("activityId",template.id).put("forkedFromTemplateId",template.id).toString())
    }
    @Test fun participantSelectionNeverMultipliesMaterialsWithoutAnExplicitRule():Unit=runBlocking {
        val work=personal();val before=work.json;val selected=WorkshopPreparation.participation(work,"STUDENTS",24,6)
        assertEquals(JSONObject(before).getJSONArray("materials").toString(),selected.getJSONArray("materials").toString())
        assertEquals(24,selected.getJSONObject("teacherPreparation").getInt("participantCount"));assertEquals(before,work.json)
        assertTrue(runCatching {WorkshopPreparation.participation(work.copy(kind="project_template"),"STUDENTS",24,6)}.isFailure)
    }
    @Test fun confirmedRulesUseTheirBaseQuantityOnceAndCanRestoreTheOriginal():Unit=runBlocking {
        val source=personal();val catalog=WorkshopSeed(context).materials()
        val prepared=source.copy(json=WorkshopPreparation.participation(source,"STUDENTS",12,3).toString())
        val line=WorkshopCodec.materials(JSONObject(prepared.json),catalog).first()
        assertTrue(runCatching {WorkshopPreparation.materialRule(prepared,catalog,line.id,"STUDENT","2",false)}.isFailure)
        val first=prepared.copy(json=WorkshopPreparation.materialRule(prepared,catalog,line.id,"STUDENT","2",true).toString())
        val again=first.copy(json=WorkshopPreparation.materialRule(first,catalog,line.id,"STUDENT","2",true).toString())
        assertEquals("24",WorkshopCodec.materials(JSONObject(first.json),catalog).first().amount)
        assertEquals("24",WorkshopCodec.materials(JSONObject(again.json),catalog).first().amount)
        val original=WorkshopPreparation.materialRule(again,catalog,line.id,"ORIGINAL","",true)
        assertEquals(line.amount,WorkshopCodec.materials(original,catalog).first().amount)
    }
    @Test fun aLateWorkshopEditCannotEraseTheNewEducationRunPointer():Unit=runBlocking {
        val database=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(database);val holder=ViewModelStore()
        val vm=withContext(Dispatchers.Main){ViewModelProvider(holder,object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun <T:ViewModel> create(modelClass:Class<T>):T=WorkshopViewModel(store) as T})[WorkshopViewModel::class.java]}
        try {
            val stale=personal();store.saveRecord(stale)
            val education=EducationRepository(store);val runId=education.createRun(stale,null,emptySet(),emptySet(),12)
            withContext(Dispatchers.Main){vm.patch(stale){j->
                j.put("results","Edición docente conservada");j.getJSONArray("steps").getJSONObject(0).put("notes","Nota conservada")
                j.getJSONArray("materials").getJSONObject(0).put("amount","3")
            }}
            assertTrue(vm.flush())
            val stored=requireNotNull(store.record(stale.id));val data=JSONObject(stored.json)
            assertEquals(runId,data.getJSONObject("teacherPreparation").getString("educationRunId"))
            assertEquals(12,data.getJSONObject("teacherPreparation").getInt("participantCount"))
            assertEquals("Edición docente conservada",data.getString("results"));assertEquals("Nota conservada",data.getJSONArray("steps").getJSONObject(0).getString("notes"))
            assertEquals("3",data.getJSONArray("materials").getJSONObject(0).getString("amount"))
            assertEquals(1,EducationData.runs(store.observeRecords().first()).size)
            val individual=stored.copy(json=WorkshopPreparation.participation(stored,"INDIVIDUAL",1,1).toString())
            assertNull(EducationData.selectedRun(individual,store.observeRecords().first()))
            database.openHelper.writableDatabase.query("SELECT COUNT(*) FROM inventory_operations").use {c->assertTrue(c.moveToFirst());assertEquals(0,c.getInt(0))}
        }finally{withContext(Dispatchers.Main){holder.clear()};database.close()}
    }
}
