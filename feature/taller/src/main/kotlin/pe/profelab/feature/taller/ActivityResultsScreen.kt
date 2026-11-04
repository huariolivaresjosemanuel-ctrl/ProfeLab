package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

/** W05 shares the same fields, dated observations and private attachments as the existing editor. */
@Composable
internal fun ActivityResultsScreen(record:WorkRecord,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions,capture:(String)->Unit){
    val data=JSONObject(record.json)
    var section by rememberSaveable(record.id){mutableStateOf("Registro")}
    val experimental=view.experiment
    val dance=view.categoryId=="dance"
    val heritage=view.categoryId=="heritage"
    val title=when {experimental->"Resultados y observación";view.categoryId=="dance"->"Práctica y reflexión";else->"Producto y reflexión"}
    WorkshopPage(title,if(experimental)"Registra, compara y analiza tus observaciones"else "Conserva lo realizado, los ajustes y lo aprendido",back,screenTag="workshop-results"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("results-sections"){WorkshopChoices(listOf("Registro","Evidencias","Conclusión"),section){section=it}}
        if(!view.personal)item("readonly"){FlowCard("Consulta sin registros personales",WorkshopColors.Yellow,Icons.Outlined.Info){Text("Esta es una definición pública. No se crean mediciones ni evidencias al abrirla. Comienza tu copia para registrar lo que realmente ocurra.");FlowAction("Comenzar mi copia","workshop-results-begin",actions.begin,false)}}
        if(section=="Registro"){
            if(experimental)item("variables"){FlowCard("Pregunta y variables",WorkshopColors.Mint,Icons.Outlined.Science){
                Text(data.optString("researchQuestion").ifBlank {"Pregunta por definir en tu copia."})
                Text("Hipótesis propia: ${data.optString("hypothesis").ifBlank {"sin registrar"}}")
                listOf("independentVariable" to "Qué cambia","dependentVariable" to "Qué observas").forEach {(key,label)->data.optString(key).takeIf {it.isNotBlank()}?.let {Text("$label: $it")}}
                val controls=WorkshopCodec.text(data.opt("controlConditions")).ifBlank {data.optString("controls")};if(controls.isNotBlank())BriefText("Condiciones a mantener: $controls")
            }}
            item("observations"){FlowCard(if(experimental)"Registro de observaciones"else "Registro del proceso",WorkshopColors.Paper,Icons.Outlined.Assignment){
                if(view.personal)WorkshopObservations(record,vm)else Text("Tu copia podrá guardar registros con fecha, notas, medidas opcionales con su unidad y evidencias propias. Sin valores pregrabados.")
            }}
            item("results-text"){ResultTextField(record,"results",when {experimental->"Resultados observados";dance->"Práctica realizada";heritage->"Actividad realizada";else->"Producto realizado"},view.personal,vm)}
            item("outcome-reference"){FlowCard("Referencia visual opcional",WorkshopColors.Mint){WorkshopMediaSlot(record,"outcome_reference",modifier=Modifier.fillMaxWidth());Text("Una imagen editorial es una referencia ilustrativa, nunca evidencia de tus resultados.",style=MaterialTheme.typography.bodySmall)}}
        }
        if(section=="Evidencias")item("personal-evidence"){FlowCard("Evidencias fotográficas",WorkshopColors.Mint,Icons.Outlined.PhotoCamera){
            if(view.personal)WorkshopEvidence(record,vm,capture)else Text("Sin fotografías personales. Añadir evidencias es opcional y requiere iniciar una copia.")
        }}
        if(section=="Conclusión"){
            item("analysis"){ResultTextField(record,"analysis",when {experimental->"Mi análisis";dance->"Secuencia y ajustes";heritage->"Contexto y explicación";else->"Ajustes y explicación"},view.personal,vm)}
            item("limitations"){ResultTextField(record,"limitations",if(experimental)"Limitaciones y otros factores"else "Dificultades y adaptaciones",view.personal,vm)}
            item("conclusion"){ResultTextField(record,"conclusion",when {experimental->"Mi conclusión";dance || heritage->"Observación y reflexión";else->"Mi reflexión"},view.personal,vm)}
        }
        item("results-next"){FlowAction("Ver guía docente","workshop-results-next",{actions.navigate(WorkshopWindow.CLASSROOM)});FlowAction("Volver al procedimiento","workshop-results-steps",{actions.navigate(WorkshopWindow.STEPS)},false)}
        if(view.personal)item("results-save-state"){Text("Los cambios se guardan en este dispositivo. Si aparece un error, conserva el borrador y utiliza Reintentar.",style=MaterialTheme.typography.bodySmall)}
    }
}

@Composable
private fun ResultTextField(record:WorkRecord,field:String,label:String,editable:Boolean,vm:WorkshopViewModel){
    val text=JSONObject(record.json).optString(field)
    FlowCard(label,WorkshopColors.Paper){
        if(editable)OutlinedTextField(text,{value->if(value.length<=ActivityResultsData.MAX_TEXT)vm.editResult(record,field,value)},
            label={Text(label)},supportingText={Text("${text.length}/${ActivityResultsData.MAX_TEXT} · escribe solo tus observaciones")},minLines=3,
            modifier=Modifier.fillMaxWidth().testTag("workshop-result-field:$field"))
        else Text("Sin datos personales. Este campo se completa únicamente en tu copia.")
    }
}
