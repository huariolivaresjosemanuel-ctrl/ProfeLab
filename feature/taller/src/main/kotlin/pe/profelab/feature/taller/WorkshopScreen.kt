package pe.profelab.feature.taller

import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.graphics.BitmapFactory
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.model.*
import pe.profelab.core.ui.*
import java.time.Instant

@Composable
fun WorkshopScreen(all:List<WorkRecord>,templates:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,
    export:(WorkRecord)->Unit,capture:(String)->Unit,initialId:String?=null) {
    var section by rememberSaveable {mutableStateOf("Materiales")}
    var selected by rememberSaveable(initialId) {mutableStateOf(initialId)}
    var query by rememberSaveable {mutableStateOf("")};var grade by rememberSaveable {mutableStateOf("Todos")}
    var area by rememberSaveable {mutableStateOf("")};var maxTime by rememberSaveable {mutableStateOf("")}
    var difficulty by rememberSaveable {mutableStateOf("")};var resource by rememberSaveable {mutableStateOf("")}
    var onlyFav by rememberSaveable {mutableStateOf(false)};var inventory by rememberSaveable {mutableStateOf(false)}
    var shopping by rememberSaveable {mutableStateOf(false)}
    var showFilters by rememberSaveable {mutableStateOf(false)};var category by rememberSaveable {mutableStateOf("")}
    var safetyFilter by rememberSaveable {mutableStateOf("Todas")}
    var pendingDelete by remember {mutableStateOf<WorkRecord?>(null)}
    val sources=templates+all.filter {it.kind in setOf("catalog_project","catalog_experiment") && !it.archived}
    val current=(all+sources).find {it.id==selected}
    fun create(kind:String,source:WorkRecord?=null) {
        val data=JSONObject(source?.json ?: "{}").put("forkedFromTemplateId",source?.id).put("status","BORRADOR")
        val r=WorkRecord(newId(),kind,source?.title.orEmpty(),data.toString(),System.currentTimeMillis());save(r);selected=r.id
    }
    if(inventory) {InventoryScreen(all,save,archive){inventory=false};return}
    if(shopping) {
        val requirements=all.filter {it.kind in setOf("project","experiment") && !it.archived}.map {supplies(JSONObject(it.json))}
        val missing=InventoryRules.shopping(requirements,stock(all,all.filter {it.kind in setOf("project","experiment") && !it.archived}.map {it.id}.toSet()))
        val stored=all.find {it.kind=="shopping" && !it.archived}
        var text by remember {mutableStateOf(stored?.let {JSONObject(it.json).optString("text")} ?: missing.joinToString("\n") {"${it.name}: ${it.quantity ?: "comprobar cantidad"} ${it.unit}"})}
        Page {PageTitle("Lista de compras",back={shopping=false});Text("Consolidada de tus proyectos; las herramientas se cuentan como reutilizables.");OutlinedTextField(text,{text=it},minLines=8,modifier=Modifier.fillMaxWidth());Button(onClick={save(WorkRecord(stored?.id ?: newId(),"shopping","Lista de compras",JSONObject().put("text",text).toString(),System.currentTimeMillis()))}){Text("Guardar lista editable")}}
        return
    }
    if(current!=null) {
        val isTemplate=current.kind.contains("template") || current.kind.startsWith("catalog_")
        if(isTemplate) {
            Page {
                PageTitle(current.title,back={selected=null});TemplatePedagogy(JSONObject(current.json))
                Text("Plantilla editorial inmutable. Requiere validación docente y de seguridad.")
                Text(materialText(JSONObject(current.json)))
                val steps=JSONObject(current.json).optJSONArray("steps") ?: JSONArray()
                for(i in 0 until steps.length()) Text("${i+1}. ${steps.getJSONObject(i).optString("title")}")
                Button(onClick={create(if(current.kind.contains("experiment"))"experiment" else "project",current)}){Text("Crear mi copia de trabajo")}
            };return
        }
        val fields=mutableListOf(Field("objectiveSuggested","Objetivo"),Field("notes","Notas e ideas"),Field("variants","Variantes y usos"),Field("alternatives","Alternativas de material y justificación docente (no sustituyen inventario automáticamente)"),Field("adaptations","Adaptaciones por barreras"),Field("sources","Fuentes y citas; ubicación solo si verificada"))
        if(current.kind=="experiment") fields+=listOf(Field("researchQuestion","Pregunta de indagación"),Field("hypothesis","Hipótesis del usuario"),Field("independentVariable","Variable independiente"),Field("dependentVariable","Variable dependiente"),Field("controls","Condiciones controladas"),Field("results","Resultados reales"),Field("analysis","Interpretación y límites"),Field("conclusion","Conclusión del usuario"))
        RecordEditor(current,fields,all,save,{selected=null},export,capture) {data,change->
            TemplatePedagogy(data)
            var material by remember(current.id) {mutableStateOf(materialText(data))}
            OutlinedTextField(material,{material=it;change(JSONObject(data.toString()).put("materials",parseMaterials(it)))},label={Text("Material|cantidad|unidad|consumible o herramienta (una línea por material)")},minLines=4,modifier=Modifier.fillMaxWidth())
            val missing=InventoryRules.missing(supplies(data),stock(all,setOf(current.id)))
            val safety=data.optJSONObject("safety")
            val approved=data.optBoolean("safetyReviewedByUser")
            val risk=safety?.optString("riskLevel").orEmpty()+safety?.optString("level").orEmpty()
            val classification=when {
                risk.contains("PROHIBITED") || !approved -> "No recomendado: revisión de seguridad pendiente"
                risk.contains("ADULT") || safety?.optBoolean("adultRequired")==true -> "Preparación adulta"
                missing.isNotEmpty() || supplies(data).isEmpty() -> "Me faltan materiales"
                else -> "Puedo hacerlo"
            }
            Text(classification,style=MaterialTheme.typography.titleMedium)
            Row {Checkbox(approved,{change(JSONObject(data.toString()).put("safetyReviewedByUser",it))});Text("He revisado personalmente la seguridad y adaptaciones; no es aval MINEDU")}
            InfoCard(if(missing.isEmpty() && supplies(data).isNotEmpty())"Cantidades disponibles" else "Me faltan materiales",missing.joinToString("\n"){"${it.name}: ${it.quantity ?: "revisar cantidad/unidad"} ${it.unit}"}.ifBlank {"Comprueba también herramientas y seguridad antes de preparar."})
            Text("Preparación adulta y seguridad",style=MaterialTheme.typography.titleMedium)
            val warnings=safety?.optJSONArray("warnings") ?: JSONArray()
            for(i in 0 until warnings.length())Text("• ${warnings.getString(i)}")
            val forbidden=safety?.optJSONArray("childrenForbiddenActions") ?: safety?.optJSONArray("prohibited") ?: JSONArray()
            for(i in 0 until forbidden.length())Text("Evitar / restringir al alumnado: ${forbidden.getString(i)}")
            ReorderSteps(data,change)
            Text("Observación anónima del aprendizaje",style=MaterialTheme.typography.titleMedium)
            JsonFields(data,listOf(Field("before","Antes"),Field("during","Durante"),Field("after","Después"),Field("rubric","Rúbrica editable: criterio / niveles / evidencia"),Field("observationChecklist","Lista de cotejo")),change)
            var observation by remember {mutableStateOf("")}
            OutlinedTextField(observation,{observation=it},label={Text("Registro anecdótico o medida fechada")},modifier=Modifier.fillMaxWidth())
            TextButton(onClick={if(observation.isNotBlank()){val a=data.optJSONArray("records") ?: JSONArray();val next=JSONArray(a.toString()).put(JSONObject().put("at",Instant.now().toString()).put("observation",observation));change(JSONObject(data.toString()).put("records",next));observation=""}}){Text("Añadir observación con fecha")}
            val records=data.optJSONArray("records") ?: JSONArray();for(i in 0 until records.length()){val r=records.getJSONObject(i);Text("${r.optString("at")} · ${r.optString("observation")}")}
            TextButton(onClick={val copy=current.copy(id=newId(),title=current.title+" (copia)");save(copy);selected=copy.id}){Text("Duplicar creación")}
            TextButton(onClick={pendingDelete=current}){Text("Archivar creación")}
        }
    } else Page {
        PageTitle("Mi taller","Crea materiales didácticos, paso a paso")
        ChoiceRow(listOf("Materiales","Experimentos","Mis ideas"),section){section=it}
        Row {Button(onClick={create("project")}){Text("Nuevo proyecto")};TextButton(onClick={inventory=true}){Text("Inventario")}}
        sources.firstOrNull {if(section=="Experimentos")it.kind.contains("experiment")else !it.kind.contains("experiment")}?.takeIf {section!="Mis ideas"}?.let {featured->
            Card(onClick={selected=featured.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){
                Row(Modifier.padding(16.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)){
                    Thumbnail(featured.id)
                    Column(Modifier.weight(1f)){Text("Proyecto destacado",style=MaterialTheme.typography.labelMedium);Text(featured.title,style=MaterialTheme.typography.titleLarge);Text(JSONObject(featured.json).optString("objectiveSuggested",JSONObject(featured.json).optString("researchQuestion")));TextButton(onClick={selected=featured.id}){Text("Ver guía")}}
                }
            }
        }
        TextButton(onClick={shopping=true}){Text("Lista de compras")}
        val available=all.filter {it.kind=="inventory" && !it.archived}
        if(available.isNotEmpty())Card(colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)){
            Column(Modifier.padding(12.dp)){Text("Materiales disponibles",style=MaterialTheme.typography.titleMedium);available.take(5).forEach {r->val data=JSONObject(r.json);TextButton(onClick={inventory=true}){Text("${r.title} · ${data.optString("quantity")} ${data.optString("unit")}")}}}
        }
        all.firstOrNull {it.kind in setOf("project","experiment") && !it.archived}?.let {project->
            val steps=JSONObject(project.json).optJSONArray("steps") ?: JSONArray()
            val done=(0 until steps.length()).count {steps.getJSONObject(it).optBoolean("done")}
            Card(onClick={selected=project.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Paper)){Column(Modifier.padding(12.dp)){Text("Preparación: ${project.title}",style=MaterialTheme.typography.titleMedium);Text("$done/${steps.length()} pasos completados")}}
        }
        OutlinedTextField(query,{query=it},label={Text("Buscar proyectos o temas")},modifier=Modifier.fillMaxWidth())
        TextButton(onClick={showFilters=!showFilters}){Text(if(showFilters)"Cerrar filtros" else "Filtrar por grado, área, recursos y seguridad")}
        if(showFilters){
        ChoiceRow(listOf("Todos","1","2","3","4","5","6"),grade){grade=it}
        OutlinedTextField(area,{area=it},label={Text("Área")},modifier=Modifier.fillMaxWidth())
        Row {OutlinedTextField(maxTime,{maxTime=it},label={Text("Hasta minutos")},modifier=Modifier.weight(1f));OutlinedTextField(difficulty,{difficulty=it},label={Text("Dificultad")},modifier=Modifier.weight(1f))}
        OutlinedTextField(resource,{resource=it},label={Text("Material necesario")},modifier=Modifier.fillMaxWidth())
        Row {Checkbox(onlyFav,{onlyFav=it});Text("Solo favoritos")}
        OutlinedTextField(category,{category=it},label={Text("Categoría de material")},modifier=Modifier.fillMaxWidth())
        ChoiceRow(listOf("Todas","Revisión pendiente","Preparación adulta","Revisadas por mí"),safetyFilter){safetyFilter=it}
        }
        if(section=="Experimentos") InfoCard("Propuestas en revisión","Los ocho experimentos no están validados pedagógicamente ni como procedimientos seguros. No se recomiendan automáticamente para niños.")
        val list=if(section=="Mis ideas") all.filter {it.kind in setOf("project","experiment") && !it.archived} else sources.filter {if(section=="Experimentos")it.kind.contains("experiment") else !it.kind.contains("experiment")}
        val shown=list.filter {r->val j=JSONObject(r.json);val safety=j.optJSONObject("safety");r.title.contains(query,true) && j.optString("kind",r.kind).contains(category,true) && when(safetyFilter){"Revisión pendiente"->!j.optBoolean("safetyReviewedByUser");"Preparación adulta"->safety?.optString("riskLevel").orEmpty().contains("ADULT") || safety?.optBoolean("adultRequired")==true;"Revisadas por mí"->j.optBoolean("safetyReviewedByUser");else->true} && j.optString("area").contains(area,true) && j.optString("difficulty").contains(difficulty,true) && (maxTime.toIntOrNull()?.let {j.optInt("estimatedMinutes",0)<=it} ?: true) && (grade=="Todos" || j.optJSONArray("grades")?.toString()?.contains(grade)==true) && materialText(j).contains(resource,true) && (!onlyFav || all.any {it.kind=="favorite" && it.id=="favorite:${r.id}" && !it.archived})}
        shown.forEach {r->
            Card(onClick={selected=r.id},colors=CardDefaults.cardColors(containerColor=ProfeColors.Mint)) {
                Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Thumbnail(r.id)
                    Column(Modifier.weight(1f)){Text(r.title,style=MaterialTheme.typography.titleMedium);Text(JSONObject(r.json).optString("area"));Text("Ver guía y preparación")}
                }
            }
            TextButton(onClick={val id="favorite:${r.id}";if(all.any {it.id==id && !it.archived})archive(id) else save(WorkRecord(id,"favorite",r.title,"{}",System.currentTimeMillis()))}){Text("Alternar favorito")}
        }
        if(shown.isEmpty()) InfoCard("Sin coincidencias","Cambia los filtros o crea tu proyecto.")
    }
    pendingDelete?.let {r->AlertDialog(onDismissRequest={pendingDelete=null},title={Text("¿Archivar creación?")},text={Text("Los recursos y evidencias vinculados se conservan.")},confirmButton={TextButton(onClick={archive(r.id);pendingDelete=null;selected=null}){Text("Archivar")}},dismissButton={TextButton(onClick={pendingDelete=null}){Text("Cancelar")}})}
}

