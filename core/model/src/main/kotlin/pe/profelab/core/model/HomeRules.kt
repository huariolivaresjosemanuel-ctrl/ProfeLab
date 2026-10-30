package pe.profelab.core.model

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class WeekProgress(val done: Int, val total: Int) {
    val fraction: Float get() = if (total == 0) 0f else done.toFloat() / total
}
enum class TaskFilter(val label: String) {
    ALL("Todas"), TODAY("Hoy"), WEEK("Esta semana"), UPCOMING("Próximas"),
    OVERDUE("Vencidas"), UNDATED("Sin fecha"), DONE("Completadas"), REMINDERS("Avisos")
}
object HomeRules {
    fun greeting(hour: Int): String {
        require(hour in 0..23)
        return when (hour) { in 5..11 -> "Buenos días"; in 12..18 -> "Buenas tardes"; else -> "Buenas noches" }
    }
    fun inWeek(task: Task, today: LocalDate, zone: ZoneId): Boolean {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val date = task.dueDate(zone) ?: return false
        return !date.isBefore(monday) && date.isBefore(monday.plusDays(7))
    }
    fun progress(tasks: List<Task>, today: LocalDate, zone: ZoneId): WeekProgress {
        val week = tasks.filter { inWeek(it, today, zone) }
        return WeekProgress(week.count { it.completed }, week.size)
    }
    fun filter(tasks: List<Task>, filter: TaskFilter, today: LocalDate, zone: ZoneId): List<Task> = tasks.filter {
        val due = it.dueDate(zone)
        when (filter) {
            TaskFilter.ALL -> true
            TaskFilter.TODAY -> due == today
            TaskFilter.WEEK -> inWeek(it, today, zone)
            TaskFilter.UPCOMING -> !it.completed && due != null && !due.isBefore(today)
            TaskFilter.OVERDUE -> !it.completed && due != null && due.isBefore(today)
            TaskFilter.UNDATED -> due == null
            TaskFilter.DONE -> it.completed
            TaskFilter.REMINDERS -> it.reminderEnabled && !it.completed
        }
    }.sortedWith(compareBy<Task> { it.completed }.thenBy { it.dueAt ?: Long.MAX_VALUE }.thenByDescending { it.priority.ordinal }.thenBy { it.id })
}
