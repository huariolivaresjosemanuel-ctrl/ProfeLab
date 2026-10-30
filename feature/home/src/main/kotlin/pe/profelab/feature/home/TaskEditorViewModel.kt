package pe.profelab.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import pe.profelab.core.model.*

data class TaskEditorState(
    val loading: Boolean = true, val task: Task? = null, val saving: Boolean = false,
    val finished: Boolean = false, val draftSaved: Boolean = false, val error: String? = null,
)
class TaskEditorViewModel(private val id: String, private val tasks: TaskRepository, private val drafts: DraftRepository) : ViewModel() {
    private val key = "task:$id"
    private val operations = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private val _state = MutableStateFlow(TaskEditorState())
    val state = _state.asStateFlow()
    init {
        viewModelScope.launch {
            try {
                val stored = if (id == "new") null else tasks.findTask(id)
                if (id != "new" && stored == null) {
                    _state.value=TaskEditorState(loading=false,error="Esta tarea ya no está disponible. El historial se conserva.")
                } else {
                    val draft = drafts.readDraft(key)
                    val task = draft?.let { decode(it.payload) } ?: stored ?: Task(newId(),"",createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis())
                    _state.value=TaskEditorState(loading=false,task=task,draftSaved=draft != null)
                }
            } catch(e: CancellationException) { throw e }
            catch (_: Exception) { _state.value=TaskEditorState(loading=false,error="No se pudo recuperar el borrador. Vuelve a abrir la tarea para reintentar.") }
            for (operation in operations) {
                try { operation() }
                catch(e: CancellationException) { throw e }
                catch (_: Exception) { _state.value=_state.value.copy(saving=false,draftSaved=false,error="No se pudo guardar. Conserva esta pantalla y vuelve a intentar; el último guardado sigue intacto.") }
            }
        }
    }
    fun update(transform: (Task)->Task) {
        val current = _state.value.task ?: return
        if (_state.value.saving || _state.value.finished) return
        val task=transform(current).copy(updatedAt=System.currentTimeMillis())
        _state.value=_state.value.copy(task=task,draftSaved=false,error=null)
        operations.trySend {
            drafts.writeDraft(Draft(key,encode(task),System.currentTimeMillis()))
            if (_state.value.task==task) _state.value=_state.value.copy(draftSaved=true)
        }
    }
    fun save() {
        val task=_state.value.task ?: return
        if (_state.value.saving) return
        if (task.title.isBlank()) { _state.value=_state.value.copy(error="Escribe un título para guardar la tarea."); return }
        val dueAt = task.dueAt
        if (task.reminderEnabled && (dueAt == null || dueAt <= System.currentTimeMillis())) {
            _state.value=_state.value.copy(error="Para activar un aviso, elige una fecha y hora futuras."); return
        }
        _state.value=_state.value.copy(saving=true,error=null)
        operations.trySend {
            tasks.saveTask(task.copy(title=task.title.trim(),updatedAt=System.currentTimeMillis()))
            drafts.clearDraft(key)
            _state.value=_state.value.copy(saving=false,finished=true)
        }
    }
    fun delete() {
        val task=_state.value.task ?: return
        if (_state.value.saving) return
        _state.value=_state.value.copy(saving=true)
        operations.trySend { tasks.deleteTask(task.id); drafts.clearDraft(key); _state.value=_state.value.copy(saving=false,finished=true) }
    }
    fun discard() {
        if (_state.value.saving) return
        _state.value=_state.value.copy(saving=true)
        operations.trySend { drafts.clearDraft(key); _state.value=_state.value.copy(saving=false,finished=true) }
    }
    fun leave() {
        if (_state.value.saving || _state.value.finished) return
        _state.value=_state.value.copy(saving=true)
        operations.trySend {
            _state.value.task?.let { drafts.writeDraft(Draft(key,encode(it),System.currentTimeMillis())) }
            _state.value=_state.value.copy(saving=false,finished=true)
        }
    }
    private fun encode(t: Task): String = JSONObject().apply {
        put("id",t.id); put("title",t.title); put("notes",t.notes); put("dueAt",t.dueAt)
        put("priority",t.priority.name); put("completedAt",t.completedAt); put("reminder",t.reminderEnabled)
        put("createdAt",t.createdAt); put("updatedAt",t.updatedAt)
        put("relatedKind",t.related?.kind?.name); put("relatedId",t.related?.id)
    }.toString()
    private fun decode(raw: String): Task {
        val j=JSONObject(raw)
        return Task(id=j.getString("id"),title=j.getString("title"),notes=j.getString("notes"),
            dueAt=if(j.has("dueAt")) j.getLong("dueAt") else null,priority=Priority.valueOf(j.getString("priority")),
            completedAt=if(j.has("completedAt")) j.getLong("completedAt") else null,reminderEnabled=j.getBoolean("reminder"),
            related=if(j.has("relatedId")) EntityRef(EntityKind.valueOf(j.getString("relatedKind")),j.getString("relatedId")) else null,
            createdAt=j.getLong("createdAt"),updatedAt=j.getLong("updatedAt"))
    }
}
