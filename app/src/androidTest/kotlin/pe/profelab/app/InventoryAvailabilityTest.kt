package pe.profelab.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import pe.profelab.core.model.*
import pe.profelab.core.content.WorkshopCodec

@RunWith(AndroidJUnit4::class)
class InventoryAvailabilityTest {
    private fun project()=WorkRecord("run","project","Mi copia",JSONObject().put("materials",JSONArray().put(JSONObject().put("id","m1").put("name","Ligas").put("materialId","ligas").put("amount","10").put("unit","unidad").put("materialKind","CONSUMABLE"))).toString(),1)
    private fun stock(type:String="CONSUMABLE",quantity:String="14",reserved:String="0")=WorkRecord("stock","inventory","Ligas",JSONObject().put("materialId","ligas").put("materialKind",type).put("quantity",quantity).put("reserved",reserved).put("unit","unidad").toString(),1)
    @Test fun unknownTypeIsReviewWithoutRewritingTheHistoricalPayload(){
        val s=stock(type="UNKNOWN");val before=s.json
        assertEquals(AvailabilityState.REVIEW,InventoryAvailability.state(WorkshopCodec.assess(project(),listOf(s),emptyList()).coverage.single()))
        assertEquals(before,s.json)
    }
    @Test fun invalidMatchingStockDoesNotTurnIntoAnAssertedMissingAmount(){
        val coverage=WorkshopCodec.assess(project(),listOf(stock(quantity="por revisar")),emptyList()).coverage.single()
        assertEquals(AvailabilityState.REVIEW,InventoryAvailability.state(coverage));assertNull(coverage.missing)
    }
    @Test fun ownReservationIsAvailableButAnotherOwnersReservationIsProtected(){
        val s=stock(reserved="9");val j=JSONObject(s.json).put("inventoryLedgerVersion",1).put("inventoryReservations",JSONArray().put(JSONObject().put("ownerId","run").put("quantity","5")).put(JSONObject().put("ownerId","other").put("quantity","4")))
        val coverage=WorkshopCodec.assess(project(),listOf(s.copy(json=j.toString())),emptyList()).coverage.single()
        assertEquals(AvailabilityState.READY,InventoryAvailability.state(coverage));assertEquals(0,requireNotNull(coverage.available).compareTo(java.math.BigDecimal("10")))
    }
}
