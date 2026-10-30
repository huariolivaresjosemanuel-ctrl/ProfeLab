package pe.profelab.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskEntity::class, ProfileEntity::class, DraftEntity::class, ActivityEntity::class], version = 1, exportSchema = true)
abstract class ProfeDatabase : RoomDatabase() {
    abstract fun dao(): ProfeDao
    companion object {
        fun create(context: Context): ProfeDatabase = Room.databaseBuilder(
            context.applicationContext, ProfeDatabase::class.java, "profelab.db"
        ).build() // No destructive fallback. Future versions require an explicit migration.
    }
}
