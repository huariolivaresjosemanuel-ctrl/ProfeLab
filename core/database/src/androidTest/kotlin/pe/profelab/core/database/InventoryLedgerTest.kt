package pe.profelab.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.*

@RunWith(AndroidJUnit4::class)
class InventoryLedgerTest {
    private val context=ApplicationProvider.getApplicationContext<Context>()
    private fun database()=Room.inMemoryDatabaseBuilder(context,ProfeDatabase::class.java).build()
    private suspend fun seed(db:ProfeDatabase,qty:String="14",reserved:String="0",type:String="CONSUMABLE",id:String="stock"):WorkspaceRepository {
        val store=WorkspaceRepository(db)
        store.saveRecord(WorkRecord("run","project","Proyecto de prueba","{}",1))
        store.saveRecord(WorkRecord(id,"inventory","Ligas",JSONObject().put("quantity",qty).put("reserved",reserved).put("reservationProjectId",if(reserved!="0")"run"else "").put("unit","unidad").put("materialId",id).put("materialKind",type).toString(),1))
        return store
    }
    private suspend fun value(store:WorkspaceStore,id:String="stock")=JSONObject(requireNotNull(store.record(id)).json)
    private suspend fun rejected(block:suspend ()->Unit){try{block();fail("Expected an explained rejection")}catch(_:IllegalArgumentException){}}
    private fun request(id:String="consume",amount:String="8",release:Boolean=true)=ConsumptionRequest(id,"run","Uso real",listOf(ActualUsage("stock",amount)),release)

