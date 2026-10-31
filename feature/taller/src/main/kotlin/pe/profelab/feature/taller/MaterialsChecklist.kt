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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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
    val a=WorkshopCodec.assess(record,all,catalog);val j=JSONObject(record.json)
    val prepared=WorkshopCodec.strings(j.optJSONObject("workshopFlow")?.opt("preparedMaterialIds")).toSet()
    val inventory=WorkshopCodec.inventory(all,catalog);val text=WorkshopFlowAdapter.shoppingText(record,a)
    WorkshopPage("Materiales","Revisa lo que ya tienes y lo que te falta",back,screenTag="workshop-materials"){
        item("summary"){ActivitySummary(record,view,{actions.navigate(WorkshopWindow.DETAIL)})}
        item("heading"){Text("Materiales y herramientas",style=MaterialTheme.typography.titleLarge);Text("La preparación marcada por ti no acredita existencias ni registra consumo.",style=MaterialTheme.typography.bodySmall)}
        a.coverage.forEach {c->val need=c.requirement;val id="${need.materialId}:${InventoryRules.unit(need.unit)}:${need.tool}:${need.reusable}"
            item("requirement:$id"){FlowCard(color=if(c.covered)WorkshopColors.Paper else if(c.review.isBlank())WorkshopColors.Peach else WorkshopColors.Yellow){
                Row {MaterialGlyph("",need.tool);Text(need.name,Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)}
                Text("Necesitas: ${displayNumber(need.quantity)} ${need.unit}")
                val matching=inventory.filter {s->s.materialId==need.materialId && s.tool==need.tool && s.reusable==need.reusable}
                fun total(reserve:Boolean):BigDecimal? {
                    val quantities=matching.map {s->InventoryRules.convert(if(reserve)s.reserved else s.quantity,s.unit,need.unit,s.contentAmount,s.contentUnit,need.contentAmount,need.contentUnit)}
                    return if(quantities.any {it==null} || c.review.isNotBlank())null else quantities.filterNotNull().fold(BigDecimal.ZERO,BigDecimal::add)
                }
                Text("Existencia física: ${displayNumber(total(false))} · Reserva total: ${displayNumber(total(true))} ${need.unit}",style=MaterialTheme.typography.bodySmall)
                RequirementRow(c)
                if(view.personal)Row {Checkbox(id in prepared,{value->vm.patch(record){data->val flow=data.optJSONObject("workshopFlow") ?: JSONObject();val ids=WorkshopCodec.strings(flow.opt("preparedMaterialIds")).toSet();data.put("workshopFlow",flow.put("version",1).put("preparedMaterialIds",JSONArray((if(value)ids+id else ids-id).toList())))}});Text("Lo tengo preparado (check personal)",Modifier.weight(1f))}
            }}
        }
        if(a.coverage.isEmpty())item("empty"){Text("No se han definido materiales. Usa el editor en tu copia personal.")}
        item("notice"){FlowCard(color=WorkshopColors.Mint,icon=Icons.Outlined.Info,title="Tu inventario se conserva"){Text("Avanzar o terminar pasos nunca descuenta materiales. Solo se registra el uso real que revises y confirmes. La disponibilidad incluye tus reservas propias; las ajenas siguen protegidas.")}}
        item("buy"){FlowCard("Te falta comprar",WorkshopColors.Peach,Icons.Outlined.ShoppingCart){androidx.compose.foundation.text.selection.SelectionContainer {Text(text)};FlowAction("Generar / actualizar lista de compras","workshop-generate-shopping",{vm.addShopping(record,catalog);actions.shopping()});ShoppingShareButtons(text)}}
        item("inventory"){FlowAction("Registrar o revisar mis existencias","workshop-to-inventory",actions.inventory,false)}
        if(view.personal)item("operations"){InventoryOperationPanel(record,all,vm)}else item("copy"){Text("Comienza una copia para guardar preparación, reservas y consumos.");FlowAction("Comenzar mi proyecto","workshop-materials-begin",actions.begin,false)}
        item("next"){FlowAction("Comprender el propósito","workshop-materials-next",{actions.navigate(WorkshopWindow.PURPOSE)})}
    }
}
