package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.activity.compose.BackHandler
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import org.json.JSONArray
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

@Composable
internal fun WorkshopInventory(all:List<WorkRecord>,catalog:List<MaterialDefinition>,vm:WorkshopViewModel,state:WorkshopSaveState,back:()->Unit){
    var editing by rememberSaveable {mutableStateOf<String?>(null)};var isNew by rememberSaveable {mutableStateOf(false)}
    var query by rememberSaveable {mutableStateOf("")};var remove by remember {mutableStateOf<WorkRecord?>(null)}
    val chosen=all.find {it.id==editing} ?: if(isNew && editing!=null)WorkRecord(requireNotNull(editing),"inventory","","{}",System.currentTimeMillis())else null
    if(chosen!=null){InventoryForm(chosen,all,catalog,vm,state,{editing=null;isNew=false});return}
    val shown=all.filter {it.kind=="inventory" && !it.archived && (it.title.contains(query,true) || JSONObject(it.json).optString("category").contains(query,true))}
    WorkshopPage("Mi inventario","Registra únicamente las existencias que realmente tienes",back){
        item("add"){Button(onClick={editing=newId();isNew=true},modifier=Modifier.fillMaxWidth()){Text("Agregar material o herramienta")}}
        item("search"){OutlinedTextField(query,{query=it},label={Text("Buscar material o categoría")},modifier=Modifier.fillMaxWidth())}
        if(shown.isEmpty())item("empty"){WorkshopCard("Sin materiales en esta vista",WorkshopColors.Mint){Text("Añade nombre, categoría, cantidad, unidad y tipo de uso. El catálogo de proyectos nunca se convierte en existencias automáticamente.")}}
        items(shown,key={it.id}){r->val j=JSONObject(r.json);WorkshopCard(r.title,WorkshopColors.Paper){
            Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){MaterialGlyph(j.optString("category"),WorkshopCodec.kind(j)==MaterialKind.TOOL);Column(Modifier.weight(1f)){Text("${j.optString("quantity")} ${safeMaterialText(j.optString("unit"))} · ${WorkshopCodec.kind(j).label}");Text("Reservado: ${j.optString("reserved","0")} · ${j.optString("category","Otros")}",style=MaterialTheme.typography.bodySmall)}}
            if(InventoryRules.decimal(j.optString("quantity"))==null)Text("Cantidad antigua inválida; edítala antes de compararla.",color=MaterialTheme.colorScheme.error)
            Row {TextButton(onClick={editing=r.id;isNew=false}){Text("Editar / ajustar existencia")};TextButton(onClick={remove=r}){Text("Eliminar")}}
        }}
    }
    remove?.let {r->AlertDialog(onDismissRequest={remove=null},title={Text("¿Eliminar ${r.title} del inventario?")},text={Text("Se archiva su registro. No se eliminan proyectos ni evidencias. Puedes recuperarlo desde Perfil.")},confirmButton={TextButton(onClick={vm.archive(r.id);remove=null}){Text("Eliminar del inventario")}},dismissButton={TextButton(onClick={remove=null}){Text("Cancelar")}})}
}
@Composable
private fun InventoryForm(record:WorkRecord,all:List<WorkRecord>,catalog:List<MaterialDefinition>,vm:WorkshopViewModel,state:WorkshopSaveState,back:()->Unit){
    val data=JSONObject(record.json)
    var name by rememberSaveable(record.id){mutableStateOf(record.title)};var category by rememberSaveable(record.id){mutableStateOf(data.optString("category","Otros"))}
    var amount by rememberSaveable(record.id){mutableStateOf(data.optString("quantity"))};var unit by rememberSaveable(record.id){mutableStateOf(data.optString("unit","unidad"))}
    var kind by rememberSaveable(record.id){mutableStateOf(WorkshopCodec.kind(data).label)};var notes by rememberSaveable(record.id){mutableStateOf(data.optString("notes"))}
    var reserved by rememberSaveable(record.id){mutableStateOf(data.optString("reserved","0"))};var owner by rememberSaveable(record.id){mutableStateOf(data.optString("reservationProjectId"))}
    var content by rememberSaveable(record.id){mutableStateOf(data.optString("contentAmount"))};var contentUnit by rememberSaveable(record.id){mutableStateOf(data.optString("contentUnit"))}
    var dirty by rememberSaveable(record.id){mutableStateOf(false)};var error by remember {mutableStateOf<String?>(null)}
    var leave by remember {mutableStateOf(false)};val scope=rememberCoroutineScope()
    var candidate by remember {mutableStateOf<WorkRecord?>(null)};var newDefinition by remember {mutableStateOf<MaterialDefinition?>(null)}
    LaunchedEffect(record.json){if(!dirty){amount=data.optString("quantity");reserved=data.optString("reserved","0")}}
    LaunchedEffect(state.error){if(state.error!=null)dirty=true}
    fun requestBack(){if(dirty)leave=true else scope.launch {if(vm.flush())back()else error="Resuelve el aviso de guardado antes de salir."}}
    BackHandler {requestBack()}
    WorkshopPage(if(record.title.isBlank())"Agregar a mi inventario"else record.title,"Cantidad y unidad reales · sin precios ficticios",::requestBack){
        item("form"){WorkshopCard("Datos del material",WorkshopColors.Mint){
            if(data.optString("materialKind") !in MaterialKind.entries.map {it.name})Text("El tipo anterior requiere revisión. Confirma si es consumible, reutilizable o herramienta antes de registrar un consumo.",color=MaterialTheme.colorScheme.error)
            OutlinedTextField(name,{name=it.take(120);dirty=true},label={Text("Nombre")},modifier=Modifier.fillMaxWidth())
            if(name.isNotBlank())catalog.filter {(listOf(it.name)+it.synonyms).any {s->s.contains(name,true)}}.take(6).forEach {d->TextButton(onClick={name=d.name;category=d.category;unit=d.unit;kind=d.kind.label;dirty=true}){Text("Usar ${d.name}")}}
            Text("Categoría");WorkshopChoices(materialCategories,category){category=it;dirty=true}
            OutlinedTextField(amount,{amount=it;dirty=true},label={Text("Cantidad en existencia")},modifier=Modifier.fillMaxWidth())
            OutlinedTextField(unit,{unit=it;dirty=true},label={Text("Unidad de medida")},modifier=Modifier.fillMaxWidth())
            WorkshopChoices(materialUnits,InventoryRules.unit(unit)){unit=it;dirty=true}
            Text("Tipo de uso");WorkshopChoices(MaterialKind.entries.map {it.label},kind){kind=it;dirty=true}
            if(InventoryRules.unit(unit)=="paquete"){
                OutlinedTextField(content,{content=it;dirty=true},label={Text("Cantidad contenida en cada paquete")},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(contentUnit,{contentUnit=it;dirty=true},label={Text("Unidad del contenido")},modifier=Modifier.fillMaxWidth())
                Text("Sin equivalencia conocida, el paquete requiere revisión manual y no cubre requisitos de otras unidades.",style=MaterialTheme.typography.bodySmall)
            }
            OutlinedTextField(reserved,{reserved=it;dirty=true},label={Text("Cantidad reservada, en la misma unidad")},modifier=Modifier.fillMaxWidth())
            Text("Reserva para: ${all.find {it.id==owner}?.title ?: "sin proyecto específico"}")
            TextButton(onClick={owner="";dirty=true}){Text("Sin asignación de proyecto")}
            all.filter {it.kind in setOf("project","experiment") && !it.archived}.forEach {p->TextButton(onClick={owner=p.id;dirty=true}){Text("Reservar para ${p.title.ifBlank {"Borrador"}}")}}
            OutlinedTextField(notes,{notes=it.take(4000);dirty=true},label={Text("Observaciones")},minLines=2,modifier=Modifier.fillMaxWidth())
            error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
            Button(enabled=state.pending==0,onClick={
                val q=InventoryRules.decimal(amount);val r=InventoryRules.decimal(reserved)
                if(name.isBlank() || '|' in name || unit.isBlank() || '|' in unit || q==null || r==null || r>q || (content.isNotBlank() && (InventoryRules.decimal(content)?.signum()!=1 || contentUnit.isBlank() || InventoryRules.unit(contentUnit)=="paquete")))error="Revisa nombre, cantidad no negativa, unidad, reserva y contenido del paquete."
                else {
                    val type=MaterialKind.entries.first {it.label==kind};val definition=WorkshopCodec.resolve(name,catalog);val id=definition?.id ?: if(name==record.title)data.optString("materialId").ifBlank {legacyMaterialId(name)}else legacyMaterialId(name)
                    val next=JSONObject(record.json).put("materialId",id).put("category",category).put("quantity",q.toPlainString()).put("unit",unit.trim()).put("materialKind",type.name).put("tool",type==MaterialKind.TOOL).put("reusable",type==MaterialKind.REUSABLE).put("reserved",r.toPlainString()).put("reservationProjectId",owner).put("notes",notes).put("contentAmount",content).put("contentUnit",contentUnit)
                    newDefinition=if(definition==null)MaterialDefinition(id,name.trim(),category=category,unit=unit.trim(),kind=type)else null
                    candidate=record.copy(title=name.trim(),json=next.toString());error=null
                }
            },modifier=Modifier.fillMaxWidth()){Text("Revisar y guardar inventario")}
        }}
        if(all.any {it.id==record.id})item("adjust"){InventoryAdjustment(record,vm,state,dirty)}
        if(all.any {it.id==record.id})item("pending-consumption"){PendingInventoryOperations("manual:${record.id}",all,vm)}
        item("back"){Button(onClick=::requestBack,modifier=Modifier.fillMaxWidth()){Text("Volver al inventario")}}
    }
    candidate?.let {next->AlertDialog(onDismissRequest={candidate=null},title={Text("Confirmar existencias")},text={Text("${next.title}: ${JSONObject(next.json).optString("quantity")} ${JSONObject(next.json).optString("unit")}. Tipo confirmado: $kind. Confirmas la cantidad que realmente tienes. Los ajustes quedan registrados.")},confirmButton={TextButton(onClick={dirty=false;vm.saveInventory(next,newDefinition,catalog);candidate=null}){Text("Guardar")}},dismissButton={TextButton(onClick={candidate=null}){Text("Cancelar")}})}
    if(leave)AlertDialog(onDismissRequest={leave=false},title={Text("Cambios del formulario sin guardar")},text={Text("Puedes seguir editando y confirmar el guardado, o descartar únicamente estos cambios del formulario.")},confirmButton={TextButton(onClick={leave=false}){Text("Seguir editando")}},dismissButton={TextButton(onClick={leave=false;back()}){Text("Descartar cambios")}})
}
@Composable
private fun InventoryAdjustment(record:WorkRecord,vm:WorkshopViewModel,state:WorkshopSaveState,dirty:Boolean){
    val data=JSONObject(record.json);var amount by rememberSaveable(record.id){mutableStateOf("")};var reason by rememberSaveable(record.id){mutableStateOf("")}
    var operation by rememberSaveable(record.id){mutableStateOf("Entrada")};var error by remember {mutableStateOf<String?>(null)};var confirm by remember {mutableStateOf(false)};var undo by remember {mutableStateOf(false)}
    var movementId by rememberSaveable(record.id){mutableStateOf(newId())}
    WorkshopCard("Ajustar existencia y trazabilidad",WorkshopColors.Paper){
        Text("Actual: ${data.optString("quantity")} ${data.optString("unit")} · reservado ${data.optString("reserved","0")}")
        if(dirty)Text("Guarda primero los cambios del formulario antes de ajustar existencias.",style=MaterialTheme.typography.bodySmall)
        WorkshopChoices(if(WorkshopCodec.kind(data)==MaterialKind.CONSUMABLE)listOf("Entrada","Salida","Consumo")else listOf("Entrada","Salida"),operation){operation=it}
        OutlinedTextField(amount,{amount=it},label={Text("Cantidad positiva del movimiento")},modifier=Modifier.fillMaxWidth())
        OutlinedTextField(reason,{reason=it},label={Text("Motivo")},modifier=Modifier.fillMaxWidth())
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
        Button(enabled=!dirty && state.pending==0,onClick={if(InventoryRules.decimal(amount)?.signum()!=1 || reason.isBlank())error="Indica cantidad positiva y motivo"else {error=null;movementId=newId();confirm=true}}){Text("Revisar ajuste")}
        TextButton(enabled=!dirty && state.pending==0,onClick={undo=true}){Text("Revertir último ajuste reversible")}
        val movements=data.optJSONArray("movements") ?: JSONArray()
        for(i in (movements.length()-1) downTo maxOf(0,movements.length()-20)){val event=movements.optJSONObject(i) ?: continue;Text("${event.optString("at")} · ${event.optString("reason")}\n${event.optString("before").ifBlank {"—"}} → ${event.optString("after").ifBlank {event.optString("quantity")}} ${event.optString("unit",data.optString("unit"))}",style=MaterialTheme.typography.bodySmall)}
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("¿Confirmar $operation?")},text={Text("$amount ${data.optString("unit")} de ${record.title}. Los pasos de un proyecto nunca descuentan existencias por sí solos.")},confirmButton={TextButton(onClick={val q=requireNotNull(InventoryRules.decimal(amount));vm.adjustInventory(record.id,if(operation=="Entrada")q else -q,reason,operation=="Consumo",movementId);confirm=false;amount="";reason=""}){Text("Confirmar")}},dismissButton={TextButton(onClick={confirm=false}){Text("Cancelar")}})
    if(undo)AlertDialog(onDismissRequest={undo=false},title={Text("¿Revertir el último ajuste?")},text={Text("Solo si el saldo y las reservas siguen siendo compatibles. Se añade un movimiento inverso; no se borra el historial.")},confirmButton={TextButton(onClick={vm.undoInventory(record.id);undo=false}){Text("Revertir")}},dismissButton={TextButton(onClick={undo=false}){Text("Cancelar")}})
}
