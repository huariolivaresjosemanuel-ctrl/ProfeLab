package pe.profelab.app

import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import pe.profelab.app.reminders.LocalReminders
import pe.profelab.core.content.BundledSourceCatalog
import pe.profelab.core.database.*

/** Constructor injection at the composition root; features see only core contracts. */
class AppGraph(context: Context) {
    val database=ProfeDatabase.create(context)
    val repository=LocalRepository(database)
    val ocrDrafts=RetainedOcrDrafts(context,repository)
    val workspace=WorkspaceRepository(database)
    val transfers=BackupTransfer(context,database)
    val seeds=pe.profelab.core.content.WorkshopSeed(context)
    val packs=pe.profelab.core.content.ContentPacks(context,workspace)
    val files=pe.profelab.core.files.DocumentFiles(context)
    val incomingPdf=kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val preferences=LocalPreferences(context)
    val catalog=BundledSourceCatalog(context)
    private val reminders=LocalReminders(context)
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val _reminderWarning=MutableStateFlow<String?>(null)
    val reminderWarning=_reminderWarning.asStateFlow()
    fun startReminderSync() {
        scope.launch {
            repository.observeTasks().retryWhen { _, _ ->
                _reminderWarning.value="No se pudieron leer los avisos locales. Se reintentará automáticamente."
                delay(30_000); true
            }.collect { tasks -> sync(tasks) }
        }
    }
    fun refreshReminders() { scope.launch {
        try { sync(repository.observeTasks().first()) }
        catch(e: CancellationException) { throw e }
        catch (_: Exception) { _reminderWarning.value="No se pudo comprobar la programación de avisos." }
    } }
    private suspend fun sync(tasks: List<pe.profelab.core.model.Task>) {
        try {
            reminders.reconcile(tasks)
            _reminderWarning.value=if(tasks.any { it.reminderEnabled && !it.completed } && !reminders.allowed())
                "Android tiene los avisos desactivados. Puedes seguir consultando tus tareas." else null
        } catch(e: CancellationException) { throw e }
        catch (_: Exception) { _reminderWarning.value="No se pudieron programar los avisos. Las tareas siguen guardadas." }
    }
}
