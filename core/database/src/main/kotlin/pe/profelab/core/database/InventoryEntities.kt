package pe.profelab.core.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName="inventory_operations",indices=[Index(value=["idempotencyKey"],unique=true),Index("runId"),Index(value=["revertsId"],unique=true)])
data class InventoryOperationEntity(@PrimaryKey val id:String,val idempotencyKey:String,val runId:String,
    val kind:String,val status:String,val payload:String,val payloadHash:String,val revertsId:String?,val createdAt:Long,val updatedAt:Long)

@Entity(tableName="inventory_movements",indices=[Index("operationId"),Index(value=["inventoryId","sequence"],unique=true)],foreignKeys=[ForeignKey(entity=InventoryOperationEntity::class,parentColumns=["id"],childColumns=["operationId"],onDelete=ForeignKey.NO_ACTION)])
data class InventoryMovementEntity(@PrimaryKey val id:String,val operationId:String,val inventoryId:String,
    val beforeState:String,val afterState:String,val beforeReservations:String,val afterReservations:String,val sequence:Long,val at:Long)

@Entity(tableName="inventory_reservations",indices=[Index(value=["inventoryId","ownerId"],unique=true)])
data class InventoryReservationEntity(@PrimaryKey val id:String,val inventoryId:String,val ownerId:String,
    val quantity:String,val unit:String,val status:String,val updatedAt:Long)

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_operations WHERE id=:id") suspend fun operation(id:String):InventoryOperationEntity?
    @Query("SELECT * FROM inventory_operations WHERE revertsId=:id") suspend fun reversal(id:String):InventoryOperationEntity?
    @Query("SELECT * FROM inventory_operations WHERE runId=:runId AND kind='CONSUMPTION' AND status='PREPARED'") suspend fun prepared(runId:String):List<InventoryOperationEntity>
    @Query("SELECT * FROM inventory_operations WHERE runId=:runId ORDER BY CASE WHEN status='PREPARED' THEN 0 ELSE 1 END, createdAt DESC, id DESC LIMIT 100") fun observe(runId:String):Flow<List<InventoryOperationEntity>>
    @Insert suspend fun insertOperation(row:InventoryOperationEntity)
    @Update suspend fun updateOperation(row:InventoryOperationEntity)
    @Insert suspend fun insertMovement(row:InventoryMovementEntity)
    @Query("SELECT * FROM inventory_movements WHERE operationId=:id ORDER BY inventoryId") suspend fun movements(id:String):List<InventoryMovementEntity>
    @Query("SELECT * FROM inventory_movements WHERE inventoryId=:id ORDER BY sequence DESC LIMIT 1") suspend fun latest(id:String):InventoryMovementEntity?
    @Query("SELECT * FROM inventory_reservations WHERE inventoryId=:id ORDER BY id") suspend fun reservations(id:String):List<InventoryReservationEntity>
    @Upsert suspend fun putReservation(row:InventoryReservationEntity)
}
