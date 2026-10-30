package pe.profelab.core.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

enum class EntityKind { TASK, PROFILE, SESSION, PROJECT, NOTE, DOCUMENT, OBSERVATION }
data class EntityRef(val kind: EntityKind, val id: String)
enum class Priority(val label: String) { LOW("Baja"), NORMAL("Normal"), HIGH("Alta") }
data class Profile(val id: String, val name: String = "", val academicCycle: Int? = null, val institution: String = "")
data class Task(
    val id: String,
    val title: String,
    val notes: String = "",
    val dueAt: Long? = null,
    val priority: Priority = Priority.NORMAL,
    val completedAt: Long? = null,
    val reminderEnabled: Boolean = false,
    val reminderDeliveredFor: Long? = null,
    val related: EntityRef? = null,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val completed: Boolean get() = completedAt != null
    fun dueDate(zone: ZoneId): LocalDate? = dueAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
}
data class ActivityEvent(val id: String, val target: EntityRef, val label: String, val at: Long)
data class Draft(val key: String, val payload: String, val updatedAt: Long)
data class UserPreferences(val showCompleted: Boolean = false)

data class SourceReference(
    val id: String, val title: String, val year: Int, val author: String,
    val area: String, val officialUrl: String, val description: String,
    val rightsNote: String, val distributionApproved: Boolean = false,
    val localDocumentId: String? = null,
    val kind: String = "",
)
enum class Failure { INVALID_INPUT, NOT_FOUND, PERMISSION_DENIED, STORAGE_FULL, CORRUPT, UNSUPPORTED, UNAVAILABLE }
sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Error(val reason: Failure, val message: String) : Outcome<Nothing>
}
