package pe.profelab.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.*
import pe.profelab.core.ui.*

@Composable
fun HomeScreen(state: HomeState, openSection: (String) -> Unit, openTasks: (TaskFilter) -> Unit,
    openTask: (String) -> Unit, openActivity: () -> Unit, openProfile: () -> Unit,
    complete: (Task,Boolean) -> Unit, retry: () -> Unit, openResource: (EntityRef)->Unit,
    preferences:UserPreferences=UserPreferences(),welcome:@Composable ()->Unit={}) {
    Page {
        if (state.loading) { CircularProgressIndicator(); return@Page }
        if (state.error != null) { ErrorNotice(state.error,retry); return@Page }
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                Text(PersonalizationRules.greeting(state.now.hour,preferences,state.profile?.name.orEmpty()),style=MaterialTheme.typography.headlineSmall)
                Text(state.profile?.academicCycle?.let { "$it.º ciclo · Educación Primaria" } ?: "Tu espacio para enseñar y crear",style=MaterialTheme.typography.bodyLarge)
                Text("Pequeñas ideas, grandes aprendizajes",style=MaterialTheme.typography.bodyMedium,color=ProfeColors.Muted)
            }
            if (LocalDensity.current.fontScale < 1.4f){
                if(preferences.mascotVisible && preferences.onboardingCompleted)CanonicalMascot(Modifier.width(86.dp).height(108.dp),preferences.reduceMotion,entrance=true)
                else DeskPlant(Modifier.size(86.dp))
            }
        }
        welcome()
        if (state.profile == null) TextButton(onClick=openProfile) { Text("Configurar mi perfil académico") }
        val progress = HomeRules.progress(state.tasks,state.now.toLocalDate(),state.now.zone)
        Card(onClick={openTasks(TaskFilter.WEEK)},colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)) {
            Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Outlined.BarChart,null,Modifier.size(32.dp),tint=ProfeColors.Green)
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text("Tu avance esta semana",style=MaterialTheme.typography.titleMedium)
                    if (progress.total == 0) Text("Aún no tienes tareas con fecha esta semana.",style=MaterialTheme.typography.bodyMedium)
                    else {
                        LinearProgressIndicator(progress={progress.fraction},modifier=Modifier.fillMaxWidth())
                        Text("${progress.done}/${progress.total} completadas · ¡Sigue así!")
                    }
                    Text("Lunes a domingo · según fecha de entrega",style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        BoxWithConstraints {
            val twoColumns = maxWidth >= 300.dp && LocalDensity.current.fontScale < 1.3f
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                if (twoColumns) {
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        SoftCard("Próximas entregas","Tareas, informes y evidencias",Icons.Outlined.Assignment,ProfeColors.Coral,Modifier.weight(1f)) { openTasks(TaskFilter.UPCOMING) }
                        SoftCard("Mi taller","Crea materiales para tus clases",Icons.Outlined.Brush,ProfeColors.Mint,Modifier.weight(1f)) { openSection("taller") }
                    }
                    Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                        SoftCard("Sesiones","Diseña y organiza tus sesiones",Icons.Outlined.Groups,ProfeColors.Blue,Modifier.weight(1f)) { openSection("sessions") }
                        SoftCard("Prácticas","Tu experiencia en la escuela",Icons.Outlined.AssignmentTurnedIn,ProfeColors.Paper,Modifier.weight(1f)) { openSection("practices") }
                    }
                } else {
                    SoftCard("Próximas entregas","Tareas, informes y evidencias",Icons.Outlined.Assignment,ProfeColors.Coral) { openTasks(TaskFilter.UPCOMING) }
                    SoftCard("Mi taller","Crea materiales para tus clases",Icons.Outlined.Brush,ProfeColors.Mint) { openSection("taller") }
                    SoftCard("Sesiones","Diseña y organiza tus sesiones",Icons.Outlined.Groups,ProfeColors.Blue) { openSection("sessions") }
                    SoftCard("Prácticas","Tu experiencia en la escuela",Icons.Outlined.AssignmentTurnedIn,ProfeColors.Paper) { openSection("practices") }
                }
                SoftCard("Biblioteca MINEDU","Guías, currículos y recursos oficiales",Icons.AutoMirrored.Outlined.MenuBook,ProfeColors.Mint) { openSection("library") }
            }
        }
        BoxWithConstraints {
            val todayTasks = HomeRules.filter(state.tasks,TaskFilter.TODAY,state.now.toLocalDate(),state.now.zone)
            if (maxWidth >= 340.dp && LocalDensity.current.fontScale < 1.3f) {
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    ActivityCard(state.activity.take(4),Modifier.weight(1f),openActivity,openResource)
                    TodayCard(todayTasks,Modifier.weight(1f),complete,openTask) { openTasks(TaskFilter.TODAY) }
                }
            } else Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                ActivityCard(state.activity.take(3),Modifier,openActivity,openResource)
                TodayCard(todayTasks,Modifier,complete,openTask) { openTasks(TaskFilter.TODAY) }
            }
        }
    }
}

@Composable
private fun ActivityCard(events: List<ActivityEvent>, modifier: Modifier, all: () -> Unit, openResource: (EntityRef)->Unit) {
    Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text("Actividad reciente",style=MaterialTheme.typography.titleMedium)
            TextButton(onClick=all) { Text("Ver todo") }
            if (events.isEmpty()) Text("Aquí aparecerán tus tareas y trabajos guardados.",style=MaterialTheme.typography.bodyMedium)
            events.forEach { event ->
                TextButton(onClick={openResource(event.target)}) { Text(event.label) }
            }
        }
    }
}
@Composable
private fun TodayCard(tasks: List<Task>, modifier: Modifier, complete: (Task,Boolean)->Unit, edit: (String)->Unit, all: ()->Unit) {
    Card(modifier.fillMaxWidth(),colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(12.dp)) {
            Text("Pendientes de hoy",style=MaterialTheme.typography.titleMedium)
            TextButton(onClick=all) { Text("Organizar tareas") }
            if (tasks.isEmpty()) Text("Sin entregas para hoy. Puedes añadir una tarea.",style=MaterialTheme.typography.bodyMedium)
            tasks.take(5).forEach { task -> TaskRow(task,{complete(task,it)},{edit(task.id)}) }
        }
    }
}
