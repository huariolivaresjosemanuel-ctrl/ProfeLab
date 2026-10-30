package pe.profelab.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.profelab.core.model.*

data class WelcomeState(val loading:Boolean=true,val mode:WelcomeMode=WelcomeMode.NONE,val prefs:UserPreferences=UserPreferences(),
    val legacyName:String="",val busy:Boolean=false,val error:String?=null,val startupError:Boolean=false)

/** One instance per Activity ViewModelStore: rotation, tabs and document pickers cannot replay it. */
class WelcomeFlowModel(private val preferences:PreferencesRepository,private val profiles:ProfileRepository,
    private val prepare:suspend ()->Unit,initiallyHandled:Boolean=false,externalLaunch:Boolean=false,
    private val autoDismissReturn:Boolean=true):ViewModel(){
    private val _state=MutableStateFlow(WelcomeState());val state=_state.asStateFlow()
    var sessionHandled=initiallyHandled || externalLaunch;private set
    private var external=externalLaunch
    private var returnJob:Job?=null
    private var loadJob:Job?=null
    init {retry()}
    fun retry(){
        if(loadJob?.isActive==true)return
        loadJob=viewModelScope.launch {
            _state.value=_state.value.copy(loading=true,error=null,startupError=false)
            try {
                prepare()
                val profile=profiles.observeProfile().first();val prefs=preferences.preferences.first()
                val mode=GreetingPolicy.mode(prefs,profile!=null,sessionHandled,external)
                _state.value=WelcomeState(false,mode,prefs,profile?.name.orEmpty())
                if(mode==WelcomeMode.NONE)sessionHandled=true
                if(mode==WelcomeMode.RETURNING && autoDismissReturn){
                    // Dismiss a visible greeting, never hold the Android splash or navigation on a timer.
                    returnJob=viewModelScope.launch {delay(1300);dismissForSession()}
                }
            }catch(e:CancellationException){throw e}catch(_:Exception){_state.value=_state.value.copy(loading=false,startupError=!external,error="No se pudo leer tu bienvenida local. Tus datos no se han borrado.")}
        }
    }
    fun externalContentRequested(){external=true;dismissForSession()}
    fun dismissForSession(){returnJob?.cancel();sessionHandled=true;_state.value=_state.value.copy(mode=WelcomeMode.NONE,error=null,startupError=false)}
    fun continueWelcome(name:String="",role:LocalRole=LocalRole.UNSPECIFIED){
        val current=state.value
        if(current.busy || current.loading)return
        if(current.mode==WelcomeMode.RETURNING){dismissForSession();return}
        _state.value=current.copy(busy=true,error=null)
        viewModelScope.launch {
            try {
                when(current.mode){
                    WelcomeMode.REGISTER->preferences.completeWelcomeRegistration(name,role)
                    WelcomeMode.EXISTING_INTRO->preferences.markWelcomeIntroSeen()
                    else->Unit
                }
                // Do not reinstate an overlay dismissed meanwhile by an external PDF intent.
                sessionHandled=true;_state.value=_state.value.copy(mode=WelcomeMode.NONE,busy=false,error=null)
            }catch(e:CancellationException){throw e}catch(_:Exception){_state.value=_state.value.copy(busy=false,error="No se pudo guardar. El nombre anterior se conserva; puedes reintentar o entrar sin guardar esta presentación.")}
        }
    }
}
