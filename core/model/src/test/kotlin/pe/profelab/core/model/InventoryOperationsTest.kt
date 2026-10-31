package pe.profelab.core.model

import java.math.BigDecimal as D
import org.junit.Assert.*
import org.junit.Test

class InventoryOperationsTest {
    @Test fun fourAvailabilityStatesAreDistinct(){
        val need=Supply("Papel",D("10"),"hoja")
        fun coverage(q:String)=InventoryRules.assess(listOf(need),listOf(Stock("Papel",D(q),D.ZERO,"hoja"))).single()
        assertEquals(AvailabilityState.READY,InventoryAvailability.state(coverage("10")))
        assertEquals(AvailabilityState.PARTIAL,InventoryAvailability.state(coverage("4")))
        assertEquals(AvailabilityState.MISSING,InventoryAvailability.state(coverage("0")))
        assertEquals(AvailabilityState.REVIEW,InventoryAvailability.state(InventoryAvailability.evaluate(listOf(need),emptyList()).single()))
    }
    @Test fun reservedStockIsNotAvailable(){
        val r=InventoryAvailability.evaluate(listOf(Supply("Liga",D("10"),"unidad")),listOf(Stock("Liga",D("14"),D("10"),"unidad"))).single()
        assertEquals(AvailabilityState.PARTIAL,InventoryAvailability.state(r));assertEquals(0,D("6").compareTo(requireNotNull(r.missing)))
    }
    @Test fun packageWithoutEquivalenceNeedsReview(){
        val r=InventoryAvailability.evaluate(listOf(Supply("Papel",D.ONE,"hoja")),listOf(Stock("Papel",D.ONE,D.ZERO,"paquete"))).single()
        assertEquals(AvailabilityState.REVIEW,InventoryAvailability.state(r))
    }
    @Test fun ownReservationReleasedAllowsTheActualConsumption(){assertEquals("6",InventoryQuantities.text(InventoryQuantities.consume(D("14"),D.ZERO,D("8"))))}
    @Test(expected=IllegalArgumentException::class) fun otherProjectsReservationsAreProtected(){InventoryQuantities.consume(D("14"),D("10"),D("8"))}
    @Test(expected=IllegalArgumentException::class) fun insufficientBalanceCannotBecomeNegative(){InventoryQuantities.consume(D("1"),D.ZERO,D("2"))}
    @Test(expected=IllegalArgumentException::class) fun zeroIsNotAConsumption(){InventoryQuantities.consume(D("10"),D.ZERO,D.ZERO)}
    @Test fun decimalAmountsStayExact(){assertEquals("0.2",InventoryQuantities.text(InventoryQuantities.consume(D("0.3"),D.ZERO,D("0.1"))));assertEquals(0,D("1.5").compareTo(InventoryQuantities.amount("1,5")))}
    @Test(expected=IllegalArgumentException::class) fun precisionLimitIsNotSilentlyRounded(){InventoryQuantities.amount("0.000000001")}
}
