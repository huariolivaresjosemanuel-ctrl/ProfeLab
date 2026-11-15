package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import pe.profelab.core.content.EducationProgress
import pe.profelab.core.content.WorkshopClosure
import pe.profelab.core.model.*

@Composable
internal fun ActivityClosurePanel(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,saved:(String)->Unit={}){
    val execution=if(EducationProgress.hasSelection(record))EducationProgress.selectedId(record)else ""
    val operation=rememberSaveable(record.id,execution){newId()}
    var closing by remember {mutableStateOf(false)}
    var confirmed by rememberSaveable(record.id,execution){mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    val scope=rememberCoroutineScope();val state by vm.state.collectAsStateWithLifecycle()
    FlowCard("Cierre y reflexión",WorkshopColors.Mint){
        Text("${view.completedSteps} de ${view.steps.size} pasos marcados. Finalizar no completa pasos ni consume materiales.")
        if(WorkshopClosure.savedAt(record)>0)Text("Esta actividad tiene un cierre guardado para el contexto seleccionado.",style=MaterialTheme.typography.bodySmall)
        if(confirmed && state.pending==0 && state.error==null)Text("Cierre guardado",Modifier.testTag("workshop-close-confirmed"))
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(enabled=!closing && state.pending==0 && state.error==null,onClick={
            closing=true;confirmed=false;error=null
            scope.launch {try {
                confirmed=vm.finish(record,operation)
                if(confirmed)saved(operation)
                if(!confirmed)error="No se confirmó el cierre. Conserva el borrador y resuelve el aviso de guardado."
            }finally{closing=false}}
        },modifier=Modifier.fillMaxWidth().testTag("workshop-save-and-finish")){Text(if(closing)"Guardando…"else "Guardar y finalizar")}
        TextButton(enabled=!closing,onClick={scope.launch {confirmed=false;if(!vm.flush())error="Hay cambios pendientes; revisa el aviso de guardado."else error=null}}){Text("Guardar y continuar editando")}
    }
}