    @Test fun confirmedConsumptionReleasesOnlyOwnReservationAndIsIdempotent():Unit=runBlocking {
        val db=database();try {
            val store=seed(db);store.setInventoryReservation("reserve","run","stock","10")
            store.prepareConsumption(request());assertEquals("14",value(store).getString("quantity"))
            store.confirmInventoryOperation("consume");store.confirmInventoryOperation("consume")
            assertEquals("6",value(store).getString("quantity"));assertEquals("0",value(store).getString("reserved"));assertEquals(1,db.inventory().movements("consume").size)
        }finally{db.close()}
    }
    @Test fun simultaneousConfirmationsCommitExactlyOnce():Unit=runBlocking {
        val db=database();try {
            val store=seed(db);store.prepareConsumption(request())
            coroutineScope {repeat(16){launch(Dispatchers.IO){store.confirmInventoryOperation("consume")}}}
            assertEquals("6",value(store).getString("quantity"));assertEquals(1,db.inventory().movements("consume").size)
        }finally{db.close()}
    }
    @Test fun preparedCommandSurvivesDatabaseCloseAndReopen():Unit=runBlocking {
        val name="qa-inventory-${newId()}.db"
        var db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build()
        try {
            seed(db).prepareConsumption(request());db.close()
            db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build();val store=WorkspaceRepository(db)
            assertEquals("PREPARED",db.inventory().operation("consume")?.status)
            store.confirmInventoryOperation("consume");db.close()
            db=Room.databaseBuilder(context,ProfeDatabase::class.java,name).build();val reopened=WorkspaceRepository(db)
            reopened.confirmInventoryOperation("consume");assertEquals("6",value(reopened).getString("quantity"))
        }finally{db.close();context.deleteDatabase(name)}
    }
    @Test fun failedMultiLineCommitRollsBackEarlierLinesAndReservations():Unit=runBlocking {
        val db=database();try {
            val store=seed(db,reserved="10");seed(db,qty="1",id="zz-second")
            store.prepareConsumption(ConsumptionRequest("multi","run","Uso parcial",listOf(ActualUsage("stock","8"),ActualUsage("zz-second","2"))))
            rejected {store.confirmInventoryOperation("multi")}
            assertEquals("14",value(store).getString("quantity"));assertEquals("10",value(store).getString("reserved"));assertEquals("1",value(store,"zz-second").getString("quantity"))
            assertEquals("PREPARED",db.inventory().operation("multi")?.status);assertTrue(db.inventory().movements("multi").isEmpty())
        }finally{db.close()}
    }
    @Test fun toolsReusableAndUnknownTypesCannotBeConsumed():Unit=runBlocking {
        for(type in listOf("TOOL","REUSABLE","UNKNOWN")){
            val db=database();try{val store=seed(db,type=type);rejected {store.prepareConsumption(request())};assertEquals("14",value(store).getString("quantity"))}finally{db.close()}
        }
    }
    @Test fun otherProjectReservationRemainsProtected():Unit=runBlocking {
        val db=database();try {
            val store=seed(db);store.saveRecord(WorkRecord("other","experiment","Otra ejecución","{}",1))
            store.setInventoryReservation("r1","run","stock","6");store.setInventoryReservation("r2","other","stock","5")
            store.prepareConsumption(request());store.confirmInventoryOperation("consume")
            assertEquals("6",value(store).getString("quantity"));assertEquals("5",value(store).getString("reserved"))
            assertEquals("other",value(store).getString("reservationProjectId"))
            rejected {store.setInventoryReservation("too-much","run","stock","2")}
            assertEquals("5",value(store).getString("reserved"))
        }finally{db.close()}
    }
    @Test fun keyCannotBeReusedForAnotherPayloadOrParallelDraft():Unit=runBlocking {
        val db=database();try {
            val store=seed(db);store.prepareConsumption(request())
            rejected {store.prepareConsumption(request(amount="7"))};rejected {store.prepareConsumption(request(id="another"))}
            store.confirmInventoryOperation("consume");assertEquals("6",value(store).getString("quantity"))
        }finally{db.close()}
    }
    @Test fun cancelledDraftDoesNotChangeStockAndCannotCommit():Unit=runBlocking {
        val db=database();try {val store=seed(db);store.prepareConsumption(request());store.cancelInventoryOperation("consume");rejected {store.confirmInventoryOperation("consume")};assertEquals("14",value(store).getString("quantity"));assertTrue(db.inventory().movements("consume").isEmpty())}finally{db.close()}
    }
    @Test fun preparingTheSameCommittedCommandAfterArchiveReturnsItsReceipt():Unit=runBlocking {
        val db=database();try {
            val store=seed(db);store.prepareConsumption(request());store.confirmInventoryOperation("consume");store.archiveRecord("stock")
            assertEquals("COMMITTED",store.prepareConsumption(request()).status)
            store.confirmInventoryOperation("consume");assertEquals("6",value(store).getString("quantity"))
            rejected {store.prepareConsumption(request(amount="7"))}
        }finally{db.close()}
    }
    @Test fun reversalAddsCompensationWithoutErasingOriginalAndCannotRepeat():Unit=runBlocking {
        val db=database();try {
            val store=seed(db,reserved="10");store.prepareConsumption(request());store.confirmInventoryOperation("consume")
            store.reverseInventoryOperation("undo","consume");store.reverseInventoryOperation("undo-again","consume")
            assertEquals("14",value(store).getString("quantity"));assertEquals("10",value(store).getString("reserved"))
            val events=value(store).getJSONArray("movements");assertTrue((0 until events.length()).any {events.getJSONObject(it).optString("id")=="consume"});assertEquals("REVERSAL",events.getJSONObject(events.length()-1).getString("kind"));assertEquals("consume",db.inventory().operation("undo")?.revertsId)
        }finally{db.close()}
    }
    @Test fun staleFormCannotOverwriteAConfirmedBalance():Unit=runBlocking {
        val db=database();try {val store=seed(db);val stale=requireNotNull(store.record("stock"));store.prepareConsumption(request());store.confirmInventoryOperation("consume");rejected {store.saveRecord(stale)};assertEquals("6",value(store).getString("quantity"))}finally{db.close()}
    }
    @Test fun notesCannotOverwriteTheMultiOwnerProjectionEvenWithTheSameTotal():Unit=runBlocking {
        val db=database();try {
            val store=seed(db);store.saveRecord(WorkRecord("other","project","Otra copia","{}",1))
            store.setInventoryReservation("a","run","stock","5");store.setInventoryReservation("b","other","stock","5")
            val stale=requireNotNull(store.record("stock"))
            store.setInventoryReservation("c","run","stock","6");store.setInventoryReservation("d","other","stock","4")
            rejected {store.saveRecord(stale.copy(json=JSONObject(stale.json).put("notes","Mi nota tardía").toString()))}
            val current=requireNotNull(store.record("stock"));val forged=JSONObject(current.json).put("notes","Mi nota actual").put("inventoryReservations",JSONArray())
            store.saveRecord(current.copy(json=forged.toString()))
            val after=value(store);assertEquals("Mi nota actual",after.getString("notes"));assertEquals(2,after.getJSONArray("inventoryReservations").length());assertEquals("10",after.getString("reserved"))
        }finally{db.close()}
    }
    @Test fun reservationNeverChangesPhysicalQuantityAndPreventsArchive():Unit=runBlocking {
        val db=database();try {val store=seed(db);store.setInventoryReservation("r","run","stock","10");assertEquals("14",value(store).getString("quantity"));rejected {store.archiveRecord("stock")};assertFalse(requireNotNull(store.record("stock")).archived);store.setInventoryReservation("release","run","stock","0");store.archiveRecord("stock");assertTrue(requireNotNull(store.record("stock")).archived)}finally{db.close()}
    }
    @Test fun legacyMutationKeyCannotExecuteDeltaTwice():Unit=runBlocking {
        val db=database();try {
            val store=seed(db)
            suspend fun apply(){store.mutateInventory("one-adjustment"){rows->val old=rows.single {it.id=="stock"};val j=JSONObject(old.json);listOf(old.copy(json=j.put("quantity",(j.getString("quantity").toBigDecimal()+java.math.BigDecimal.ONE).toPlainString()).toString()))}}
            apply();apply();assertEquals("15",value(store).getString("quantity"))
        }finally{db.close()}
    }
}
