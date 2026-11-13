package pe.profelab.core.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.AssistantAction
import pe.profelab.core.model.AssistantMessage

@Composable
fun TallerWelcomeDialog(greeting:String,actions:List<AssistantMessage>,reduceMotion:Boolean,onAction:(AssistantAction)->Unit,dismiss:()->Unit){
    AlertDialog(onDismissRequest=dismiss,modifier=Modifier.testTag("taller-welcome"),
        title={Text(greeting)},
        text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)){
            Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                CanonicalMascot(Modifier.width(72.dp).height(100.dp),reduceMotion=reduceMotion,gesture=MascotGesture.WAVE)
                Text("Puedes explorar, preparar materiales o retomar tu trabajo. No necesitas registrar inventario ni alumnos para empezar.",modifier=Modifier.weight(1f))
            }
            actions.take(3).forEach {message->OutlinedButton(onClick={onAction(message.action)},modifier=Modifier.fillMaxWidth().testTag("welcome-${message.action.name}")){Text(message.label)}}
        }},
        confirmButton={TextButton(onClick=dismiss,modifier=Modifier.testTag("taller-welcome-skip")){Text("Omitir bienvenida")}})
}
