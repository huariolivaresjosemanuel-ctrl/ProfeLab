package pe.profelab.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import pe.profelab.core.model.*

data class WelcomeState(val loading:Boolean=true,val mode:WelcomeMode=WelcomeMode.NONE,val prefs:UserPreferences=UserPreferences(),
    val legacyName:String="",val busy:Boolean=false,val error:String?=null,val startupError:Boolean=false,
    val phase:WelcomePhase=when(mode){WelcomeMode.REGISTER,WelcomeMode.EXISTING_INTRO->WelcomePhase.FORM;WelcomeMode.RETURNING->WelcomePhase.GREETING;else->WelcomePhase.NONE},
    val positionMs:Int=0,val videoComplete:Boolean=false,val videoFailed:Boolean=false)

class WelcomeFlowModel(private val preferences:PreferencesRepository,private val profiles:ProfileRepository,
    private val prepare:suspend ()->Unit,initiallyHandled:Boolean=false,externalLaunch:Boolean=false,
    autoDismissReturn:Boolean=true):ViewModel(){
    private val _state=MutableStateFlow(WelcomeState());val state=_state.asStateFlow()
    var sessionHandled=initiallyHandled || externalLaunch;private set
    private var external=externalLaunch
    private var allowAutomaticFinish=autoDismissReturn
    fun accessibilityChanged(touchExploration:Boolean){allowAutomaticFinish=!touchExploration}
    private var loadJob:Job?=null
    init {retry()}
    fun retry(){if(loadJob?.isActive==true)return;loadJob=viewModelScope.launch {
        _state.value=_state.value.copy(loading=true,error=null,startupError=false)
        try {
            prepare();val profile=profiles.observeProfile().first();val prefs=preferences.preferences.first()
            val mode=GreetingPolicy.mode(prefs,profile!=null,sessionHandled,external)
            val phase=if(!prefs.mascotVisible || prefs.reduceMotion)when(mode){WelcomeMode.REGISTER,WelcomeMode.EXISTING_INTRO->WelcomePhase.FORM;else->WelcomeMediaPolicy.initial(mode)}else WelcomeMediaPolicy.initial(mode)
            _state.value=WelcomeState(false,mode,prefs,profile?.name.orEmpty(),phase=phase)
            if(mode==WelcomeMode.NONE)sessionHandled=true
        }catch(e:CancellationException){throw e}catch(_:Exception){_state.value=_state.value.copy(loading=false,startupError=!external,error="No se pudo leer tu bienvenida local. Tus datos no se han borrado.")}
    }}
    fun externalContentRequested(){external=true;dismissForSession()}
    fun dismissForSession(){sessionHandled=true;_state.value=_state.value.copy(mode=WelcomeMode.NONE,phase=WelcomePhase.NONE,error=null,startupError=false)}
    fun videoPosition(clip:WelcomeClip,position:Int){if(WelcomeMediaPolicy.clip(state.value.phase)==clip)_state.value=state.value.copy(positionMs=position.coerceIn(0,clip.durationMs.toInt()))}
    fun videoFailed(clip:WelcomeClip){if(WelcomeMediaPolicy.clip(state.value.phase)==clip)_state.value=state.value.copy(videoFailed=true)}
    fun videoFinished(clip:WelcomeClip){
        if(WelcomeMediaPolicy.clip(state.value.phase)!=clip)return
        _state.value=state.value.copy(videoComplete=true,positionMs=clip.durationMs.toInt())
        if(allowAutomaticFinish){if(clip==WelcomeClip.ENTRY)showForm()else dismissForSession()}
    }
    private fun showForm(){_state.value=state.value.copy(phase=WelcomePhase.FORM,positionMs=0,videoComplete=false,videoFailed=false)}
    fun continueWelcome(name:String="",role:LocalRole=LocalRole.UNSPECIFIED){
        val current=state.value;if(current.busy || current.loading)return
        if(current.phase==WelcomePhase.ENTRY){showForm();return}
        if(current.phase==WelcomePhase.GREETING){dismissForSession();return}
        _state.value=current.copy(busy=true,error=null)
        viewModelScope.launch {try {
            when(current.mode){WelcomeMode.REGISTER->preferences.completeWelcomeRegistration(name,role);WelcomeMode.EXISTING_INTRO->preferences.markWelcomeIntroSeen();else->Unit}
            val saved=preferences.preferences.first()
            sessionHandled=true
            if(external || state.value.mode==WelcomeMode.NONE)_state.value=state.value.copy(busy=false)
            else _state.value=state.value.copy(mode=WelcomeMode.RETURNING,phase=WelcomePhase.GREETING,prefs=saved,busy=false,error=null,positionMs=0,videoComplete=false,videoFailed=false)
        }catch(e:CancellationException){throw e}catch(_:Exception){_state.value=_state.value.copy(busy=false,error="No se pudo guardar. El nombre anterior se conserva; puedes reintentar o entrar sin guardar esta presentación.")}}
    }
}
