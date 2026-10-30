package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

@Composable
internal fun WorkshopShopping(all:List<WorkRecord>,catalog:List<MaterialDefinition>,vm:WorkshopViewModel,state:WorkshopSaveState,back:()->Unit){
    val basket=shoppingRecord(all);val data=JSONObject(basket.json);val lines=shoppingLines(data,all,catalog)
    var formJson by rememberSaveable {mutableStateOf<String?>(null)};var editKey by rememberSaveable {mutableStateOf<String?>(null)}
    val form=formJson?.let {val raw=JSONObject(it);WorkshopCodec.material(raw,catalog).copy(name=raw.optString("name"))}
    var confirm by remember {mutableStateOf<ShoppingLine?>(null)};var quantity by rememberSaveable {mutableStateOf("")}
    var receiptId by rememberSaveable {mutableStateOf("")};var error by remember {mutableStateOf<String?>(null)}
    val checked=WorkshopCodec.strings(data.opt("purchased")).toSet()
    WorkshopPage("Lista de compras","Faltantes calculados por cantidades · sin precios inventados",back){
        item("add"){Button(onClick={editKey=null;formJson=WorkshopCodec.materialJson(MaterialLine(newId(),"","","","unidad",MaterialKind.CONSUMABLE)).toString()},modifier=Modifier.fillMaxWidth()){Text("Añadir o editar un producto")}}
        item("selections"){WorkshopCard("Proyectos incluidos",WorkshopColors.Mint){
            val selected=data.optJSONArray("selections") ?: JSONArray()
            if(selected.length()==0)Text("Abre una guía y pulsa Añadir faltantes a compras. Repetir el mismo proyecto actualiza su selección, nunca duplica sus requisitos.")
            for(i in 0 until selected.length()){val s=selected.getJSONObject(i);Text(s.optString("title").ifBlank {"Proyecto sin título"});TextButton(onClick={vm.editBasket {j->val a=j.optJSONArray("selections") ?: JSONArray();val next=JSONArray();for(n in 0 until a.length()){val item=a.getJSONObject(n);if(item.optString("projectId")!=s.optString("projectId"))next.put(item)};j.put("selections",next)}}){Text("Quitar este proyecto de la compra")}}
        }}
        if(lines.isEmpty())item("empty"){WorkshopCard("No hay compras pendientes en esta lista",WorkshopColors.Paper){Text("El inventario cubre los requisitos seleccionados o todavía no añadiste proyectos. Las existencias cambian solo al confirmar su incorporación.")}}
        items(lines,key={it.key}){line->WorkshopCard(line.supply.name,if(line.supply.tool || line.supply.reusable)WorkshopColors.Blue else WorkshopColors.Yellow){
            Text("Cantidad a adquirir: ${line.amount.ifBlank {"por revisar"}} ${safeMaterialText(line.supply.unit)}")
            Text(when{line.supply.tool->"Herramienta reutilizable";line.supply.reusable->"Material reutilizable";else->"Consumible"},style=MaterialTheme.typography.bodySmall)
            if(line.review.isNotBlank())Text(line.review,style=MaterialTheme.typography.bodySmall)
            Row {Checkbox(line.key in checked,{value->vm.editBasket {j->val keys=WorkshopCodec.strings(j.opt("purchased")).toSet();j.put("purchased",JSONArray((if(value)keys+line.key else keys-line.key).toList()))}});Text("Adquirido · pendiente de incorporar",modifier=Modifier.weight(1f))}
            Row {TextButton(onClick={editKey=line.key;formJson=WorkshopCodec.materialJson(MaterialLine(line.key,line.supply.materialId,line.supply.name,line.amount,line.supply.unit,when{line.supply.tool->MaterialKind.TOOL;line.supply.reusable->MaterialKind.REUSABLE;else->MaterialKind.CONSUMABLE},line.supply.contentAmount?.toPlainString().orEmpty(),line.supply.contentUnit)).toString()}){Text("Editar cantidad")};TextButton(onClick={vm.editBasket {j->j.put("excluded",JSONArray((WorkshopCodec.strings(j.opt("excluded"))+line.key).distinct()))}}){Text("Quitar")}}
            Button(enabled=line.key in checked && state.pending==0,onClick={confirm=line;quantity=line.amount;receiptId=newId();error=null},modifier=Modifier.fillMaxWidth()){Text("Revisar incorporación al inventario")}
        }}
        item("privacy"){Text("Marcar Adquirido no cambia existencias. Revisa la cantidad real y confirma su incorporación; lista e inventario se guardan en una misma transacción.",style=MaterialTheme.typography.bodySmall)}
        val receipts=data.optJSONArray("receipts") ?: JSONArray()
        if(receipts.length()>0)item("receipts"){WorkshopCard("Compras incorporadas",WorkshopColors.Paper){for(i in receipts.length()-1 downTo maxOf(0,receipts.length()-20)){val receipt=receipts.getJSONObject(i);Text("${receipt.optString("at")}\n${receipt.optString("name")}: ${receipt.optString("quantity")} ${receipt.optString("unit")}",style=MaterialTheme.typography.bodySmall)}}}
        val legacy=all.filter {it.kind=="shopping" && !it.archived}
        if(legacy.isNotEmpty())item("legacy"){WorkshopCard("Tu lista anterior se conserva",WorkshopColors.Paper){legacy.forEach {Text(safeMaterialText(JSONObject(it.json).optString("text")))};Text("La lista antigua de texto no se incorpora como stock automáticamente. Puedes registrar sus productos con el formulario.",style=MaterialTheme.typography.bodySmall)}}
    }
    form?.let {initial->MaterialForm(initial,catalog,{formJson=null},lockIdentity=editKey!=null && editKey?.startsWith("manual:")==false){edited->
        vm.editBasket {j->
            val computed=lines.firstOrNull {it.key==editKey && !it.manual} ?: lines.firstOrNull {!it.manual && it.supply.materialId==edited.materialId && it.supply.tool==(edited.kind==MaterialKind.TOOL) && it.supply.reusable==(edited.kind==MaterialKind.REUSABLE) && InventoryRules.convert(java.math.BigDecimal.ONE,edited.unit,it.supply.unit,InventoryRules.decimal(edited.contentAmount),edited.contentUnit,it.supply.contentAmount,it.supply.contentUnit)!=null}
            if(computed!=null){
                val amount=InventoryRules.decimal(edited.amount)?.let {InventoryRules.convert(it,edited.unit,computed.supply.unit,InventoryRules.decimal(edited.contentAmount),edited.contentUnit,computed.supply.contentAmount,computed.supply.contentUnit)}
                val overrides=j.optJSONObject("overrides") ?: JSONObject();j.put("overrides",overrides.put(computed.key,amount?.toPlainString().orEmpty()))
            }else {
                val manual=j.optJSONArray("manual") ?: JSONArray();val next=JSONArray();var stableId=edited.id.removePrefix("manual:");var normalized=edited
                for(i in 0 until manual.length()){val old=manual.getJSONObject(i)
                    val converted=InventoryRules.decimal(edited.amount)?.let {InventoryRules.convert(it,edited.unit,old.optString("unit"),InventoryRules.decimal(edited.contentAmount),edited.contentUnit,InventoryRules.decimal(old.optString("contentAmount")),old.optString("contentUnit"))}
                    val same=old.optString("materialId")==edited.materialId && WorkshopCodec.kind(old)==edited.kind && (converted!=null || InventoryRules.unit(old.optString("unit"))==InventoryRules.unit(edited.unit))
                    if(same){stableId=old.getString("id");if(converted!=null)normalized=edited.copy(amount=converted.toPlainString(),unit=old.optString("unit"),contentAmount=old.optString("contentAmount"),contentUnit=old.optString("contentUnit"))}else if("manual:${old.optString("id")}"!=editKey)next.put(old)}
                next.put(WorkshopCodec.materialJson(normalized.copy(id=stableId)));j.put("manual",next)
                j.put("stocked",JSONArray(WorkshopCodec.strings(j.opt("stocked")).filterNot {it=="manual:$stableId"}))
            }
            j
        };formJson=null
    }}
    confirm?.let {line->AlertDialog(onDismissRequest={confirm=null},title={Text("Incorporar ${line.supply.name}")},text={Column {
        Text("Cantidad realmente adquirida, en ${safeMaterialText(line.supply.unit)}. No se descuenta ningún consumible.")
        OutlinedTextField(quantity,{quantity=it},label={Text("Cantidad")})
        error?.let {Text(it,color=MaterialTheme.colorScheme.error)}
    }},confirmButton={TextButton(onClick={val q=InventoryRules.decimal(quantity);if(q==null || q.signum()<=0)error="Escribe una cantidad positiva"else {vm.acquire(line,q,receiptId,catalog);confirm=null}}){Text("Confirmar incorporación")}},dismissButton={TextButton(onClick={confirm=null}){Text("Cancelar")}})}
}
