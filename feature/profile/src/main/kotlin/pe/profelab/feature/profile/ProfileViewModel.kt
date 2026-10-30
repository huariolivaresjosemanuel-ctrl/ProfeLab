package pe.profelab.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONObject
import pe.profelab.core.model.*

data class ProfileState(val loading: Boolean=true, val id: String="", val name: String="", val cycle: String="", val institution: String="", val saved: Boolean=false, val saving: Boolean=false, val draftSaved: Boolean=false, val error: String?=null, val finished: Boolean=false)
class ProfileViewModel(private val profiles: ProfileRepository, private val drafts: DraftRepository, private val preferences: PreferencesRepository) : ViewModel() {
    private val _state=MutableStateFlow(ProfileState())
    val state=_state.asStateFlow()
    val settings=preferences.preferences.catch {
        _state.value=_state.value.copy(error="No se pudieron leer las preferencias locales.")
        emit(UserPreferences())
    }.stateIn(viewModelScope,SharingStarted.WhileSubscribed(5_000),UserPreferences())
    private val operations=Channel<suspend ()->Unit>(Channel.UNLIMITED)
    init {
        viewModelScope.launch {
            try {
                val profile=profiles.observeProfile().first() ?: Profile(newId())
                val draft=drafts.readDraft("profile")?.let { JSONObject(it.payload) }
                _state.value=ProfileState(false,profile.id,draft?.getString("name") ?: profile.name,
                    draft?.getString("cycle") ?: profile.academicCycle?.toString().orEmpty(),
                    draft?.getString("institution") ?: profile.institution,draftSaved=draft!=null)
            } catch(e: CancellationException) { throw e }
            catch (_: Exception) { _state.value=ProfileState(loading=false,error="No se pudo leer el perfil. Vuelve a abrir esta pantalla.") }
            for(operation in operations) try { operation() }
            catch(e: CancellationException) { throw e }
            catch (_: Exception) { _state.value=_state.value.copy(saving=false,saved=false,error="No se pudo guardar. Inténtalo de nuevo; los datos anteriores se conservan.") }
        }
    }
    fun update(name: String=state.value.name, cycle: String=state.value.cycle, institution: String=state.value.institution) {
        if(state.value.saving || state.value.id.isBlank()) return
        val next=state.value.copy(name=name.take(60),cycle=cycle.take(2),institution=institution.take(120),saved=false,draftSaved=false,error=null)
        _state.value=next
        operations.trySend {
            val data=JSONObject().put("name",next.name).put("cycle",next.cycle).put("institution",next.institution)
            drafts.writeDraft(Draft("profile",data.toString(),System.currentTimeMillis()))
            if(state.value==next) _state.value=next.copy(draftSaved=true)
        }
    }
    fun save() {
        val s=state.value
        if(s.saving || s.id.isBlank()) return
        val cycle=s.cycle.toIntOrNull()
        if(s.cycle.isNotBlank() && (cycle==null || cycle !in 1..20)) { _state.value=s.copy(error="Indica un ciclo académico entre 1 y 20, o deja el campo vacío."); return }
        _state.value=s.copy(saving=true,error=null)
        operations.trySend { profiles.saveProfile(Profile(s.id,s.name,cycle,s.institution)); _state.value=state.value.copy(saving=false,saved=true,draftSaved=false) }
    }
    fun showCompleted(enabled: Boolean) { operations.trySend { preferences.setShowCompleted(enabled) } }
    fun leave() {
        if(state.value.saving || state.value.finished) return
        val s=state.value
        _state.value=s.copy(saving=true)
        operations.trySend {
            if(!s.saved && s.id.isNotBlank()) drafts.writeDraft(Draft("profile",JSONObject().put("name",s.name).put("cycle",s.cycle).put("institution",s.institution).toString(),System.currentTimeMillis()))
            _state.value=state.value.copy(saving=false,finished=true)
        }
    }
}
