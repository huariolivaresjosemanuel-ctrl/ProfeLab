package pe.profelab.feature.sessions

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import pe.profelab.core.model.*
import pe.profelab.core.ui.*

@Composable
fun SessionWorkspace(all:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,export:(WorkRecord)->Unit,capture:(String)->Unit,initialId:String?=null) {
    var selected by rememberSaveable(initialId){mutableStateOf(initialId)}
    var query by rememberSaveable {mutableStateOf("")};var grade by rememberSaveable {mutableStateOf("Todos")}
    var area by rememberSaveable {mutableStateOf("")};var state by rememberSaveable {mutableStateOf("Todos")}
    var remove by remember {mutableStateOf<WorkRecord?>(null)}
    var showFilters by rememberSaveable {mutableStateOf(false)};var templateMinutes by rememberSaveable {mutableStateOf("45")}
    val current=all.find {it.id==selected}
    if(current!=null) {
        RecordEditor(current,listOf(Field("date","Fecha de la sesión"),Field("purpose","Propósito"),Field("evidence","Evidencia de aprendizaje"),Field("criteria","Criterios"),Field("instrument","Instrumento de evaluación / rúbrica"),Field("competencyManual","Competencia: entrada del usuario"),Field("capacityManual","Capacidades: entrada del usuario"),Field("performanceManual","Desempeños: entrada del usuario"),Field("curriculumSource","Referencia, edición y página curricular para cotejo docente"),Field("start","Inicio: actividades"),Field("development","Desarrollo: actividades"),Field("close","Cierre: reflexión y consolidación"),Field("materials","Materiales y recursos"),Field("adaptations","Adecuaciones e inclusión"),Field("reflection","Observación posterior")),all,save,{selected=null},export,capture){data,change->
            Text("Grado escolar de Primaria",style=MaterialTheme.typography.titleMedium)
            ChoiceRow((1..6).map {it.toString()},data.optString("grade","1")){change(JSONObject(data.toString()).put("grade",it))}
            ChoiceRow(listOf("Matemática","Comunicación","Ciencia y Tecnología","Personal Social","Arte","Tutoría"),data.optString("area","Matemática")){change(JSONObject(data.toString()).put("area",it))}
            ChoiceRow(listOf("BORRADOR","EN_PROGRESO","REVISADA","ARCHIVADA"),data.optString("status","BORRADOR")){change(JSONObject(data.toString()).put("status",it))}
            JsonFields(data,listOf(Field("totalMinutes","Duración total (minutos)",1),Field("startMinutes","Inicio (minutos)",1),Field("developmentMinutes","Desarrollo (minutos)",1),Field("closeMinutes","Cierre (minutos)",1)),change)
            val total=data.optString("totalMinutes").toIntOrNull()
            val minutes=listOf("startMinutes","developmentMinutes","closeMinutes").map {data.optString(it).toIntOrNull()}
            if(total==null || total<=0 || minutes.any {it==null || it<0}) Text("Duraciones pendientes o inválidas; puedes conservar el borrador.")
            else if(minutes.filterNotNull().sum()!=total) Text("Revisa tiempos: la secuencia suma ${minutes.filterNotNull().sum()} minutos; el total es $total.",color=MaterialTheme.colorScheme.error)
            Text("ProfeLab no acredita alineación oficial de textos introducidos. Coteja cada competencia/capacidad/desempeño contra su fuente antes de usarla.")
            Row {Checkbox(data.optBoolean("curriculumVerifiedByUser"),{checked->change(JSONObject(data.toString()).put("curriculumVerifiedByUser",checked))},enabled=data.optString("curriculumSource").isNotBlank());Text("He cotejado mis textos con la edición y página indicadas")}
            TextButton(onClick={val r=current.copy(id=newId(),title=current.title+" (copia)");save(r);selected=r.id}){Text("Duplicar sesión")}
            TextButton(onClick={remove=current}){Text("Archivar sesión")}
            val revisions=all.filter {it.kind=="revision" && it.id.startsWith("history:${current.id}:")}
            revisions.take(10).forEach {old->TextButton(onClick={save(current.copy(json=old.json));selected=null}){Text("Recuperar versión ${old.revision} de esta sesión")}}
        }
    }else Page {
        PageTitle("Sesiones","Planifica tu clase")
        Text("Plantilla estructural offline · duración en minutos",style=MaterialTheme.typography.bodySmall)
        ChoiceRow(listOf("45","60","90"),templateMinutes){templateMinutes=it}
        Button(onClick={val total=templateMinutes.toInt();val r=WorkRecord(newId(),"session","",JSONObject().put("grade","1").put("area","Matemática").put("status","BORRADOR").put("totalMinutes",templateMinutes).put("startMinutes","10").put("developmentMinutes",(total-20).toString()).put("closeMinutes","10").toString(),System.currentTimeMillis());save(r);selected=r.id}){Text("Nueva sesión")}
        OutlinedTextField(query,{query=it},label={Text("Buscar sesiones")},modifier=Modifier.fillMaxWidth())
        TextButton(onClick={showFilters=!showFilters}){Text(if(showFilters)"Cerrar filtros" else "Grado, área y estado")}
        if(showFilters){
        ChoiceRow(listOf("Todos")+(1..6).map {it.toString()},grade){grade=it}
        OutlinedTextField(area,{area=it},label={Text("Filtrar área")},modifier=Modifier.fillMaxWidth())
        ChoiceRow(listOf("Todos","BORRADOR","EN_PROGRESO","REVISADA"),state){state=it}
        }
        val shown=all.filter {r->val j=JSONObject(r.json);r.kind=="session" && !r.archived && r.title.contains(query,true) && (grade=="Todos" || j.optString("grade")==grade) && j.optString("area").contains(area,true) && (state=="Todos" || j.optString("status")==state)}
        shown.firstOrNull()?.let {featured->
            val data=JSONObject(featured.json)
            Card(colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
                    Text(featured.title.ifBlank {"Sesión en preparación"},style=MaterialTheme.typography.titleLarge)
                    Text("${data.optString("grade")}.º de Primaria · ${data.optString("area")} · ${data.optString("status")}")
                    listOf(Triple("Propósito","purpose",ProfeColors.Mint),Triple("Evidencia","evidence",ProfeColors.Coral),Triple("Criterios","criteria",ProfeColors.Blue)).forEach {(title,key,color)->
                        Card(onClick={selected=featured.id},colors=CardDefaults.cardColors(containerColor=color)){
                            Column(Modifier.padding(12.dp)){Text(title,style=MaterialTheme.typography.titleMedium);Text(data.optString(key).ifBlank {"Completar en el borrador"})}
                        }
                    }
                    Text("Secuencia de la sesión · ${data.optString("totalMinutes")} minutos",style=MaterialTheme.typography.titleMedium)
                    listOf("Inicio" to "start","Desarrollo" to "development","Cierre" to "close").forEach {(label,key)->TextButton(onClick={selected=featured.id}){Text("$label (${data.optString(key+"Minutes")} min) · ${data.optString(key).take(160).ifBlank {"Completar"}}")}}
                    Button(onClick={export(featured)}){Text("Exportar borrador")}
                }
            }
        }
        shown.forEach {r->Card(onClick={selected=r.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Blue)){ListItem(headlineContent={Text(r.title.ifBlank {"Borrador sin título"})},supportingContent={val j=JSONObject(r.json);Text("${j.optString("grade")}.º Primaria · ${j.optString("area")} · ${j.optString("status")}")})}}
        if(shown.isEmpty()) InfoCard("Sin sesiones en esta vista","Crea una sesión o cambia los filtros. Se guarda como elaboración personal.")
    }
    remove?.let {r->AlertDialog(onDismissRequest={remove=null},title={Text("¿Archivar esta sesión?")},text={Text("No se eliminan los proyectos, notas ni documentos vinculados.")},confirmButton={TextButton(onClick={archive(r.id);remove=null;selected=null}){Text("Archivar")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
