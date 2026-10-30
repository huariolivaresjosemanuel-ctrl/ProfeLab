package pe.profelab.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import pe.profelab.core.model.*
import java.time.ZonedDateTime

data class HomeState(
    val loading: Boolean = true, val profile: Profile? = null, val tasks: List<Task> = emptyList(),
    val activity: List<ActivityEvent> = emptyList(), val error: String? = null,
    val now: ZonedDateTime = ZonedDateTime.now(),
)
class HomeViewModel(
    private val tasks: TaskRepository, profiles: ProfileRepository, activities: ActivityRepository,
) : ViewModel() {
    private val revision = MutableStateFlow(0)
    private val ticker = flow { while (true) { emit(ZonedDateTime.now()); delay(30_000) } }
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state = revision.flatMapLatest {
        combine(tasks.observeTasks(),profiles.observeProfile(),activities.observeActivity(),ticker) { t,p,a,now ->
            HomeState(false,p,t,a,now=now)
        }.catch { emit(HomeState(loading=false,error="No se pudieron leer los datos locales. Tus archivos se conservan.")) }
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),HomeState())
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()
    fun retry() { revision.value++ }
    fun dismissMessage() { _message.value=null }
    suspend fun complete(task: Task, value: Boolean): Boolean {
        try { tasks.setCompleted(task.id,value,System.currentTimeMillis()); return true }
        catch (e: CancellationException) { throw e }
        catch (_: Exception) { _message.value="No se pudo guardar el cambio. Inténtalo de nuevo."; return false }
    }
}
