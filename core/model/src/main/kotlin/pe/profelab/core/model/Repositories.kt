package pe.profelab.core.model

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeTasks(): Flow<List<Task>>
    suspend fun findTask(id: String): Task?
    suspend fun saveTask(task: Task)
    suspend fun setCompleted(id: String, completed: Boolean, at: Long)
    suspend fun deleteTask(id: String)
    suspend fun markReminderDelivered(id: String, dueAt: Long)
}
interface ProfileRepository {
    fun observeProfile(): Flow<Profile?>
    suspend fun saveProfile(profile: Profile)
}
interface ActivityRepository { fun observeActivity(): Flow<List<ActivityEvent>> }
interface DraftRepository {
    suspend fun readDraft(key: String): Draft?
    suspend fun writeDraft(draft: Draft)
    suspend fun clearDraft(key: String)
}
interface PreferencesRepository {
    val preferences: Flow<UserPreferences>
    suspend fun setShowCompleted(enabled: Boolean)
    suspend fun setPersonalization(name:String,role:LocalRole)
    suspend fun setMascotPreferences(visible:Boolean,reduceMotion:Boolean,personalizedGreeting:Boolean)
}
interface EducationalSourceRepository { suspend fun sources(): List<SourceReference> }

/** Implemented in :app using WorkManager. No feature depends on WorkManager or another UI. */
interface ReminderScheduler {
    suspend fun reconcile(tasks: List<Task>)
    suspend fun cancel(taskId: String)
}
