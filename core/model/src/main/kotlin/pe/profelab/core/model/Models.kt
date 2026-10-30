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
enum class LocalRole(val label:String,val greeting:String) {
    UNSPECIFIED("Sin especificar",""), TEACHER("Docente","docente"),
    STUDENT("Estudiante de Educación Primaria","futuro docente"), INTERN("Practicante","practicante")
}
data class UserPreferences(val showCompleted: Boolean = false,val displayName:String="",val role:LocalRole=LocalRole.UNSPECIFIED,
    val onboardingCompleted:Boolean=false,val personalizedGreetings:Boolean=true,val mascotVisible:Boolean=true,val reduceMotion:Boolean=false,
    val welcomeIntroSeen:Boolean=false)

object PersonalizationRules {
    fun name(value:String)=value.filterNot {it.isISOControl()}.trim().take(60)
    fun greeting(hour:Int,preferences:UserPreferences,legacyName:String=""):String {
        val period=HomeRules.greeting(hour)
        if(!preferences.personalizedGreetings)return period
        val display=name(preferences.displayName.ifBlank {legacyName})
        if(display.isBlank())return "$period. ¡Qué bueno verte!"
        val role=preferences.role.greeting.takeIf {it.isNotBlank()}?.plus(" ").orEmpty()
        return "$period, $role$display"
    }
}

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
