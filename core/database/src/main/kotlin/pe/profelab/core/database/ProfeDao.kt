package pe.profelab.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfeDao {
    @Query("SELECT * FROM tasks ORDER BY updatedAt DESC")
    fun observeTasks(): Flow<List<TaskEntity>>
    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun task(id: String): TaskEntity?
    @Upsert suspend fun putTask(task: TaskEntity)
    @Query("UPDATE tasks SET completedAt = :completedAt, updatedAt = :at WHERE id = :id")
    suspend fun complete(id: String, completedAt: Long?, at: Long)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: String)
    @Query("UPDATE tasks SET reminderDeliveredFor = :dueAt WHERE id = :id AND dueAt = :dueAt")
    suspend fun markReminderDelivered(id: String, dueAt: Long)
    @Query("SELECT * FROM profiles WHERE slot = 1") fun observeProfile(): Flow<ProfileEntity?>
    @Upsert suspend fun putProfile(profile: ProfileEntity)
    @Query("SELECT * FROM drafts WHERE `key` = :key") suspend fun draft(key: String): DraftEntity?
    @Upsert suspend fun putDraft(draft: DraftEntity)
    @Query("DELETE FROM drafts WHERE `key` = :key") suspend fun deleteDraft(key: String)
    @Query("SELECT * FROM activity ORDER BY at DESC, id DESC LIMIT 100")
    fun observeActivity(): Flow<List<ActivityEntity>>
    @Upsert suspend fun putActivity(event: ActivityEntity)
}
