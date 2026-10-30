package pe.profelab.feature.taller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*
import java.math.BigDecimal
import java.time.Instant

data class WorkshopSaveState(val pending:Int=0,val message:String="Cambios locales",val error:String?=null)
class WorkshopViewModel(private val store:WorkspaceStore):ViewModel(){
    private val queue=Channel<Pair<String,suspend ()->Unit>>(Channel.UNLIMITED)
    private val _state=MutableStateFlow(WorkshopSaveState());val state=_state.asStateFlow()
    private var retryOperation:Pair<String,suspend ()->Unit>?=null
    private val desired=androidx.compose.runtime.mutableStateMapOf<String,WorkRecord>()
    private val saving=mutableMapOf<String,Int>();private val acknowledgements=mutableMapOf<String,Int>()
    private val uncommitted=mutableSetOf<String>()
    init{viewModelScope.launch {for((label,operation) in queue){try{operation();val retried=retryOperation?.second===operation;if(retried)retryOperation=null;_state.value=_state.value.copy(message=label,error=if(retried)null else _state.value.error)}catch(e:CancellationException){throw e}catch(e:Exception){retryOperation=label to operation;_state.value=_state.value.copy(error=e.message?.take(220) ?: "No se pudo guardar. Conserva el borrador y reintenta.")}finally{_state.value=_state.value.copy(pending=(_state.value.pending-1).coerceAtLeast(0))}}}}
    private fun action(label:String,operation:suspend ()->Unit){_state.value=_state.value.copy(pending=_state.value.pending+1);queue.trySend(label to operation)}
    fun retry(){val previous=retryOperation;if(previous!=null)action(previous.first,previous.second)else uncommitted.toList().forEach {id->desired[id]?.let(::save)}}
    fun clearError(){retryOperation=null;_state.value=_state.value.copy(error=null)}
    fun current(record:WorkRecord)=desired[record.id] ?: record
    fun reconcile(rows:List<WorkRecord>){rows.forEach {r->if((saving[r.id] ?: 0)==0 && r.revision>=(acknowledgements[r.id] ?: Int.MAX_VALUE))desired.remove(r.id)}}
    fun save(record:WorkRecord){desired[record.id]=record;uncommitted+=record.id;saving[record.id]=(saving[record.id] ?: 0)+1;action("Guardado en este dispositivo"){
        try{val snapshot=desired[record.id] ?: record;store.saveRecord(snapshot.copy(updatedAt=System.currentTimeMillis()));acknowledgements[record.id]=store.record(record.id)?.revision ?: 0;if(desired[record.id]===snapshot)uncommitted.remove(record.id)}
        finally{saving[record.id]=((saving[record.id] ?: 1)-1).coerceAtLeast(0)}
    }}
    fun patch(record:WorkRecord,edit:(JSONObject)->Unit){val latest=current(record);val j=JSONObject(latest.json);edit(j);save(latest.copy(json=j.toString()))}
    fun edit(record:WorkRecord,next:JSONObject,title:String=record.title){
        val old=JSONObject(record.json);val latest=current(record);val merged=JSONObject(latest.json)
        next.keys().forEach {key->if(old.opt(key)?.toString()!=next.opt(key)?.toString())merged.put(key,next.get(key))}
        old.keys().asSequence().filter {!next.has(it)}.toList().forEach {merged.remove(it)}
        save(latest.copy(title=if(title!=record.title)title else latest.title,json=merged.toString()))
    }
    fun draft(id:String)=desired[id]
    fun archive(id:String){action("Archivado; puedes recuperarlo en Mis ideas"){store.archiveRecord(id)}}
    suspend fun flush():Boolean {
        val done=CompletableDeferred<Boolean>();_state.value=_state.value.copy(pending=_state.value.pending+1);queue.send("Cambios procesados" to {done.complete(_state.value.error==null && uncommitted.isEmpty())})
        return done.await()
    }
    private fun mutate(label:String,change:(List<WorkRecord>)->List<WorkRecord>){action(label){store.editRecords(change)}}
    fun saveInventory(record:WorkRecord,definition:MaterialDefinition?,catalog:List<MaterialDefinition> = emptyList()){mutate("Inventario guardado"){rows->
        val j=JSONObject(record.json);val q=InventoryRules.decimal(j.optString("quantity"));val r=InventoryRules.decimal(j.optString("reserved","0"))
        require(record.title.isNotBlank() && q!=null && r!=null && r<=q && j.optString("unit").isNotBlank()){"Revisa nombre, cantidades, reservas y unidad"}
        val duplicate=rows.any {r->val b=JSONObject(r.json);r.kind=="inventory" && !r.archived && r.id!=record.id && b.optString("materialId").ifBlank {WorkshopCodec.resolve(r.title,catalog)?.id ?: legacyMaterialId(r.title)}==j.optString("materialId") && InventoryRules.unit(b.optString("unit"))==InventoryRules.unit(j.optString("unit")) && WorkshopCodec.kind(b)==WorkshopCodec.kind(j)}
        require(!duplicate){"Ya tienes este material con la misma unidad y tipo. Edita su registro o usa Ajustar existencia."}
        val before=rows.find {it.id==record.id};val payload=JSONObject(j.toString())
        if(before!=null){val b=JSONObject(before.json);val changed=listOf("quantity","unit","reserved","materialKind","materialId").any {b.optString(it)!=payload.optString(it)}
            if(changed)payload.put("movements",JSONArray((b.optJSONArray("movements") ?: JSONArray()).toString()).put(JSONObject().put("id",newId()).put("at",Instant.now().toString()).put("kind","EDIT").put("reason","Corrección confirmada en formulario").put("before",b.optString("quantity")).put("after",payload.optString("quantity")).put("unitBefore",b.optString("unit")).put("unit",payload.optString("unit"))))
        }
        val extra=if(definition!=null)listOf(WorkRecord(definition.id,"user_material",definition.name,JSONObject().put("id",definition.id).put("name",definition.name).put("unit",definition.unit).put("category",definition.category).put("materialKind",definition.kind.name).put("synonyms",JSONArray(definition.synonyms)).toString(),System.currentTimeMillis()))else emptyList()
        listOf(record.copy(json=payload.toString(),updatedAt=System.currentTimeMillis()))+extra
    }}
    fun adjustInventory(id:String,delta:BigDecimal,reason:String,consume:Boolean){mutate("Existencia actualizada; movimiento registrado"){rows->
        val row=requireNotNull(rows.find {it.id==id && !it.archived});val j=JSONObject(row.json)
        val q=requireNotNull(InventoryRules.decimal(j.optString("quantity")));val reserved=requireNotNull(InventoryRules.decimal(j.optString("reserved","0")))
        require(reason.isNotBlank() && delta.signum()!=0){"Indica una cantidad y un motivo"}
        if(consume)require(WorkshopCodec.kind(j)==MaterialKind.CONSUMABLE && delta.signum()<0){"Las herramientas y reutilizables no se consumen"}
        val next=q+delta;require(next>=reserved && next.signum()>=0){"La salida excede la cantidad libre; libera primero la reserva"}
        require(InventoryRules.decimal(next.toPlainString())!=null){"El saldo excede 18 cifras significativas u 8 decimales"}
        val event=JSONObject().put("id",newId()).put("at",Instant.now().toString()).put("kind",if(consume)"CONSUME"else "ADJUST").put("reason",reason).put("before",q.toPlainString()).put("after",next.toPlainString()).put("quantity",delta.toPlainString()).put("unit",j.optString("unit"))
        j.put("quantity",next.toPlainString()).put("movements",JSONArray((j.optJSONArray("movements") ?: JSONArray()).toString()).put(event))
        listOf(row.copy(json=j.toString(),updatedAt=System.currentTimeMillis()))
    }}
    fun undoInventory(id:String){mutate("Último ajuste revertido con trazabilidad"){rows->
        val row=requireNotNull(rows.find {it.id==id && !it.archived});val j=JSONObject(row.json);val events=JSONArray((j.optJSONArray("movements") ?: JSONArray()).toString())
        require(events.length()>0){"No hay movimiento que revertir"};val last=events.getJSONObject(events.length()-1)
        require(last.optString("kind") in setOf("ADJUST","CONSUME")){"Este cambio se corrige editando el formulario; no es reversible automáticamente"}
        val before=requireNotNull(InventoryRules.decimal(last.optString("before")));val current=requireNotNull(InventoryRules.decimal(j.optString("quantity")))
        require(current.compareTo(requireNotNull(InventoryRules.decimal(last.optString("after"))))==0 && before>=requireNotNull(InventoryRules.decimal(j.optString("reserved","0")))){"El inventario cambió; corrige la cantidad manualmente"}
        events.put(JSONObject().put("id",newId()).put("at",Instant.now().toString()).put("kind","REVERSAL").put("reason","Reversión del movimiento ${last.optString("id")}").put("before",current.toPlainString()).put("after",before.toPlainString()).put("unit",j.optString("unit")))
        listOf(row.copy(json=j.put("quantity",before.toPlainString()).put("movements",events).toString(),updatedAt=System.currentTimeMillis()))
    }}
    fun addShopping(project:WorkRecord,catalog:List<MaterialDefinition>){mutate("Faltantes añadidos a compras, sin duplicar el proyecto"){rows->
        val basket=shoppingRecord(rows);val j=JSONObject(basket.json);val selections=j.optJSONArray("selections") ?: JSONArray();val next=JSONArray()
        for(i in 0 until selections.length()){val s=selections.getJSONObject(i);if(s.optString("projectId")!=project.id)next.put(s)}
        val requirements=JSONArray();WorkshopCodec.requirements(JSONObject(project.json),catalog).forEach {s->requirements.put(supplyJson(s))}
        next.put(JSONObject().put("projectId",project.id).put("title",project.title).put("requirements",requirements))
        listOf(basket.copy(json=j.put("selections",next).put("excluded",JSONArray()).toString(),updatedAt=System.currentTimeMillis()))
    }}
    fun editBasket(edit:(JSONObject)->JSONObject){mutate("Lista de compras guardada"){rows->val r=shoppingRecord(rows);listOf(r.copy(json=edit(JSONObject(r.json)).toString(),updatedAt=System.currentTimeMillis()))}}
    fun acquire(line:ShoppingLine,amount:BigDecimal,receiptId:String,catalog:List<MaterialDefinition>){mutate("Compra incorporada al inventario"){rows->
        require(amount.signum()>0){"La compra debe tener cantidad positiva"}
        require(InventoryRules.convert(amount,line.supply.unit,line.supply.unit,line.supply.contentAmount,line.supply.contentUnit,line.supply.contentAmount,line.supply.contentUnit)!=null){"Revisa la unidad o equivalencia de paquete antes de incorporar"}
        val basket=shoppingRecord(rows);val j=JSONObject(basket.json);val receipts=j.optJSONArray("receipts") ?: JSONArray()
        if((0 until receipts.length()).any {receipts.getJSONObject(it).optString("id")==receiptId})emptyList()else {
            require(line.key in WorkshopCodec.strings(j.opt("purchased"))){"Marca primero el producto como adquirido"}
            val active=shoppingLines(j,rows,catalog).firstOrNull {it.key==line.key}
            require(active!=null && active.amount==line.amount && active.supply==line.supply){"La lista cambió. Revisa la cantidad actual antes de confirmar de nuevo"}
            val type=when{line.supply.tool->MaterialKind.TOOL;line.supply.reusable->MaterialKind.REUSABLE;else->MaterialKind.CONSUMABLE}
            val match=rows.firstOrNull {r->if(r.kind!="inventory" || r.archived)false else {val d=JSONObject(r.json);d.optString("materialId").ifBlank {WorkshopCodec.resolve(r.title,catalog)?.id ?: legacyMaterialId(r.title)}==line.supply.materialId && WorkshopCodec.kind(d)==type && InventoryRules.convert(amount,line.supply.unit,d.optString("unit"),line.supply.contentAmount,line.supply.contentUnit,InventoryRules.decimal(d.optString("contentAmount")),d.optString("contentUnit"))!=null}}
            val data=JSONObject(match?.json ?: "{}");val old=InventoryRules.decimal(data.optString("quantity","0")) ?: java.math.BigDecimal.ZERO
            val unit=if(match==null)line.supply.unit else data.getString("unit")
            val added=if(match==null)amount else requireNotNull(InventoryRules.convert(amount,line.supply.unit,unit,line.supply.contentAmount,line.supply.contentUnit,InventoryRules.decimal(data.optString("contentAmount")),data.optString("contentUnit")))
            require(InventoryRules.decimal((old+added).toPlainString())!=null){"El saldo excede 18 cifras significativas u 8 decimales"}
            val movements=data.optJSONArray("movements") ?: JSONArray();movements.put(JSONObject().put("id",receiptId).put("at",Instant.now().toString()).put("kind","PURCHASE").put("reason","Compra confirmada").put("before",old.toPlainString()).put("after",(old+added).toPlainString()).put("unit",unit))
            if(match==null)data.put("reserved","0").put("category",catalog.find {it.id==line.supply.materialId}?.category ?: "Otros").put("contentAmount",line.supply.contentAmount?.toPlainString().orEmpty()).put("contentUnit",line.supply.contentUnit)
            data.put("materialId",line.supply.materialId).put("unit",unit).put("materialKind",type.name).put("tool",type==MaterialKind.TOOL).put("reusable",type==MaterialKind.REUSABLE).put("quantity",(old+added).toPlainString()).put("movements",movements)
            val inventory=(match ?: WorkRecord(newId(),"inventory",line.supply.name,"{}",System.currentTimeMillis())).copy(json=data.toString(),updatedAt=System.currentTimeMillis())
            receipts.put(JSONObject().put("id",receiptId).put("at",Instant.now().toString()).put("name",line.supply.name).put("quantity",amount.toPlainString()).put("unit",line.supply.unit).put("lineKey",line.key))
            val stocked=j.optJSONArray("stocked") ?: JSONArray();if(line.manual)stocked.put(line.key)
            j.put("purchased",JSONArray(WorkshopCodec.strings(j.opt("purchased")).filterNot {it==line.key}))
            j.optJSONObject("overrides")?.remove(line.key)
            listOf(inventory,basket.copy(json=j.put("receipts",receipts).put("stocked",stocked).toString(),updatedAt=System.currentTimeMillis()))
        }
    }}
}
