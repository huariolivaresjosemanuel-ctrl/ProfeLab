package pe.profelab.feature.taller

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.json.JSONObject
import pe.profelab.core.model.*

/** A functional T1 control in the existing guide; the approved A0-A6 redesign belongs to T2+. */
@Composable
internal fun InventoryOperationPanel(record:WorkRecord,all:List<WorkRecord>,vm:WorkshopViewModel){
    val ledger=vm.inventoryOperations ?: return
    var expanded by rememberSaveable(record.id){mutableStateOf(false)}
    var reason by rememberSaveable(record.id){mutableStateOf("")}
    var release by rememberSaveable(record.id){mutableStateOf(true)}
    var draftAmounts by rememberSaveable(record.id){mutableStateOf("{}")}
    var draftId by rememberSaveable(record.id){mutableStateOf(newId())}
    val eventsFlow=remember(ledger,record.id){ledger.observeInventoryOperations(record.id)}
    val events by eventsFlow.collectAsState(initial=emptyList())
    val amounts=JSONObject(draftAmounts)
    val hasPending=events.any {it.status=="PREPARED" && it.kind=="CONSUMPTION"}
    val submitted=events.any {it.id==draftId}
    val save by vm.state.collectAsState()
    WorkshopCard("Reservas y consumo real",WorkshopColors.Paper){
        Text("Consultar o marcar pasos no descuenta materiales. Indica lo utilizado y revisa antes de confirmar.")
        TextButton(onClick={expanded=!expanded}){Text(if(expanded)"Cerrar formulario"else "Preparar reservas o consumo")}
        if(expanded){
            val stock=all.filter {it.kind=="inventory" && !it.archived}
            if(stock.isEmpty())Text("No constan existencias. Puedes continuar tu actividad y registrar el inventario después.")
            stock.forEach {r->key(r.id){
                val j=JSONObject(r.json);val unit=j.optString("unit")
                Text("${r.title}: ${j.optString("quantity")} $unit · reservado ${j.optString("reserved","0")}")
                var reservation by rememberSaveable(record.id,r.id){mutableStateOf("")}
                OutlinedTextField(reservation,{reservation=it.take(64)},label={Text("Mi reserva total en $unit (0 libera)")},modifier=Modifier.fillMaxWidth())
                TextButton(enabled=save.pending==0 && InventoryRules.decimal(reservation)!=null,onClick={vm.reserve(record.id,r.id,reservation)}){Text("Confirmar mi reserva")}
                if(j.optString("materialKind")==MaterialKind.CONSUMABLE.name){
                    OutlinedTextField(amounts.optString(r.id),{draftAmounts=JSONObject(draftAmounts).put(r.id,it.take(64)).toString()},label={Text("Uso real de ${r.title} en $unit")},modifier=Modifier.fillMaxWidth())
                }else Text("Herramienta, reutilizable o tipo por revisar: no se descontará.",style=MaterialTheme.typography.bodySmall)
                HorizontalDivider()
            }}
            OutlinedTextField(reason,{reason=it.take(2000)},label={Text("Motivo del consumo real")},modifier=Modifier.fillMaxWidth())
            Row {Checkbox(release,{release=it});Text("Liberar mis reservas de los materiales consumidos")}
            Button(enabled=save.pending==0 && !hasPending && !submitted && reason.isNotBlank() && amounts.keys().asSequence().any {amounts.optString(it).isNotBlank()},onClick={
                val lines=amounts.keys().asSequence().filter {amounts.optString(it).isNotBlank()}.map {ActualUsage(it,amounts.getString(it))}.toList()
                vm.prepareConsumption(ConsumptionRequest(draftId,record.id,reason,lines,release))
            },modifier=Modifier.testTag("inventory-review-consumption")){Text("Revisar consumo (sin descontar todavía)")}
            if(submitted && !hasPending)TextButton(onClick={draftId=newId();draftAmounts="{}";reason=""}){Text("Preparar otro consumo explícito")}
        }
        PendingInventoryOperations(record.id,all,vm)
    }
}

@Composable
internal fun PendingInventoryOperations(runId:String,all:List<WorkRecord>,vm:WorkshopViewModel){
    val ledger=vm.inventoryOperations ?: return
    val flow=remember(ledger,runId){ledger.observeInventoryOperations(runId)}
    val operations by flow.collectAsState(initial=emptyList())
    val save by vm.state.collectAsState()
    var reverse by rememberSaveable(runId){mutableStateOf<String?>(null)}
    operations.filter {it.status=="PREPARED" && it.kind=="CONSUMPTION"}.forEach {op->
        Text("Consumo pendiente: ${op.reason}",style=MaterialTheme.typography.titleSmall)
        op.lines.forEach {line->val r=all.find {it.id==line.inventoryId};Text("${r?.title ?: "Material no disponible"}: ${line.amount} ${r?.let {JSONObject(it.json).optString("unit")}.orEmpty()}")}
        Text(if(op.releaseOwnReservations)"Se liberarán tus reservas de estas líneas."else "Se mantienen las reservas; solo se usará saldo libre.")
        Button(enabled=save.pending==0,onClick={vm.confirmConsumption(op.id)},modifier=Modifier.testTag("inventory-confirm:${op.id}")){Text("Confirmar consumo real")}
        TextButton(enabled=save.pending==0,onClick={vm.cancelConsumption(op.id)}){Text("Cancelar sin descontar")}
    }
    operations.filter {it.status=="COMMITTED" && it.kind in setOf("CONSUMPTION","RESERVATION")}.take(3).forEach {op->
        Text("${if(op.kind=="CONSUMPTION")"Consumo"else "Reserva"} registrado: ${op.id}",style=MaterialTheme.typography.bodySmall)
        TextButton(enabled=save.pending==0,onClick={reverse=op.id}){Text("Revisar reversión")}
    }
    reverse?.let {id->AlertDialog(onDismissRequest={reverse=null},title={Text("¿Revertir esta operación?")},text={Text("Se añadirá una compensación; el historial no se borra. Si hubo movimientos posteriores, deberás revisar el inventario.")},confirmButton={TextButton(onClick={vm.reverseConsumption(id);reverse=null}){Text("Confirmar reversión")}},dismissButton={TextButton(onClick={reverse=null}){Text("Cancelar")}})}
}
