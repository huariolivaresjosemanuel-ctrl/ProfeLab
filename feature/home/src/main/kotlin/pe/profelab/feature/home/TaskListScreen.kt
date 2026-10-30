package pe.profelab.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.*
import pe.profelab.core.ui.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskListScreen(state: HomeState, initialFilter: TaskFilter, showCompleted: Boolean,
    newTask: ()->Unit, edit: (String)->Unit, complete: (Task,Boolean)->Unit, back: ()->Unit) {
    var selected by rememberSaveable(initialFilter) { mutableStateOf(initialFilter.name) }
    var query by rememberSaveable { mutableStateOf("") }
    val filter=TaskFilter.valueOf(selected)
    Page {
        PageTitle(if(filter==TaskFilter.REMINDERS) "Avisos locales" else "Mis tareas",back=back)
        if(state.loading) { CircularProgressIndicator(); return@Page }
        if(state.error!=null) { ErrorNotice(state.error); return@Page }
        Button(onClick=newTask) { Text("Añadir tarea") }
        OutlinedTextField(query,{query=it},label={Text("Buscar tarea")},modifier=Modifier.fillMaxWidth())
        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            TaskFilter.entries.forEach { f -> FilterChip(selected=f==filter,onClick={selected=f.name},label={Text(f.label)}) }
        }
        val filtered=HomeRules.filter(state.tasks,filter,state.now.toLocalDate(),state.now.zone).filter {
            (showCompleted || filter in setOf(TaskFilter.DONE,TaskFilter.WEEK) || !it.completed) &&
                (it.title.contains(query,true) || it.notes.contains(query,true))
        }
        if(filtered.isEmpty()) InfoCard("Sin tareas en esta vista","Añade una tarea o cambia el filtro. Los avisos solo se programan cuando los activas.")
        filtered.forEach { task -> TaskRow(task,{complete(task,it)},{edit(task.id)}); HorizontalDivider() }
    }
}
