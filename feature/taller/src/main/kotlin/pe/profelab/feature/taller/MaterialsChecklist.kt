package pe.profelab.feature.taller

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.*
import pe.profelab.core.model.*
import java.math.BigDecimal

@Composable
internal fun ShoppingShareButtons(text:String){
    val context=LocalContext.current;var message by remember {mutableStateOf<String?>(null)}
    FlowAction("Copiar lista","workshop-copy-shopping",{
        try{(context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Lista ProfeLab",text));message="Lista copiada"}catch(_:Exception){message="No se pudo copiar. Puedes seleccionar el texto de la lista."}
    },false)
    FlowAction("Compartir · WhatsApp y otras apps","workshop-share-shopping",{
        try{context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,text),"Compartir lista de ProfeLab"))}catch(_:android.content.ActivityNotFoundException){message="No hay una aplicación para compartir; usa Copiar lista."}
    },false)
    message?.let {Text(it,style=MaterialTheme.typography.bodySmall)}
}

@Composable
internal fun MaterialsChecklist(record:WorkRecord,all:List<WorkRecord>,catalog:List<MaterialDefinition>,view:ActivityViewState,vm:WorkshopViewModel,back:()->Unit,actions:ActivityActions){
    val assessment=WorkshopCodec.assess(record,all,catalog);val data=JSONObject(record.json)
    val prepared=WorkshopCodec.strings(data.optJSONObject("workshopFlow")?.opt("preparedMaterialIds")).toSet()
    val stocks=WorkshopCodec.inventory(all,catalog);val otherStocks=WorkshopCodec.inventory(all,catalog,setOf(record.id))
    val text=WorkshopFlowAdapter.shoppingText(record,assessment)
    WorkshopPage("Materiales e inventario","Revisa lo que necesitas y qué tienes",back,screenTag="workshop-materials"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("heading"){WorkshopChoices(listOf("Lista de materiales","Inventario"),"Lista de materiales"){if(it=="Inventario")actions.inventory()};Text("Materiales y herramientas",style=MaterialTheme.typography.titleMedium)}
        assessment.coverage.forEach {coverage->val need=coverage.requirement;val key="${need.materialId}:${InventoryRules.unit(need.unit)}:${need.tool}:${need.reusable}"
            item("requirement:$key"){
                var detail by rememberSaveable(record.id,key){mutableStateOf(false)}
                FlowCard(color=WorkshopColors.Paper,contentPadding=10.dp){
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                        WorkshopMediaSlot(record,"material_icon",materialId=need.materialId,modifier=Modifier.size(56.dp))
                        Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)){
                            Text(need.name,style=MaterialTheme.typography.titleMedium)
                            Text("${displayNumber(need.quantity)} ${need.unit}",style=MaterialTheme.typography.bodyMedium)
                            Text(when(InventoryAvailability.state(coverage)){AvailabilityState.READY->"Suficiente";AvailabilityState.PARTIAL->"Disponible parcialmente";AvailabilityState.MISSING->"Me falta";AvailabilityState.REVIEW->"Sin datos · revisar"},style=MaterialTheme.typography.labelLarge,color=WorkshopColors.Green)
                        }
                        IconButton(onClick={detail=!detail}){Icon(if(detail)Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,if(detail)"Ocultar existencias y reservas"else "Ver existencias y reservas")}
                    }
                    if(detail){
                        val physical=convertedTotal(stocks,need,false,coverage.review)
                        val reserved=convertedTotal(stocks,need,true,coverage.review)
                        val others=convertedTotal(otherStocks,need,true,coverage.review)
                        val own=if(reserved!=null && others!=null)(reserved-others).max(BigDecimal.ZERO)else null
                        Text("Necesitas: ${displayNumber(need.quantity)} ${need.unit}")
                        Text("Existencia física: ${displayNumber(physical)} ${need.unit}")
                        Text("Reserva personal: ${displayNumber(own)} · Reserva de terceros: ${displayNumber(others)} ${need.unit}")
                        Text("Disponible para esta actividad: ${displayNumber(coverage.available)} · Falta: ${displayNumber(coverage.missing)} ${need.unit}")
                        Text(when{need.tool->"Herramienta: no se consume por completar pasos";need.reusable->"Reutilizable: no se consume por completar pasos";else->"Consumible: requiere confirmar uso real"},style=MaterialTheme.typography.bodySmall)
                        if(coverage.review.isNotBlank())Text(coverage.review,style=MaterialTheme.typography.bodySmall)
                    }
                    if(view.personal)Row(verticalAlignment=Alignment.CenterVertically){Checkbox(key in prepared,{value->vm.patch(record){j->val flow=j.optJSONObject("workshopFlow") ?: JSONObject();val ids=WorkshopCodec.strings(flow.opt("preparedMaterialIds")).toSet();j.put("workshopFlow",flow.put("version",1).put("preparedMaterialIds",JSONArray((if(value)ids+key else ids-key).toList())))}});Text("Lo tengo preparado · check personal",Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)}
                }
            }
        }
        if(assessment.coverage.isEmpty())item("empty"){Text("No se han definido materiales. Usa el editor en tu copia personal.")}
        item("notice"){FlowCard(color=WorkshopColors.Mint,icon=Icons.Outlined.Info,title="Tu inventario se conserva"){Text("Los consumibles solo se descuentan después de confirmar su uso real. Avanzar, completar pasos o marcar preparación no cambia existencias.",style=MaterialTheme.typography.bodyMedium)}}
        item("inventory"){FlowCard("Estado de inventario",WorkshopColors.Paper,Icons.Outlined.Inventory2){Text("${assessment.covered} de ${assessment.coverage.size} requisitos cubiertos");FlowAction("Registrar o revisar mis existencias","workshop-to-inventory",actions.inventory,false)}}
        item("buy"){FlowAction("Generar lista de compras","workshop-generate-shopping",{vm.addShopping(record,catalog);actions.shopping()})}
        item("share"){FlowCard("Lista de compras",WorkshopColors.Paper,Icons.Outlined.ShoppingCart){androidx.compose.foundation.text.selection.SelectionContainer {Text(text)};ShoppingShareButtons(text)}}
        if(view.personal)item("operations"){InventoryOperationPanel(record,all,vm)}else item("copy"){Text("Comienza una copia para guardar preparación, reservas y consumos.");FlowAction("Comenzar mi proyecto","workshop-materials-begin",actions.begin,false)}
        item("next"){FlowAction("Ver procedimiento guiado","workshop-materials-next",{actions.navigate(WorkshopWindow.STEPS)});FlowAction("Más información pedagógica","workshop-materials-purpose",{actions.navigate(WorkshopWindow.PURPOSE)},false)}
    }
}

private fun convertedTotal(stocks:List<Stock>,need:Supply,reserved:Boolean,review:String):BigDecimal? {
    if(review.isNotBlank())return null
    val matching=stocks.filter {it.materialId==need.materialId && it.tool==need.tool && it.reusable==need.reusable}
    val amounts=matching.map {s->InventoryRules.convert(if(reserved)s.reserved else s.quantity,s.unit,need.unit,s.contentAmount,s.contentUnit,need.contentAmount,need.contentUnit)}
    return if(amounts.any {it==null})null else amounts.filterNotNull().fold(BigDecimal.ZERO,BigDecimal::add)
}
