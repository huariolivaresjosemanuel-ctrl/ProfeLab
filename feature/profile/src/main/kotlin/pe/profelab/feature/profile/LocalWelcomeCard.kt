package pe.profelab.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.*
import pe.profelab.core.ui.*

@Composable
fun LocalWelcomeCard(preferences:UserPreferences,busy:Boolean,error:String?,save:(String,LocalRole)->Unit){
    var name by rememberSaveable {mutableStateOf(preferences.displayName)}
    var role by rememberSaveable {mutableStateOf(preferences.role.name)}
    Card(colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)){
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
            if(preferences.mascotVisible)CanonicalMascot(Modifier.fillMaxWidth().height(230.dp),preferences.reduceMotion,entrance=true)
            Text("¡Hola! Soy tu compañera de ProfeLab",style=MaterialTheme.typography.titleLarge)
            Text("Prepara materiales, organiza tus clases y conserva tus ideas. ¿Cómo te gustaría que te llame?")
            OutlinedTextField(name,{name=it.filterNot {character->character.isISOControl()}.take(60)},label={Text("Nombre o apodo (opcional)")},enabled=!busy,modifier=Modifier.fillMaxWidth())
            ChoiceRow(LocalRole.entries.map {it.label},LocalRole.valueOf(role).label){label->role=LocalRole.entries.first {it.label==label}.name}
            Text("Solo en este dispositivo, sin cuenta ni contraseña. Puedes cambiarlo desde Perfil.",style=MaterialTheme.typography.bodySmall)
            error?.let {ErrorNotice(it)}
            Button(enabled=!busy,onClick={save(name,LocalRole.valueOf(role))},modifier=Modifier.fillMaxWidth()){Text(if(busy)"Guardando…"else "Guardar mi bienvenida")}
            TextButton(enabled=!busy,onClick={save("",LocalRole.UNSPECIFIED)}){Text("Continuar sin personalizar")}
        }
    }
}
