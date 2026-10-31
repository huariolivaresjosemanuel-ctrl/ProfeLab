package pe.profelab.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [TaskEntity::class, ProfileEntity::class, DraftEntity::class, ActivityEntity::class, WorkspaceEntity::class, InventoryOperationEntity::class, InventoryMovementEntity::class, InventoryReservationEntity::class], version = 3, exportSchema = true)
abstract class ProfeDatabase : RoomDatabase() {
    abstract fun dao(): ProfeDao
    abstract fun workspace(): WorkspaceDao
    abstract fun inventory(): InventoryDao
    companion object {
        val MIGRATION_1_2=object: Migration(1,2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS workspace_records (id TEXT NOT NULL PRIMARY KEY, kind TEXT NOT NULL, title TEXT NOT NULL, json TEXT NOT NULL, updatedAt INTEGER NOT NULL, revision INTEGER NOT NULL, archived INTEGER NOT NULL)")
            }
        }
        fun create(context: Context): ProfeDatabase = Room.databaseBuilder(
            context.applicationContext, ProfeDatabase::class.java, "profelab.db"
        ).addMigrations(MIGRATION_1_2, INVENTORY_MIGRATION_2_3).build()
    }
}
