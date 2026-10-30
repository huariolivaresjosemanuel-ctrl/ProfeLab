package pe.profelab.feature.library

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pe.profelab.core.ui.*

@Composable
fun LibraryScreen(state: LibraryState, retry: ()->Unit, openSource: (String)->Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    Page {
        PageTitle("Biblioteca","Guías y recursos oficiales")
        InfoCard("Referencias para tu práctica docente","Puedes consultar los títulos y abrir su fuente oficial en el navegador. La importación de PDFs y el lector estarán disponibles en una próxima versión.")
        OutlinedTextField(query,{query=it},label={Text("Buscar título, autor o área")},modifier=Modifier.fillMaxWidth())
        if(state.loading) CircularProgressIndicator()
        state.error?.let { ErrorNotice(it,retry) }
        val filtered=state.sources.filter { listOf(it.title,it.author,it.area,it.year.toString()).any { field -> field.contains(query,true) } }
        if(!state.loading && state.error==null) Text("${filtered.size} referencias · ningún PDF local",style=MaterialTheme.typography.titleMedium)
        filtered.forEach { source ->
            Card(onClick={selectedId=source.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                    Text(source.title,style=MaterialTheme.typography.titleMedium)
                    Text("${source.year} · ${source.area}",style=MaterialTheme.typography.bodyMedium)
                    Text("Solo referencia · PDF no importado",style=MaterialTheme.typography.labelLarge)
                    Text("Ver información de la fuente",style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        if(filtered.isEmpty() && !state.loading && state.error==null) InfoCard("Sin coincidencias","Prueba con otra palabra o área.")
    }
    state.sources.find { it.id==selectedId }?.let { source ->
        AlertDialog(onDismissRequest={selectedId=null},title={Text(source.title)},text={
            Column(Modifier.heightIn(max=460.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(source.author)
                Text(source.description)
                Text("Derechos pendientes de revisión: ${source.rightsNote}")
                Text("Abrir la fuente utiliza tu navegador y puede necesitar conexión.")
            }
        },confirmButton={TextButton(onClick={openSource(source.officialUrl)}) { Text("Abrir fuente oficial") }},dismissButton={TextButton(onClick={selectedId=null}) { Text("Cerrar") }})
    }
}
