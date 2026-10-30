package pe.profelab.app.reminders

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException
import pe.profelab.app.MainActivity
import pe.profelab.app.ProfeApplication
import pe.profelab.app.R

class TaskReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context,params) {
    override suspend fun doWork(): Result {
        val id=inputData.getString("taskId") ?: return Result.failure()
        val due=inputData.getLong("dueAt",-1)
        val repo=(applicationContext as ProfeApplication).graph.repository
        return try {
            val task=repo.findTask(id) ?: return Result.success()
            if(task.completed || !task.reminderEnabled || task.dueAt!=due || task.reminderDeliveredFor==due) return Result.success()
            if(Build.VERSION.SDK_INT>=33 && ContextCompat.checkSelfPermission(applicationContext,Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) return Result.success()
            if(!LocalReminders(applicationContext).allowed()) return Result.success()
            val pending=PendingIntent.getActivity(applicationContext,0,Intent(applicationContext,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val notification=NotificationCompat.Builder(applicationContext,REMINDER_CHANNEL)
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("Entrega pendiente")
                .setContentText(task.title).setContentIntent(pending).setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
            NotificationManagerCompat.from(applicationContext).notify("task:$id",1,notification)
            repo.markReminderDelivered(id,due)
            Result.success()
        } catch(e: CancellationException) { throw e }
        catch (_: SecurityException) { Result.success() }
        catch (_: Exception) { if(runAttemptCount<3) Result.retry() else Result.failure() }
    }
}
