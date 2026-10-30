package pe.profelab.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Entity(tableName="workspace_records")
data class WorkspaceEntity(@PrimaryKey val id: String, val kind: String, val title: String,
    val json: String, val updatedAt: Long, val revision: Int, val archived: Boolean)
@Dao
interface WorkspaceDao {
    @Query("SELECT * FROM workspace_records ORDER BY updatedAt DESC") fun observe(): Flow<List<WorkspaceEntity>>
    @Query("SELECT * FROM workspace_records") suspend fun all(): List<WorkspaceEntity>
    @Query("SELECT * FROM workspace_records WHERE id=:id") suspend fun find(id: String): WorkspaceEntity?
    @Upsert suspend fun put(record: WorkspaceEntity)
    @Query("UPDATE workspace_records SET archived=1 WHERE id=:id") suspend fun archive(id: String)
}
