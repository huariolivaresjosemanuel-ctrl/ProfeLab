package pe.profelab.core.database

import androidx.room.withTransaction
import kotlinx.coroutines.flow.map
import pe.profelab.core.model.*

class LocalRepository(private val db: ProfeDatabase) : TaskRepository, ProfileRepository, DraftRepository, ActivityRepository {
    private val dao = db.dao()
    override fun observeTasks() = dao.observeTasks().map { list -> list.map { it.domain() } }
    override suspend fun findTask(id: String): Task? = dao.task(id)?.domain()
    override suspend fun saveTask(task: Task) {
        require(task.title.isNotBlank() && task.title.length <= 160)
        require(!task.reminderEnabled || task.dueAt != null)
        db.withTransaction {
            val previous = dao.task(task.id)
            dao.putTask(task.entity().copy(
                // Completion may have changed while the editor was open.
                completedAt = if (previous != null) previous.completedAt else task.completedAt,
                reminderDeliveredFor = previous?.reminderDeliveredFor?.takeIf { previous?.dueAt == task.dueAt },
            ))
            event(EntityKind.TASK, task.id, if (previous == null) "Tarea creada: ${task.title}" else "Tarea editada: ${task.title}", task.updatedAt)
            dao.deleteDraft("task:${task.id}")
        }
    }
    override suspend fun setCompleted(id: String, completed: Boolean, at: Long) {
        db.withTransaction {
            val task = dao.task(id) ?: error("La tarea ya no está disponible")
            dao.complete(id, at.takeIf { completed }, at)
            event(EntityKind.TASK, id, "${if (completed) "Completada" else "Reabierta"}: ${task.title}", at)
        }
    }
    override suspend fun deleteTask(id: String) {
        db.withTransaction {
            val task = dao.task(id) ?: return@withTransaction
            dao.deleteTask(id)
            dao.deleteDraft("task:$id")
            event(EntityKind.TASK, id, "Tarea eliminada: ${task.title}", System.currentTimeMillis())
        }
    }
    override suspend fun markReminderDelivered(id: String, dueAt: Long) = dao.markReminderDelivered(id, dueAt)
    override fun observeProfile() = dao.observeProfile().map { it?.let { p -> Profile(p.id,p.name,p.academicCycle,p.institution) } }
    override suspend fun saveProfile(profile: Profile) {
        require(profile.name.length <= 60 && profile.institution.length <= 120)
        val academicCycle = profile.academicCycle
        require(academicCycle == null || academicCycle in 1..20)
        db.withTransaction {
            dao.putProfile(ProfileEntity(id=profile.id, name=profile.name.trim(), academicCycle=profile.academicCycle, institution=profile.institution.trim()))
            dao.deleteDraft("profile")
            event(EntityKind.PROFILE, profile.id, "Perfil académico actualizado", System.currentTimeMillis())
        }
    }
    override fun observeActivity() = dao.observeActivity().map { list -> list.map { ActivityEvent(it.id, EntityRef(EntityKind.valueOf(it.kind), it.targetId), it.label, it.at) } }
    override suspend fun readDraft(key: String): Draft? = dao.draft(key)?.let { Draft(it.key,it.payload,it.updatedAt) }
    override suspend fun writeDraft(draft: Draft) = dao.putDraft(DraftEntity(draft.key,draft.payload,draft.updatedAt))
    override suspend fun clearDraft(key: String) = dao.deleteDraft(key)
    private suspend fun event(kind: EntityKind, id: String, label: String, at: Long) =
        dao.putActivity(ActivityEntity(newId(),kind.name,id,label,at))
}

private fun TaskEntity.domain() = Task(id,title,notes,dueAt,Priority.valueOf(priority),completedAt,reminderEnabled,reminderDeliveredFor,
    relatedId?.let { EntityRef(EntityKind.valueOf(requireNotNull(relatedKind)),it) },createdAt,updatedAt)
private fun Task.entity() = TaskEntity(id,title.trim(),notes,dueAt,priority.name,completedAt,reminderEnabled,reminderDeliveredFor,related?.kind?.name,related?.id,createdAt,updatedAt)