@Composable
private fun Thumbnail(id:String) {
    val context=LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null,id) {value=withContext(Dispatchers.IO){runCatching {context.assets.open("workshop/images/$id.png").use {BitmapFactory.decodeStream(it)}}.getOrNull()}}
    bitmap?.let {Image(it.asImageBitmap(),"Ilustración conceptual original",Modifier.size(96.dp))}
}
@Composable
private fun TemplatePedagogy(data:JSONObject) {
    var mode by rememberSaveable {mutableStateOf("Práctico")}
    ChoiceRow(listOf("Práctico","Académico"),mode){mode=it}
    val pedagogy=data.optJSONObject("pedagogy")?.optJSONObject(if(mode=="Práctico")"practical" else "academic")
    val labels=if(mode=="Práctico")mapOf("purpose" to "Para qué sirve","howToUse" to "Cómo utilizarlo","whatToObserve" to "Qué observar") else mapOf("curricularArea" to "Área sugerida","potentialProcesses" to "Procesos que se pueden practicar","caution" to "Límites")
    labels.forEach {(key,label)->val value=pedagogy?.opt(key);if(value!=null){val text=if(value is JSONArray)(0 until value.length()).joinToString(", "){value.getString(it)}else value.toString();Text("$label: $text")}}
    if(mode=="Académico"){
        Text("Propuesta editorial: alineación curricular no homologada; no diagnostica ni garantiza efectos. No hay citas específicas cotejadas para estas afirmaciones.")
        val inclusion=data.optJSONObject("inclusion");val strategies=inclusion?.optJSONArray("suggestedAdjustments") ?: inclusion?.optJSONArray("strategies") ?: JSONArray()
        for(i in 0 until strategies.length())Text("Adaptación: ${strategies.getString(i)}")
    }
    Text("Fuente: ${data.optString("source","Creación del usuario")}")
}

