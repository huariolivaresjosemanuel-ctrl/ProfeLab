package pe.profelab.app

import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import pe.profelab.core.ui.InfoCard
import pe.profelab.core.ui.Page
import pe.profelab.core.ui.PageTitle

/** Never exposes the workspace if device-key protection could not finish atomically. */
@Composable
internal fun EducationStorageGate(graph:AppGraph,content:@Composable ()->Unit) {
    var ready by remember(graph) { mutableStateOf(false) }
    var failed by remember(graph) { mutableStateOf(false) }
    var attempt by remember(graph) { mutableIntStateOf(0) }
    LaunchedEffect(graph,attempt) {
        failed=false
        try {
            withContext(Dispatchers.IO) {
                graph.workspace.protectEducationRecords()
                graph.transfers.cleanupAbandonedStaging()
            }
            ready=true
        } catch(error:CancellationException) { throw error }
        catch(_:Exception) { failed=true }
    }
    if(ready)content() else Page {
        PageTitle("Protección de datos locales")
        if(failed) {
            InfoCard("No se pudo abrir el espacio privado","Los registros anteriores se conservan. Comprueba el bloqueo del dispositivo y el espacio disponible. No desinstales ProfeLab ni borres sus datos; vuelve a intentar la apertura.")
            Button(onClick={attempt++}) { Text("Reintentar apertura protegida") }
        } else {
            CircularProgressIndicator()
            Text("Preparando el almacenamiento privado…")
        }
    }
}
