package pe.profelab.core.model

import java.math.BigDecimal
import kotlinx.coroutines.flow.Flow

enum class AvailabilityState { READY, PARTIAL, MISSING, REVIEW }
object InventoryAvailability {
    fun state(coverage:RequirementCoverage):AvailabilityState = when {
        coverage.review.isNotBlank() || coverage.available==null || coverage.missing==null -> AvailabilityState.REVIEW
        coverage.covered -> AvailabilityState.READY
        coverage.available.signum()>0 -> AvailabilityState.PARTIAL
        else -> AvailabilityState.MISSING
    }
    fun evaluate(needs:List<Supply>,stocks:List<Stock>):List<RequirementCoverage> =
        InventoryRules.assess(needs,stocks).map { if(stocks.isEmpty())it.copy(available=null,missing=null,review="No constan existencias; registra o revisa tu inventario") else it }
}
data class ActualUsage(val inventoryId:String,val amount:String)
data class ConsumptionRequest(val operationId:String,val runId:String,val reason:String,
    val lines:List<ActualUsage>,val releaseOwnReservations:Boolean=true)
data class InventoryOperation(val id:String,val runId:String,val kind:String,val status:String,
    val reason:String,val lines:List<ActualUsage>,val releaseOwnReservations:Boolean=false)
data class InventoryReservation(val inventoryId:String,val ownerId:String,val quantity:String,val unit:String)

/** Prepared operations survive process death; confirmation receives an existing durable ID. */
interface InventoryOperationStore {
    fun observeInventoryOperations(runId:String):Flow<List<InventoryOperation>>
    suspend fun prepareConsumption(request:ConsumptionRequest):InventoryOperation
    suspend fun confirmInventoryOperation(operationId:String)
    suspend fun cancelInventoryOperation(operationId:String)
    suspend fun setInventoryReservation(operationId:String,runId:String,inventoryId:String,quantity:String)
    suspend fun reverseInventoryOperation(operationId:String,originalId:String)
    suspend fun latestInventoryOperation(inventoryId:String):String?
    /** Existing stock/shopping commands retain one key across retries in the save queue. */
    suspend fun mutateInventory(operationId:String,change:(List<WorkRecord>)->List<WorkRecord>)
}

object InventoryQuantities {
    fun amount(value:String):BigDecimal {
        require(value.length<=64){"Cantidad demasiado larga"}
        return requireNotNull(InventoryRules.decimal(value)){"Cantidad inválida; revisa precisión y decimales"}
    }
    fun text(value:BigDecimal):String {
        require(value.signum()>=0 && InventoryRules.decimal(value.toPlainString())!=null){"Saldo fuera de límites"}
        return value.stripTrailingZeros().toPlainString()
    }
    fun consume(balance:BigDecimal,otherReserved:BigDecimal,actual:BigDecimal):BigDecimal {
        require(actual.signum()>0 && otherReserved.signum()>=0 && otherReserved<=balance){"Revisa consumo y reservas"}
        return (balance-actual).also {require(it>=otherReserved){"La cantidad supera las existencias libres; no se descontó nada"};text(it)}
    }
}
