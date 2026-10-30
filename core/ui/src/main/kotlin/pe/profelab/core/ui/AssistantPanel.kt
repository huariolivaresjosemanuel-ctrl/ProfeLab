package pe.profelab.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pe.profelab.core.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantPanel(advice:AssistantAdvice,error:String?,busy:Boolean,onAction:(AssistantAction)->Unit,onHide:()->Unit,dismiss:()->Unit){
    val tablet=LocalConfiguration.current.screenWidthDp>=840
    val text:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
        Text("¿En qué puedo ayudarte?",style=MaterialTheme.typography.headlineSmall)
        Text("Opciones de ProfeLab, con tus datos locales.",style=MaterialTheme.typography.bodyMedium)
        error?.let {ErrorNotice(it)}
        advice.notices.forEach {InfoCard("Tus registros",it)}
        advice.messages.forEach {message->Card(colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(message.text)
                OutlinedButton(onClick={onAction(message.action)},modifier=Modifier.fillMaxWidth().testTag(message.id)){Text(message.label)}
            }
        }}
        if(advice.messages.isEmpty())Text("No hay opciones disponibles para esta pantalla. Puedes seguir usando las cinco secciones.")
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            TextButton(enabled=!busy,onClick=onHide,modifier=Modifier.testTag("assistant-hide")){Text("Ocultar asistente")}
            TextButton(onClick=dismiss){Text("Cerrar")}
        }
        Text("Puedes volver a mostrarla desde Perfil → Mostrar mascota.",style=MaterialTheme.typography.bodySmall)
    }}
    val body:@Composable ()->Unit={Box(Modifier.testTag("assistant-panel").heightIn(max=if(tablet)780.dp else 700.dp).verticalScroll(rememberScrollState()).padding(20.dp)){
        if(tablet)Row(horizontalArrangement=Arrangement.spacedBy(24.dp),verticalAlignment=Alignment.Top){CanonicalMascot(Modifier.weight(.7f).height(360.dp),fullBody=true);Box(Modifier.weight(1.3f)){text()}}
        else Column(verticalArrangement=Arrangement.spacedBy(16.dp)){CanonicalMascot(Modifier.fillMaxWidth().height(220.dp),fullBody=true);text()}
    }}
    if(tablet)Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.widthIn(max=960.dp).fillMaxWidth().safeDrawingPadding(),shape=MaterialTheme.shapes.large,color=ProfeColors.Cream){body()}
    }else ModalBottomSheet(onDismissRequest=dismiss,containerColor=ProfeColors.Cream){body()}
}
