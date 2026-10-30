package pe.profelab.feature.home

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskEditorScreen(vm: TaskEditorViewModel, state: TaskEditorState, isNew: Boolean, back: ()->Unit) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    var permissionDenied by rememberSaveable { mutableStateOf(false) }
    val context=LocalContext.current
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionDenied=!granted
        vm.update { it.copy(reminderEnabled=granted) }
    }
    BackHandler(enabled=!state.finished) { vm.leave() }
    LaunchedEffect(state.finished) { if(state.finished) back() }
    Page {
        PageTitle(if(isNew) "Nueva tarea" else "Editar tarea",back=vm::leave)
        if(state.loading) { CircularProgressIndicator(); return@Page }
        state.error?.let { ErrorNotice(it) }
        val task=state.task ?: return@Page
        OutlinedTextField(task.title,{text -> vm.update { it.copy(title=text.take(160)) }},label={Text("Título")},modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
        OutlinedTextField(task.notes,{text -> vm.update { it.copy(notes=text.take(10_000)) }},label={Text("Notas (opcional)")},minLines=3,modifier=Modifier.fillMaxWidth(),enabled=!state.saving)
        Text("Fecha de entrega",style=MaterialTheme.typography.titleMedium)
        val zone=ZoneId.systemDefault()
        val selected=task.dueAt?.let { Instant.ofEpochMilli(it).atZone(zone) }
        OutlinedButton(enabled=!state.saving,onClick={
            val initial=selected?.toLocalDate() ?: LocalDate.now()
            DatePickerDialog(context,{_,year,month,day ->
                val date=LocalDate.of(year,month+1,day)
                vm.update { it.copy(dueAt=date.atTime(selected?.toLocalTime() ?: LocalTime.of(18,0)).atZone(zone).toInstant().toEpochMilli()) }
            },initial.year,initial.monthValue-1,initial.dayOfMonth).show()
        }) { Text(selected?.format(DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy",Locale.forLanguageTag("es-PE"))) ?: "Elegir fecha") }
        if(selected != null) {
            OutlinedButton(enabled=!state.saving,onClick={
                TimePickerDialog(context,{_,hour,minute ->
                    vm.update { it.copy(dueAt=selected.toLocalDate().atTime(hour,minute).atZone(zone).toInstant().toEpochMilli()) }
                },selected.hour,selected.minute,true).show()
            }) { Text("Hora: ${selected.format(DateTimeFormatter.ofPattern("HH:mm"))}") }
            TextButton(onClick={vm.update { it.copy(dueAt=null,reminderEnabled=false) }},enabled=!state.saving) { Text("Quitar fecha") }
        }
        Text("Prioridad",style=MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Priority.entries.forEach { p -> FilterChip(selected=task.priority==p,onClick={vm.update { it.copy(priority=p) }},label={Text(p.label)},enabled=!state.saving) }
        }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Aviso local",style=MaterialTheme.typography.titleMedium)
                Text("Android puede retrasarlo por ahorro de batería. Necesita fecha y hora.",style=MaterialTheme.typography.bodyMedium)
            }
            Switch(checked=task.reminderEnabled,enabled=task.dueAt!=null && !state.saving,onCheckedChange={enabled ->
                if(!enabled) vm.update { it.copy(reminderEnabled=false) }
                else if(Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(context,Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED) {
                    permissionDenied=false
                    vm.update { it.copy(reminderEnabled=true) }
                } else permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        if(permissionDenied) InfoCard("Avisos desactivados","No se concedió el permiso. La tarea puede guardarse y consultarse sin notificaciones.")
        Text(if(state.draftSaved) "Borrador guardado en este dispositivo" else "${if(state.saving) "Guardando tarea…" else "Los cambios se guardan como borrador"}",style=MaterialTheme.typography.bodySmall)
        Button(onClick=vm::save,enabled=!state.saving,modifier=Modifier.fillMaxWidth()) { Text("Guardar tarea") }
        TextButton(onClick={confirmDiscard=true},enabled=!state.saving) { Text("Descartar borrador") }
        if(!isNew) TextButton(onClick={confirmDelete=true},enabled=!state.saving) { Text("Eliminar tarea") }
    }
    if(confirmDelete) AlertDialog(onDismissRequest={confirmDelete=false},title={Text("¿Eliminar esta tarea?")},text={Text("Se quitará la tarea y su aviso. Los recursos vinculados se conservan.")},confirmButton={TextButton(onClick={confirmDelete=false;vm.delete()}) { Text("Eliminar") }},dismissButton={TextButton(onClick={confirmDelete=false}) { Text("Cancelar") }})
    if(confirmDiscard) AlertDialog(onDismissRequest={confirmDiscard=false},title={Text("¿Descartar el borrador?")},text={Text("Se perderán los cambios sin confirmar. La tarea guardada anteriormente se conserva.")},confirmButton={TextButton(onClick={confirmDiscard=false;vm.discard()}) { Text("Descartar") }},dismissButton={TextButton(onClick={confirmDiscard=false}) { Text("Seguir editando") }})
}
