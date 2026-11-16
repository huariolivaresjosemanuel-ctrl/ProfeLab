package pe.profelab.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.profelab.core.model.education.*
import pe.profelab.core.ui.InfoCard
import pe.profelab.core.ui.Page
import pe.profelab.core.ui.PageTitle
import pe.profelab.core.ui.ProfeColors

/** Never exposes the workspace if device-key protection could not finish atomically. */
@Composable
internal fun EducationStorageGate(graph:AppGraph,content:@Composable ()->Unit) {
    val protection=graph.educationProtection
    val state by protection.state.collectAsStateWithLifecycle()
    val scope=rememberCoroutineScope()
    // The process-owned coordinator shares this operation with welcome and survives rotation.
    // ensureReady never retries a previously failed attempt; only the explicit button can do so.
    LaunchedEffect(protection) { protection.ensureReady() }
    EducationStorageGateContent(state,{scope.launch {protection.retry()}},content)
}

/** No storage access or exception text in UI. READY is the only state that exposes content. */
@Composable
internal fun EducationStorageGateContent(state:EducationProtectionState,onRetry:()->Unit,content:@Composable ()->Unit){
    if(state.status==EducationProtectionStatus.READY){
        // Do not pad the actual app: its existing Scaffold owns its insets.
        content()
        return
    }
    Surface(Modifier.fillMaxSize(),color=ProfeColors.Cream){
        // windowInsetsPadding both applies and consumes safeDrawing, once, on the gate only.
        Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing).testTag("private-storage-gate")){
            Page {
                Box(Modifier.testTag("private-storage-title")){PageTitle("Protección de datos locales")}
                val loading=state.status==EducationProtectionStatus.VERIFYING
                val retryable=state.status==EducationProtectionStatus.RETRYABLE_ERROR
                val statusLabel=when(state.status){
                    EducationProtectionStatus.VERIFYING->"Verificando protección"
                    EducationProtectionStatus.RETRYABLE_ERROR->"No se completó la apertura protegida"
                    EducationProtectionStatus.UNRECOVERABLE_PROTECTED_ERROR->"Apertura protegida bloqueada"
                    EducationProtectionStatus.READY->"Espacio protegido listo"
                }
                Column(Modifier.fillMaxWidth().semantics {liveRegion=LiveRegionMode.Polite;stateDescription=statusLabel},verticalArrangement=Arrangement.spacedBy(12.dp)){
                    if(loading){
                        CircularProgressIndicator(Modifier.testTag("private-storage-progress"))
                        Text("Verificando la protección de los datos locales…")
                        Text(state.stage.progressMessage(),style=MaterialTheme.typography.bodyMedium)
                    }else {
                        InfoCard(statusLabel,state.stage.failureMessage())
                        if(retryable)Text("Puedes reintentar esta comprobación. El contenido seguirá bloqueado hasta que termine correctamente.")
                        else Text("La verificación del contenido o de su clave no pudo completarse de forma segura. ProfeLab mantiene el acceso bloqueado; no reemplazará la clave ni creará un espacio vacío para continuar.")
                        Text("No desinstales ProfeLab ni borres sus datos. Conserva la instalación y los respaldos que ya tengas; el código de abajo permite identificar la etapa sin mostrar tus registros.")
                        // Only enum identifiers enter the diagnostic. Never render exception.message,
                        // exception payloads, paths, profile data, keys, IVs or encrypted bytes.
                        Text("Código: P0_${state.stage.name}_${state.failureCode?.name ?: "UNCLASSIFIED"}",
                            Modifier.testTag("private-storage-diagnostic"),style=MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick=onRetry,enabled=retryable,modifier=Modifier.fillMaxWidth().testTag("private-storage-retry")){
                        Text(when {loading->"Verificando apertura protegida…";retryable->"Reintentar apertura protegida";else->"Apertura protegida bloqueada"})
                    }
                }
            }
        }
    }
}

private fun EducationProtectionStage.progressMessage():String=when(this){
    EducationProtectionStage.OPEN_DB->"Abriendo la base local para comprobar su estado."
    EducationProtectionStage.SET_SECURE_DELETE->"Comprobando la configuración de protección del almacenamiento."
    EducationProtectionStage.READ_LEGACY->"Comprobando el formato de los registros existentes."
    EducationProtectionStage.ENCRYPT_EDU->"Aplicando la protección de los registros locales."
    EducationProtectionStage.VERIFY_EDU->"Verificando que los registros protegidos se pueden autenticar."
    EducationProtectionStage.CLEANUP_STAGING->"Revisando los archivos temporales de las operaciones anteriores."
    EducationProtectionStage.READY->"Finalizando la comprobación."
}

private fun EducationProtectionStage.failureMessage():String=when(this){
    EducationProtectionStage.OPEN_DB->"No se pudo abrir la base local para verificarla."
    EducationProtectionStage.SET_SECURE_DELETE->"No se pudo comprobar la configuración de protección del almacenamiento."
    EducationProtectionStage.READ_LEGACY->"No se pudo comprobar el formato de los registros existentes."
    EducationProtectionStage.ENCRYPT_EDU->"No se pudo completar la protección de los registros locales."
    EducationProtectionStage.VERIFY_EDU->"No se pudo verificar la lectura autenticada del contenido protegido."
    EducationProtectionStage.CLEANUP_STAGING->"No terminó la revisión de archivos temporales. Esta etapa no confirma la apertura del espacio privado."
    EducationProtectionStage.READY->"No se pudo completar la apertura protegida."
}
