package pe.profelab.feature.students

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore

/** Reuses workshopFlow.observedCriterionIds/teacherReflection, without creating an aula/run. */
@Composable
internal fun GeneralActivityReview(record:WorkRecord,store:WorkspaceStore,dismiss:()->Unit){
    val criteria=remember(record.id,record.json){WorkshopFlowAdapter.read(record).criteria}
    val flow=JSONObject(record.json).optJSONObject("workshopFlow") ?: JSONObject()
    var selectedJson by rememberSaveable(record.id){mutableStateOf((flow.optJSONArray("observedCriterionIds") ?: JSONArray()).toString())}
    var note by rememberSaveable(record.id){mutableStateOf(flow.optString("teacherReflection"))}
    var touchedJson by rememberSaveable(record.id){mutableStateOf("[]")}
    var noteChanged by rememberSaveable(record.id){mutableStateOf(false)}
    var more by rememberSaveable(record.id){mutableStateOf(false)}
    val actions=rememberEducationActions();val selected=WorkshopCodec.strings(JSONArray(selectedJson)).toSet()
    Text("Observaciones rápidas",style=MaterialTheme.typography.titleLarge)
    Text("Registro general de la actividad. No se atribuye a estudiantes ni equipos y no genera calificaciones.",style=MaterialTheme.typography.bodySmall)
    EducationState(actions)
    if(criteria.isEmpty())Text("No hay indicadores definidos. Puedes guardar una reflexión sin inventar criterios.")
    (if(more)criteria else criteria.take(3)).forEach {criterion->Row {
        Checkbox(criterion.id in selected,{checked->selectedJson=JSONArray((if(checked)selected+criterion.id else selected-criterion.id).toList()).toString();touchedJson=JSONArray((WorkshopCodec.strings(JSONArray(touchedJson))+criterion.id).distinct()).toString()},enabled=!actions.busy,modifier=Modifier.testTag("workshop-general-criterion:${criterion.id}"))
        Text(criterion.label,Modifier.weight(1f))
    }}
    if(criteria.size>3)TextButton(onClick={more=!more}){Text(if(more)"Ver menos criterios"else "Ver todos los criterios (${criteria.size})")}
    Text("Marca solo lo que observaste. Sin marcar significa «no observado», no desaprobación.",style=MaterialTheme.typography.bodySmall)
    OutlinedTextField(note,{note=it.take(6000);noteChanged=true},enabled=!actions.busy,label={Text("Mi observación (opcional)")},modifier=Modifier.fillMaxWidth(),minLines=2)
    Button(enabled=!actions.busy,onClick={actions.perform {
        store.editRecords {rows->
            val latest=requireNotNull(rows.singleOrNull {it.id==record.id}) {"La copia ya no está disponible"}
            val ids=WorkshopCodec.strings(JSONArray(touchedJson)).toSet()
            listOf(latest.copy(json=WorkshopFormativeData.update(latest,ids,selected.intersect(ids),note.takeIf {noteChanged}).toString(),updatedAt=System.currentTimeMillis()))
        }
        dismiss()
    }},modifier=Modifier.fillMaxWidth().testTag("workshop-save-general-observation")){Text("Guardar observación")}
    TextButton(enabled=!actions.busy,onClick=dismiss){Text("Continuar sin guardar")}
}
