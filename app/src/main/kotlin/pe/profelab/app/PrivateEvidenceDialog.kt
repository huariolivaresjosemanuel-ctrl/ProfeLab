package pe.profelab.app

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Photo review stays inside ProfeLab; no public URI, share Intent or screenshot is created. */
@Composable
internal fun PrivateEvidenceDialog(graph:AppGraph,id:String,dismiss:()->Unit) {
    var bitmap by remember(id) { mutableStateOf<Bitmap?>(null) }
    var failed by remember(id) { mutableStateOf(false) }
    LaunchedEffect(graph,id) {
        try {
            bitmap=withContext(Dispatchers.IO) {
                val file=graph.files.file(id)
                val bounds=BitmapFactory.Options().apply { inJustDecodeBounds=true }
                BitmapFactory.decodeFile(file.absolutePath,bounds)
                require(bounds.outWidth>0 && bounds.outHeight>0)
                var sample=1
                while(bounds.outWidth/sample>1200 || bounds.outHeight/sample>1200)sample*=2
                requireNotNull(BitmapFactory.decodeFile(file.absolutePath,BitmapFactory.Options().apply { inSampleSize=sample }))
            }
        } catch(error:CancellationException) { throw error }
        catch(_:Exception) { failed=true }
    }
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(securePolicy=SecureFlagPolicy.SecureOn)) {
        Surface(shape=MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("Evidencia privada",style=MaterialTheme.typography.titleLarge)
                when {
                    failed->Text("La imagen no está disponible en este dispositivo. Se conserva su vínculo histórico; revisa un respaldo que incluya los archivos privados.")
                    bitmap!=null->Image(requireNotNull(bitmap).asImageBitmap(),"Fotografía privada seleccionada para revisión docente",Modifier.fillMaxWidth().heightIn(min=120.dp,max=400.dp),contentScale=ContentScale.Fit)
                    else->CircularProgressIndicator()
                }
                Text("Consulta local. Esta vista no comparte ni adjunta la fotografía a un informe.",style=MaterialTheme.typography.bodySmall)
                TextButton(onClick=dismiss) { Text("Cerrar evidencia") }
            }
        }
    }
}
