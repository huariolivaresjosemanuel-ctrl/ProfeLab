package pe.profelab.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import android.net.Uri
import kotlinx.coroutines.*
import pe.profelab.core.ui.*
import java.io.File

@Composable
fun ExportPreview(file:File,graph:AppGraph,openPdf:()->Unit,back:()->Unit) {
    val scope=rememberCoroutineScope();var reviewed by remember {mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
    val destination=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")){uri->if(uri!=null)scope.launch {try{graph.files.writeTo(file,uri.toString());error="Copia guardada en el destino elegido."}catch(e:CancellationException){throw e}catch(_:Exception){error="No se pudo guardar. La copia temporal y el original se conservan."}}}
    Page {PageTitle("Vista previa de exportación",back=back)
        Text("La copia puede contener datos personales. Ábrela y revisa texto, páginas y marcas antes de compartir.")
        error?.let {InfoCard("Resultado",it)}
        Button(onClick=openPdf){Text("Abrir copia para revisarla")}
        Row {Checkbox(reviewed,{reviewed=it});Text("He revisado la copia y autorizo exportar su contenido")}
        Button(enabled=reviewed,onClick={destination.launch("ProfeLab-documento.pdf")}){Text("Guardar copia PDF")}
        OutlinedButton(enabled=reviewed,onClick={try{graph.files.share(file)}catch(_:Exception){error="No se pudo abrir el selector para compartir."}}){Text("Compartir copia con Android")}
    }
}
