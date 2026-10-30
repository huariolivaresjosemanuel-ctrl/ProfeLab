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
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.content.WorkshopSeed
import pe.profelab.core.database.ProfeDatabase
import pe.profelab.core.database.WorkspaceRepository
import pe.profelab.core.model.*
import pe.profelab.feature.taller.*
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class WorkshopPersistenceTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    @Test fun legacyConversionPreservesOriginalAndDoesNotPretendMalformedStock(){
        val old="madera|1,5|tablero|consumible\nclavos|x|unidad|desconocido"
        val data=WorkshopCodec.normalize(JSONObject().put("materials",old).put("privateNote","conservar"),emptyList())
        assertEquals(old,data.getString("legacyMaterialsOriginal"));assertEquals("conservar",data.getString("privateNote"))
        val lines=WorkshopCodec.materials(data,emptyList());assertEquals(2,lines.size);assertEquals("1,5",lines.first().amount);assertNull(lines.last().supply().quantity)
        assertFalse(InventoryRules.assess(lines.map {it.supply()},emptyList()).all {it.covered})
    }
    @Test fun catalogHasTwelvePicturesAndEightUnvalidatedExperiments()=runBlocking {
        val templates=WorkshopSeed(context).templates();assertEquals(12,templates.count {it.kind=="project_template"});assertEquals(8,templates.count {it.kind=="experiment_template"})
        templates.filter {it.kind=="project_template"}.forEach {r->context.assets.open("workshop/images/${r.id}.png").use {assertTrue(it.read()!=-1)}}
        templates.filter {it.kind=="experiment_template"}.forEach {assertEquals(SafetyStatus.PENDING,WorkshopCodec.safety(JSONObject(it.json)))}
    }
    @Test fun selectedProjectAndReceiptAreIdempotentWithPartialPurchase()=runBlocking {
        val database=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val holder=ViewModelStore();val store=WorkspaceRepository(database)
        val vm=withContext(Dispatchers.Main){ViewModelProvider(holder,object:ViewModelProvider.Factory{@Suppress("UNCHECKED_CAST")override fun <T:ViewModel> create(modelClass:Class<T>):T=WorkshopViewModel(store) as T})[WorkshopViewModel::class.java]}
        try {
            val paper=legacyMaterialId("papel");val requirements=JSONArray().put(JSONObject().put("id","paper").put("materialId",paper).put("name","papel").put("amount","5").put("unit","hoja").put("materialKind","CONSUMABLE"))
            val project=WorkRecord(newId(),"project","Creación propia",JSONObject().put("materials",requirements).put("steps",JSONArray().put(JSONObject().put("id","step").put("title","Preparar").put("done",false))).toString(),1)
            store.saveRecord(project)
            withContext(Dispatchers.Main){vm.addShopping(project,emptyList());vm.addShopping(project,emptyList())};assertTrue(vm.flush())
            val rows=store.observeRecords().first()
            val basket=shoppingRecord(rows);assertEquals(1,JSONObject(basket.json).getJSONArray("selections").length())
            val line=shoppingLines(JSONObject(basket.json),rows,emptyList()).single();assertEquals("5",line.amount)
            withContext(Dispatchers.Main){vm.editBasket {it.put("purchased",JSONArray().put(line.key)).put("overrides",JSONObject().put(line.key,"9"))}};assertTrue(vm.flush())
            val selectedRows=store.observeRecords().first();val selectedLine=shoppingLines(JSONObject(shoppingRecord(selectedRows).json),selectedRows,emptyList()).single()
            val receipt=newId();withContext(Dispatchers.Main){vm.acquire(selectedLine,BigDecimal("2"),receipt,emptyList());vm.acquire(selectedLine,BigDecimal("2"),receipt,emptyList())};assertTrue(vm.flush())
            val partial=store.observeRecords().first();val partialBasket=JSONObject(shoppingRecord(partial).json)
            assertEquals("2",JSONObject(partial.single {it.kind=="inventory"}.json).getString("quantity"))
            assertFalse(WorkshopCodec.strings(partialBasket.opt("purchased")).contains(line.key));assertFalse(partialBasket.getJSONObject("overrides").has(line.key))
            val remaining=shoppingLines(partialBasket,partial,emptyList()).single();assertEquals("3",remaining.amount)
            withContext(Dispatchers.Main){vm.editBasket {it.put("purchased",JSONArray().put(remaining.key))}};assertTrue(vm.flush())
            withContext(Dispatchers.Main){vm.acquire(remaining,BigDecimal("3"),newId(),emptyList())};assertTrue(vm.flush())
            val after=store.observeRecords().first();val inventory=after.single {it.kind=="inventory"}
            assertEquals("5",JSONObject(inventory.json).getString("quantity"));assertEquals(2,JSONObject(shoppingRecord(after).json).getJSONArray("receipts").length())
            withContext(Dispatchers.Main){vm.patch(project){j->j.getJSONArray("steps").getJSONObject(0).put("done",true)}};assertTrue(vm.flush())
            assertTrue(JSONObject(requireNotNull(WorkspaceRepository(database).record(project.id)).json).getJSONArray("steps").getJSONObject(0).getBoolean("done"))
            assertTrue(shoppingLines(JSONObject(shoppingRecord(after).json),after,emptyList()).isEmpty())
        }finally{withContext(Dispatchers.Main){holder.clear()};database.close()}
    }
    @Test fun atomicMutationRollsBackAllRowsOnValidationFailure()=runBlocking {
        val db=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build();val store=WorkspaceRepository(db)
        try {
            val old=WorkRecord("existing","inventory","Papel","{}",1);store.saveRecord(old)
            var rejected=false
            try{store.editRecords {listOf(old.copy(title="No confirmar"),WorkRecord("new","project","Invalido","x".repeat(2_000_001),1))}}catch(_:IllegalArgumentException){rejected=true}
            assertTrue(rejected);assertEquals(old.title,store.record(old.id)?.title);assertNull(store.record("new"))
        }finally{db.close()}
    }
}
