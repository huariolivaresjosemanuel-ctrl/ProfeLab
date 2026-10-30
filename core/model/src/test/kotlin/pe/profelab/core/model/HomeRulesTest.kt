package pe.profelab.core.model

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HomeRulesTest {
    private val zone=ZoneId.of("America/Lima")
    private val thursday=LocalDate.of(2026,10,29)
    private fun task(day: LocalDate?, done: Boolean=false, priority: Priority=Priority.NORMAL)=Task(
        id=newId(),title="Preparar sesión",dueAt=day?.atTime(18,0)?.atZone(zone)?.toInstant()?.toEpochMilli(),
        priority=priority,completedAt=if(done) 1L else null,createdAt=0,updatedAt=0)

    @Test fun greetingBoundaries() {
        assertEquals("Buenas noches",HomeRules.greeting(4))
        assertEquals("Buenos días",HomeRules.greeting(5))
        assertEquals("Buenos días",HomeRules.greeting(11))
        assertEquals("Buenas tardes",HomeRules.greeting(12))
        assertEquals("Buenas tardes",HomeRules.greeting(18))
        assertEquals("Buenas noches",HomeRules.greeting(19))
    }
    @Test fun emptyWeekNeverInventsProgress() {
        assertEquals(WeekProgress(0,0),HomeRules.progress(emptyList(),thursday,zone))
        assertEquals(0f,WeekProgress(0,0).fraction,0.0001f)
    }
    @Test fun onlyMondayThroughSundayDueDatesCount() {
        val all=listOf(task(LocalDate.of(2026,10,25),true), task(LocalDate.of(2026,10,26),true),
            task(LocalDate.of(2026,11,1)),task(LocalDate.of(2026,11,2),true),task(null,true))
        assertEquals(WeekProgress(1,2),HomeRules.progress(all,thursday,zone))
    }
    @Test fun completionCanBeUndone() {
        val pending=task(thursday)
        assertEquals(WeekProgress(1,1),HomeRules.progress(listOf(pending.copy(completedAt=100)),thursday,zone))
        assertEquals(WeekProgress(0,1),HomeRules.progress(listOf(pending),thursday,zone))
    }
    @Test fun timezoneDefinesLocalWeek() {
        val due=java.time.Instant.parse("2026-11-02T02:00:00Z").toEpochMilli()
        val item=task(null,true).copy(dueAt=due)
        assertEquals(WeekProgress(1,1),HomeRules.progress(listOf(item),thursday,zone))
        assertEquals(WeekProgress(0,0),HomeRules.progress(listOf(item),thursday,ZoneId.of("UTC")))
    }
    @Test fun overdueExcludesCompletedAndUndated() {
        val late=task(thursday.minusDays(1))
        val all=listOf(late,task(thursday.minusDays(1),true),task(null),task(thursday))
        assertEquals(listOf(late),HomeRules.filter(all,TaskFilter.OVERDUE,thursday,zone))
    }
    @Test fun upcomingIncludesTodayAndSortsPriority() {
        val low=task(thursday,priority=Priority.LOW)
        val high=task(thursday,priority=Priority.HIGH)
        assertEquals(listOf(high,low),HomeRules.filter(listOf(low,high),TaskFilter.UPCOMING,thursday,zone))
    }
    @Test fun yearBoundaryStillUsesOneWeek() {
        val day=LocalDate.of(2027,1,1)
        assertTrue(HomeRules.inWeek(task(LocalDate.of(2026,12,28)),day,zone))
        assertFalse(HomeRules.inWeek(task(LocalDate.of(2027,1,4)),day,zone))
    }
    @Test(expected=IllegalArgumentException::class) fun invalidCoordinatesRejected() { NormalizedPoint(Float.NaN,.5f) }
    @Test fun remindersIncludeOnlyPendingEnabledTasks() {
        val enabled=task(thursday).copy(reminderEnabled=true)
        val completed=task(thursday,true).copy(reminderEnabled=true)
        assertEquals(listOf(enabled),HomeRules.filter(listOf(enabled,completed,task(thursday)),TaskFilter.REMINDERS,thursday,zone))
    }
}
