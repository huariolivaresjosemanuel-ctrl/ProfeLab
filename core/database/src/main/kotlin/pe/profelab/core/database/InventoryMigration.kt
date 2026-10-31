package pe.profelab.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Additive only. No old JSON, identifiers or stock quantities are rewritten. */
val INVENTORY_MIGRATION_2_3=object:Migration(2,3){
    override fun migrate(db:SupportSQLiteDatabase){
        db.execSQL("CREATE TABLE IF NOT EXISTS inventory_operations (id TEXT NOT NULL PRIMARY KEY, idempotencyKey TEXT NOT NULL, runId TEXT NOT NULL, kind TEXT NOT NULL, status TEXT NOT NULL, payload TEXT NOT NULL, payloadHash TEXT NOT NULL, revertsId TEXT, createdAt INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_operations_idempotencyKey ON inventory_operations (idempotencyKey)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_operations_runId ON inventory_operations (runId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_operations_revertsId ON inventory_operations (revertsId)")
        db.execSQL("CREATE TABLE IF NOT EXISTS inventory_movements (id TEXT NOT NULL PRIMARY KEY, operationId TEXT NOT NULL, inventoryId TEXT NOT NULL, beforeState TEXT NOT NULL, afterState TEXT NOT NULL, beforeReservations TEXT NOT NULL, afterReservations TEXT NOT NULL, sequence INTEGER NOT NULL, at INTEGER NOT NULL, FOREIGN KEY(operationId) REFERENCES inventory_operations(id) ON UPDATE NO ACTION ON DELETE NO ACTION)")
        db.execSQL("CREATE INDEX IF NOT EXISTS index_inventory_movements_operationId ON inventory_movements (operationId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_movements_inventoryId_sequence ON inventory_movements (inventoryId, sequence)")
        db.execSQL("CREATE TABLE IF NOT EXISTS inventory_reservations (id TEXT NOT NULL PRIMARY KEY, inventoryId TEXT NOT NULL, ownerId TEXT NOT NULL, quantity TEXT NOT NULL, unit TEXT NOT NULL, status TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_inventory_reservations_inventoryId_ownerId ON inventory_reservations (inventoryId, ownerId)")
    }
}
