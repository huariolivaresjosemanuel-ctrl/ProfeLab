package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*

@Composable
internal fun ActivityPreparationPanel(record:WorkRecord,personal:Boolean,catalog:List<MaterialDefinition>,vm:WorkshopViewModel,begin:()->Unit,participants:@Composable (WorkRecord)->Unit){
    val state by vm.state.collectAsStateWithLifecycle()
    val prep=JSONObject(record.json).optJSONObject("teacherPreparation") ?: JSONObject()
    var mode by rememberSaveable(record.id){mutableStateOf(prep.optString("participation","INDIVIDUAL"))}
    var count by rememberSaveable(record.id){mutableStateOf(prep.optInt("participantCount",1).toString())}
    var teams by rememberSaveable(record.id){mutableStateOf(prep.optInt("teamCount",1).toString())}
    var error by remember {mutableStateOf<String?>(null)}
    var rules by rememberSaveable(record.id){mutableStateOf(false)}
    FlowCard("Preparar aula y materiales",WorkshopColors.Mint){
        if(!personal){
            Text("Puedes consultar materiales y guía sin registrar existencias ni alumnos. Para guardar tu preparación, comienza una copia.")
            TextButton(onClick=begin){Text("Guardar mi preparación en una copia")}
        }else{
            WorkshopChoices(listOf("Individualmente","Con alumnos"),if(mode=="STUDENTS")"Con alumnos"else "Individualmente"){mode=if(it=="Con alumnos")"STUDENTS"else "INDIVIDUAL"}
            if(mode=="STUDENTS"){
                OutlinedTextField(count,{count=it.take(3)},label={Text("Participantes sin nombres (1–100)")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(teams,{teams=it.take(3)},label={Text("Equipos previstos")},modifier=Modifier.fillMaxWidth())
                Text("Puedes continuar solo con estas cantidades; registrar alumnos es opcional.",style=MaterialTheme.typography.bodySmall)
            }
            TextButton(onClick={
                try{val n=if(mode=="INDIVIDUAL")1 else count.toInt();val t=if(mode=="INDIVIDUAL")1 else teams.toInt()
                    vm.patch(record){j->val fresh=WorkshopPreparation.participation(record.copy(json=j.toString()),mode,n,t);j.put("teacherPreparation",fresh.getJSONObject("teacherPreparation"))};error=null
                }catch(_:IllegalArgumentException){error="Revisa participantes y equipos: entre 1 y 100, sin más equipos que participantes."}
            }){Text("Guardar participación")}
            if(mode=="STUDENTS"){
                if(state.pending==0 && state.error==null)participants(record)else Text("Espera a que termine el guardado antes de seleccionar un aula.")
            }
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            val savedRules=prep.optJSONObject("materialRules")
            val stale=if(savedRules==null)false else savedRules.keys().asSequence().any {id->val rule=savedRules.optJSONObject(id) ?: return@any false
                when(rule.optString("basis")){"STUDENT"->rule.optInt("multiplier")!=prep.optInt("participantCount",1);"TEAM"->rule.optInt("multiplier")!=prep.optInt("teamCount",1);else->false}
            }
            if(stale)Text("Cambió la participación: revisa y vuelve a aplicar las reglas de cantidades antes de preparar materiales.",color=MaterialTheme.colorScheme.error)
            TextButton(onClick={rules=!rules}){Text(if(rules)"Cerrar reglas de cantidades"else "Ajustar cantidades con una regla explícita")}
            if(rules){
                Text("Sin regla confirmada se conservan las cantidades de la ficha. Si cambias participantes, revisa y vuelve a aplicar la regla.")
                WorkshopCodec.materials(JSONObject(record.json),catalog).forEach {line->PreparationMaterialRule(record,line,catalog,vm)}
            }
        }
    }
}

@Composable
private fun PreparationMaterialRule(record:WorkRecord,line:MaterialLine,catalog:List<MaterialDefinition>,vm:WorkshopViewModel){
    var basis by rememberSaveable(record.id,line.id){mutableStateOf("Común")}
    var quantity by rememberSaveable(record.id,line.id){mutableStateOf(line.amount)}
    var confirmed by rememberSaveable(record.id,line.id){mutableStateOf(false)}
    var error by remember {mutableStateOf<String?>(null)}
    Column {
        Text(line.name,style=MaterialTheme.typography.titleSmall)
        WorkshopChoices(listOf("Común","Por alumno","Por equipo","Original"),basis){basis=it;confirmed=false}
        OutlinedTextField(quantity,{quantity=it.take(30);confirmed=false},label={Text("Cantidad base (${line.unit})")},modifier=Modifier.fillMaxWidth())
        Row {Checkbox(confirmed,{confirmed=it});Text("Confirmo esta regla para mi preparación",Modifier.weight(1f))}
        TextButton(enabled=confirmed,onClick={
            val key=when(basis){"Por alumno"->"STUDENT";"Por equipo"->"TEAM";"Original"->"ORIGINAL";else->"COMMON"}
            try{vm.patch(record){j->val next=WorkshopPreparation.materialRule(record.copy(json=j.toString()),catalog,line.id,key,quantity,confirmed);j.put("materials",next.getJSONArray("materials")).put("teacherPreparation",next.getJSONObject("teacherPreparation"))};error=null}
            catch(_:IllegalArgumentException){error="Revisa cantidad y unidad. Las unidades referenciales se ajustan primero en el editor."}
        }){Text("Aplicar regla confirmada")}
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }
}