@Composable
private fun InventoryScreen(all:List<WorkRecord>,save:(WorkRecord)->Unit,archive:(String)->Unit,back:()->Unit) {
    var editing by rememberSaveable {mutableStateOf<String?>(null)}
    var name by rememberSaveable {mutableStateOf("")};var amount by rememberSaveable {mutableStateOf("")}
    var reserved by rememberSaveable {mutableStateOf("0")};var unit by rememberSaveable {mutableStateOf("")};var tool by rememberSaveable {mutableStateOf(false)}
    var reservedFor by rememberSaveable {mutableStateOf("")}
    var error by remember {mutableStateOf<String?>(null)}
    var consumption by rememberSaveable {mutableStateOf("")};var reason by rememberSaveable {mutableStateOf("")}
    var pendingConsumption by remember {mutableStateOf<WorkRecord?>(null)}
    Page {
        PageTitle("Materiales disponibles","Cantidades y reservas personales",back)
        OutlinedTextField(name,{name=it},label={Text("Material o herramienta")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(amount,{amount=it},label={Text("Cantidad disponible")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(unit,{unit=it},label={Text("Unidad exacta (unidad, hoja, ml, g…)")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(reserved,{reserved=it},label={Text("Cantidad reservada")},modifier=Modifier.fillMaxWidth())
        Text("Reserva para: ${all.find {it.id==reservedFor}?.title ?: "sin proyecto asignado"}")
        TextButton(onClick={reservedFor=""}){Text("Quitar asignación de reserva")}
        all.filter {it.kind in setOf("project","experiment") && !it.archived}.forEach {p->TextButton(onClick={reservedFor=p.id}){Text("Reservar para ${p.title}")}}
        Row {Checkbox(tool,{tool=it});Text("Herramienta reutilizable")}
        error?.let {ErrorNotice(it)}
        Button(onClick={val q=amount.toBigDecimalOrNull();val r=reserved.toBigDecimalOrNull();if(name.isBlank()||unit.isBlank()||q==null||r==null||q.signum()<0||r.signum()<0||r>q)error="Revisa cantidad, reservas y unidad." else {val prior=all.find {it.id==editing};val data=JSONObject(prior?.json ?: "{}");save(WorkRecord(editing ?: newId(),"inventory",name,data.put("quantity",q.toPlainString()).put("reserved",r.toPlainString()).put("reservationProjectId",reservedFor).put("unit",unit).put("tool",tool).toString(),System.currentTimeMillis()));editing=null;name="";amount="";reserved="0";reservedFor="";error=null}}){Text("Guardar inventario")}
        all.find {it.id==editing}?.let {entry->
            val data=JSONObject(entry.json)
            if(!data.optBoolean("tool")){
                OutlinedTextField(consumption,{consumption=it},label={Text("Consumir cantidad no reservada (${data.optString("unit")})")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(reason,{reason=it},label={Text("Motivo / proyecto del consumo")},modifier=Modifier.fillMaxWidth())
                TextButton(onClick={
                    val used=consumption.toBigDecimalOrNull();val q=data.optString("quantity").toBigDecimalOrNull();val r=data.optString("reserved","0").toBigDecimalOrNull()
                    if(used==null || used.signum()<=0 || q==null || r==null || used>q-r || reason.isBlank())error="El consumo debe ser positivo, tener motivo y respetar las reservas. Libera antes una reserva que quieras consumir."
                    else {val events=JSONArray((data.optJSONArray("movements") ?: JSONArray()).toString());events.put(JSONObject().put("at",Instant.now().toString()).put("quantity",used.toPlainString()).put("reason",reason));pendingConsumption=entry.copy(json=JSONObject(data.toString()).put("quantity",(q-used).toPlainString()).put("movements",events).toString())}
                }){Text("Revisar descuento de inventario")}
            }
            val events=data.optJSONArray("movements") ?: JSONArray()
            for(i in 0 until events.length()){val movement=events.getJSONObject(i);Text("${movement.optString("at")} · −${movement.optString("quantity")} ${data.optString("unit")} · ${movement.optString("reason")}")}
        }
        all.filter {it.kind=="inventory" && !it.archived}.forEach {r->
            val j=JSONObject(r.json)
            TextButton(onClick={editing=r.id;name=r.title;amount=j.optString("quantity");reserved=j.optString("reserved","0");reservedFor=j.optString("reservationProjectId");unit=j.optString("unit");tool=j.optBoolean("tool")}){Text("${r.title}: ${j.optString("quantity")} ${j.optString("unit")} · reservar ${j.optString("reserved")}")}
            TextButton(onClick={archive(r.id)}){Text("Retirar registro de inventario")}
        }
    }
    pendingConsumption?.let {candidate->AlertDialog(onDismissRequest={pendingConsumption=null},title={Text("¿Descontar este consumo?")},text={Text("$consumption ${JSONObject(candidate.json).optString("unit")} de ${candidate.title}. Solo se descuenta al confirmar; no se altera por marcar pasos.")},confirmButton={TextButton(onClick={save(candidate);amount=JSONObject(candidate.json).optString("quantity");consumption="";reason="";pendingConsumption=null;error=null}){Text("Confirmar descuento")}},dismissButton={TextButton(onClick={pendingConsumption=null}){Text("Cancelar")}})}
}
