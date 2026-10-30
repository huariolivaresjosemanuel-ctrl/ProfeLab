package pe.profelab.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "tasks", indices = [Index("dueAt"), Index("updatedAt")])
data class TaskEntity(
    @PrimaryKey val id: String, val title: String, val notes: String, val dueAt: Long?,
    val priority: String, val completedAt: Long?, val reminderEnabled: Boolean,
    val reminderDeliveredFor: Long?, val relatedKind: String?, val relatedId: String?,
    val createdAt: Long, val updatedAt: Long,
)
@Entity(tableName = "profiles", indices = [Index(value = ["id"], unique = true)])
data class ProfileEntity(
    @PrimaryKey val slot: Int = 1, val id: String, val name: String,
    val academicCycle: Int?, val institution: String,
)
@Entity(tableName = "drafts")
data class DraftEntity(@PrimaryKey val key: String, val payload: String, val updatedAt: Long)
@Entity(tableName = "activity", indices = [Index("at")])
data class ActivityEntity(
    @PrimaryKey val id: String, val kind: String, val targetId: String, val label: String, val at: Long,
)
