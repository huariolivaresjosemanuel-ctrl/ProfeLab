package pe.profelab.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import pe.profelab.core.model.UserPreferences
import pe.profelab.core.model.LocalRole
import pe.profelab.core.ui.*

@Composable
fun ProfileScreen(vm: ProfileViewModel, state: ProfileState, preferences: UserPreferences, back: ()->Unit, about: ()->Unit, tools: ()->Unit = {},installation:@Composable ()->Unit={}) {
    BackHandler(enabled=!state.finished) { vm.leave() }
    LaunchedEffect(state.finished) { if(state.finished) back() }
    Page {
        PageTitle("Mi perfil","Educación Primaria",vm::leave)
        if(state.loading) { CircularProgressIndicator(); return@Page }
        state.error?.let { ErrorNotice(it) }
        if(state.id.isNotBlank()) {
            OutlinedTextField(state.name,{vm.update(name=it)},label={Text("Nombre o apodo (opcional)")},modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
            OutlinedTextField(state.cycle,{vm.update(cycle=it)},label={Text("Ciclo académico universitario (opcional)")},supportingText={Text("Es tu ciclo de estudios; el grado escolar se indicará en cada sesión.")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
            OutlinedTextField(state.institution,{vm.update(institution=it)},label={Text("Institución (opcional)")},modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
            Button(onClick=vm::save,enabled=!state.saving,modifier=Modifier.fillMaxWidth()) { Text(if(state.saving) "Guardando…" else "Guardar perfil") }
            if(state.saved) Text("Perfil guardado en este dispositivo")
            else if(state.draftSaved) Text("Borrador guardado. Confirma para actualizar tu perfil.")
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Mostrar tareas completadas en las listas",modifier=Modifier.weight(1f))
            Switch(checked=preferences.showCompleted,onCheckedChange=vm::showCompleted)
        }
        Text("Mi bienvenida",style=MaterialTheme.typography.titleMedium)
        ChoiceRow(LocalRole.entries.map {it.label},preferences.role.label){label->vm.role(LocalRole.entries.first {it.label==label})}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Saludo personalizado",Modifier.weight(1f));Switch(preferences.personalizedGreetings,{vm.mascot(personalizedGreeting=it)})}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Mostrar la maestra de ProfeLab",Modifier.weight(1f));Switch(preferences.mascotVisible,{vm.mascot(visible=it)})}
        Row(verticalAlignment=Alignment.CenterVertically){Text("Reducir movimiento",Modifier.weight(1f));Switch(preferences.reduceMotion,{vm.mascot(reduceMotion=it)})}
        installation()
        InfoCard("Tus datos son locales","No necesitas una cuenta. Evita datos identificables de menores. Antes de desinstalar, exporta un respaldo y comprueba su integridad desde Datos y contenidos.")
        OutlinedButton(onClick=about) { Text("Acerca de ProfeLab y créditos") }
        OutlinedButton(onClick=tools) { Text("Respaldo, paquetes y trabajos archivados") }
    }
}
