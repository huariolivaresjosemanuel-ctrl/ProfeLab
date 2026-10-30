package pe.profelab.feature.home

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import pe.profelab.core.model.Task
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TaskRow(task: Task, complete: (Boolean)->Unit, edit: ()->Unit) {
    Row(verticalAlignment=Alignment.CenterVertically,modifier=Modifier.fillMaxWidth()) {
        Checkbox(checked=task.completed,onCheckedChange=complete,modifier=Modifier.semantics { contentDescription="${if(task.completed) "Reabrir" else "Completar"} ${task.title}" })
        TextButton(onClick=edit,modifier=Modifier.weight(1f)) {
            Column(Modifier.fillMaxWidth()) {
                Text(task.title,style=MaterialTheme.typography.bodyLarge,textDecoration=if(task.completed) TextDecoration.LineThrough else null)
                Text(task.dueAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("d MMM · HH:mm",Locale.forLanguageTag("es-PE"))) } ?: "Sin fecha",style=MaterialTheme.typography.bodySmall)
                if (task.reminderEnabled) Text("Aviso activado · horario aproximado",style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
