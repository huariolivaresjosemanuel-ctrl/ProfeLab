package pe.profelab.feature.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import java.time.LocalTime

/** A standalone overlay flow. The app/NavHost underneath is retained, never recreated on dismissal. */
@Composable
fun WelcomeScreen(state:WelcomeState,confirm:(String,LocalRole)->Unit,skipSession:()->Unit,onPosition:(WelcomeClip,Int)->Unit={_,_->},onComplete:(WelcomeClip)->Unit={},onFailure:(WelcomeClip)->Unit={}){
    BackHandler {if(!state.busy)skipSession()}
    Surface(Modifier.fillMaxSize().testTag("welcome-screen"),color=ProfeColors.Cream){
        Box(Modifier.safeDrawingPadding().imePadding(),contentAlignment=Alignment.TopCenter){
            Column(Modifier.widthIn(max=960.dp).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement=Arrangement.spacedBy(16.dp)){
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                    Text("ProfeLab",style=MaterialTheme.typography.headlineLarge,color=ProfeColors.Green,modifier=Modifier.weight(1f))
                    TextButton(enabled=!state.busy,onClick=skipSession,modifier=Modifier.testTag("welcome-skip")){Text("Omitir")}
                }
                BoxWithConstraints {
                    val wide=maxWidth>=700.dp && LocalDensity.current.fontScale<1.4f
                    val art:@Composable (Modifier)->Unit={modifier->if(state.prefs.mascotVisible){
                        val clip=WelcomeMediaPolicy.clip(state.phase)
                        if(clip==null)CanonicalMascot(modifier.height(if(wide)400.dp else 248.dp),state.prefs.reduceMotion,fullBody=wide)
                        else Box(modifier,contentAlignment=Alignment.Center){LocalWelcomeVideo(clip,state,Modifier.height(if(wide)360.dp else 248.dp).aspectRatio(9f/16f),onPosition,onComplete,onFailure)}
                    }}
                    val content:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
                        if(state.mode==WelcomeMode.REGISTER && state.phase==WelcomePhase.FORM){
                            Text("¡Hola! Soy tu compañera de ProfeLab. ¿Cómo te gustaría que te llame?",style=MaterialTheme.typography.headlineSmall)
                            Text("Prepara tus clases, crea materiales y conserva tus ideas. Tu nombre o apodo es opcional.")
                            LocalWelcomeCard(state.prefs,state.busy,state.error,confirm)
                        }else {
                            Text(GreetingPolicy.message(LocalTime.now().hour,state.prefs,state.legacyName),style=MaterialTheme.typography.headlineSmall)
                            if(state.phase==WelcomePhase.ENTRY)Text("Un momento para comenzar. Puedes continuar al registro o elegir Omitir para entrar directamente.")
                            else if(state.mode==WelcomeMode.EXISTING_INTRO)Text("Soy tu compañera de ProfeLab. Tu perfil y tus trabajos siguen contigo; no necesitas registrarte otra vez.")
                            else Text("¿Continuamos creando y aprendiendo?")
                            if(state.videoFailed)Text("No se pudo reproducir el vídeo. Puedes continuar con la bienvenida estática.")
                            state.error?.let {ErrorNotice(it)}
                            Button(enabled=!state.busy,onClick={confirm("",LocalRole.UNSPECIFIED)},modifier=Modifier.fillMaxWidth().testTag("welcome-continue")){Text(if(state.busy)"Guardando…"else "Continuar")}
                        }
                        if(state.error!=null)TextButton(onClick=skipSession,enabled=!state.busy){Text("Entrar sin guardar esta presentación")}
                        Text("Sin cuenta ni conexión. Puedes cambiar tu nombre, rol y preferencias desde Perfil.",style=MaterialTheme.typography.bodySmall)
                    }}
                    if(wide)Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(32.dp)){
                        art(Modifier.weight(.8f));Box(Modifier.weight(1.2f)){content()}
                    }else Column(verticalArrangement=Arrangement.spacedBy(16.dp)){art(Modifier.fillMaxWidth());content()}
                }
            }
        }
    }
}
