package pe.profelab.app.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pe.profelab.core.model.*
import java.util.concurrent.TimeUnit

const val REMINDER_CHANNEL="profelab_tasks"
private const val REMINDER_TAG="profelab-reminder"
class LocalReminders(private val context: Context) : ReminderScheduler {
    private val work=WorkManager.getInstance(context)
    private val mutex=Mutex()
    init { context.getSystemService(NotificationManager::class.java).createNotificationChannel(
        NotificationChannel(REMINDER_CHANNEL,"Entregas y tareas",NotificationManager.IMPORTANCE_DEFAULT).apply { description="Avisos que activas en tus tareas" }
    ) }
    fun allowed(): Boolean {
        val permission=Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED
        val channel=context.getSystemService(NotificationManager::class.java).getNotificationChannel(REMINDER_CHANNEL)
        return permission && NotificationManagerCompat.from(context).areNotificationsEnabled() && channel?.importance != NotificationManager.IMPORTANCE_NONE
    }
    override suspend fun reconcile(tasks: List<Task>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val eligible=tasks.filter { it.reminderEnabled && !it.completed && it.dueAt!=null && it.reminderDeliveredFor!=it.dueAt }
            val current=work.getWorkInfosByTag(REMINDER_TAG).get()
            val desired=eligible.map { tag(it) }.toSet()
            current.filter { !it.state.isFinished }.forEach { info ->
                if(!allowed() || info.tags.none { it in desired }) work.cancelWorkById(info.id).result.get()
            }
            // Clear delivered notifications when a task is completed, deleted or its reminder disabled.
            val notificationManager=context.getSystemService(NotificationManager::class.java)
            val activeIds=tasks.filter { !it.completed && it.reminderEnabled }.map { it.id }.toSet()
            notificationManager.activeNotifications.filter { it.tag?.startsWith("task:")==true }.forEach {
                if(it.tag.removePrefix("task:") !in activeIds) notificationManager.cancel(it.tag,it.id)
            }
            if(allowed()) eligible.forEach { task ->
                val key=tag(task)
                if(current.none { key in it.tags && !it.state.isFinished }) {
                    val request=OneTimeWorkRequestBuilder<TaskReminderWorker>()
                        .setInitialDelay((requireNotNull(task.dueAt)-System.currentTimeMillis()).coerceAtLeast(0),TimeUnit.MILLISECONDS)
                        .setInputData(workDataOf("taskId" to task.id,"dueAt" to task.dueAt))
                        .addTag(REMINDER_TAG).addTag(key).build()
                    work.enqueueUniqueWork("task:${task.id}",ExistingWorkPolicy.REPLACE,request).result.get()
                }
            }
        }
    }
    override suspend fun cancel(taskId: String) { withContext(Dispatchers.IO) { work.cancelUniqueWork("task:$taskId").result.get() } }
    private fun tag(task: Task)="delivery:${task.id}:${task.dueAt}"
}
