package pe.profelab.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
fun AssistantPanel(advice:AssistantAdvice,error:String?,busy:Boolean,onAction:(AssistantAction)->Unit,onHide:()->Unit,dismiss:()->Unit,context:String="home",reduceMotion:Boolean=true){
    val tablet=LocalConfiguration.current.screenWidthDp>=840
    var advanced by rememberSaveable(context){mutableStateOf(false)}
    val primary=AssistantExperience.primary(advice,context)
    val secondary=advice.messages.filter {message->primary.none {it.id==message.id}}
    val text:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)){
            CanonicalMascot(Modifier.width(72.dp).height(94.dp),reduceMotion=reduceMotion,gesture=MascotGesture.EXPLAIN)
            Column(Modifier.weight(1f)){Text("¿En qué puedo ayudarte?",style=MaterialTheme.typography.headlineSmall);Text("Ayuda local para esta pantalla.",style=MaterialTheme.typography.bodyMedium)}
        }
        error?.let {ErrorNotice(it)}
        advice.notices.take(1).forEach {Text(it,style=MaterialTheme.typography.bodySmall)}
        primary.forEach {message->Card(colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){
            Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(message.text)
                OutlinedButton(enabled=!busy,onClick={onAction(message.action)},modifier=Modifier.fillMaxWidth().testTag(message.id)){Text(message.label)}
            }
        }}
        if(advice.messages.isEmpty())Text("No hay opciones disponibles para esta pantalla. Puedes seguir usando las cinco secciones.")
        if(secondary.isNotEmpty() || advice.notices.size>1)TextButton(onClick={advanced=!advanced},modifier=Modifier.testTag("assistant-more")){Text(if(advanced)"Mostrar menos opciones"else "Más opciones de ayuda")}
        if(advanced){
            advice.notices.drop(1).forEach {InfoCard("Tus registros",it)}
            secondary.forEach {message->Column(verticalArrangement=Arrangement.spacedBy(6.dp)){
                Text(message.text,style=MaterialTheme.typography.bodySmall)
                OutlinedButton(enabled=!busy,onClick={onAction(message.action)},modifier=Modifier.fillMaxWidth().testTag(message.id)){Text(message.label)}
            }}
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
            TextButton(enabled=!busy,onClick=onHide,modifier=Modifier.testTag("assistant-hide")){Text("Ocultar asistente")}
            TextButton(onClick=dismiss){Text("Cerrar")}
        }
        Text("Puedes volver a mostrarla desde Perfil → Mostrar la maestra de ProfeLab.",style=MaterialTheme.typography.bodySmall)
    }}
    val body:@Composable ()->Unit={Box(Modifier.testTag("assistant-panel").heightIn(max=if(tablet)720.dp else 660.dp).verticalScroll(rememberScrollState()).padding(20.dp)){text()}}
    if(tablet || reduceMotion)Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)){
        Surface(Modifier.widthIn(max=680.dp).fillMaxWidth().safeDrawingPadding(),shape=MaterialTheme.shapes.large,color=ProfeColors.Cream){body()}
    }else ModalBottomSheet(onDismissRequest=dismiss,containerColor=ProfeColors.Cream){body()}
}
