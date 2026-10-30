package pe.profelab.feature.home

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ActivityScreen(state: HomeState, open: (EntityRef)->Unit, back: ()->Unit) {
    Page {
        PageTitle("Actividad reciente","Últimos 100 cambios guardados",back)
        if(state.loading) CircularProgressIndicator()
        state.error?.let { ErrorNotice(it) }
        if(!state.loading && state.error==null && state.activity.isEmpty()) InfoCard("Aún no hay actividad","Tus acciones aparecerán aquí cuando guardes tareas o el perfil.")
        state.activity.forEach { event ->
            TextButton(onClick={open(event.target)}) { Text(event.label) }
            Text(Instant.ofEpochMilli(event.at).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),style=MaterialTheme.typography.bodySmall)
            HorizontalDivider()
        }
    }
}
