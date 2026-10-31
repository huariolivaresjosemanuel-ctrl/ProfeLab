package pe.profelab.core.database

import androidx.room.withTransaction
import java.math.BigDecimal
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*

internal object InventorySnapshots {
    val fields=listOf("quantity","reserved","unit","materialId","materialKind","tool","reusable","reservationProjectId","contentAmount","contentUnit")
    fun hash(text:String)=MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8)).joinToString(""){"%02x".format(it)}
    fun state(row:WorkRecord?):String {
        if(row==null)return "{}"
        val j=JSONObject(row.json);val result=JSONObject()
        fields.forEach {result.put(it,j.opt(it) ?: JSONObject.NULL)}
        return result.put("archived",row.archived).toString()
    }
    fun reservationJson(r:InventoryReservationEntity)=JSONObject().put("id",r.id).put("inventoryId",r.inventoryId).put("ownerId",r.ownerId).put("quantity",r.quantity).put("unit",r.unit).put("status",r.status).put("updatedAt",r.updatedAt)
    fun reservations(rows:List<InventoryReservationEntity>)=JSONArray().also {a->rows.sortedBy {it.id}.forEach {a.put(reservationJson(it))}}.toString()
    fun reservation(j:JSONObject)=InventoryReservationEntity(j.getString("id"),j.getString("inventoryId"),j.getString("ownerId"),j.getString("quantity"),j.getString("unit"),j.getString("status"),j.getLong("updatedAt"))
    fun view(row:InventoryOperationEntity):InventoryOperation {
        val p=JSONObject(row.payload);val a=p.optJSONArray("lines") ?: JSONArray()
        return InventoryOperation(row.id,row.runId,row.kind,row.status,p.optString("reason"),(0 until a.length()).map {i->val l=a.getJSONObject(i);ActualUsage(l.getString("inventoryId"),l.optString("amount"))},p.optBoolean("releaseOwnReservations"))
    }
}

/** All stock, reservation and ledger writes execute in the SAME Room transaction. */
class InventoryOperationRepository(private val db:ProfeDatabase):InventoryOperationStore {
    private val dao=db.inventory()
    override fun observeInventoryOperations(runId:String)=dao.observe(runId).map {rows->rows.map(InventorySnapshots::view)}
    private fun key(id:String){require(id.matches(Regex("[a-zA-Z0-9._:-]{1,160}"))){"Identificador de operación inválido"}}
    private suspend fun stock(id:String):WorkRecord {
        val e=requireNotNull(db.workspace().find(id)){"El material ya no existe"}
        require(e.kind=="inventory" && !e.archived){"El material no está activo"}
        return WorkRecord(e.id,e.kind,e.title,e.json,e.updatedAt,e.revision,e.archived)
    }
    private suspend fun run(id:String,stockId:String?=null){
        if(stockId!=null && id=="manual:$stockId")return
        val r=db.workspace().find(id)
        require(r!=null && !r.archived && r.kind in setOf("project","experiment")){"Inicia o abre una copia personal; no se consume una plantilla"}
    }
    private fun amount(value:String)=InventoryQuantities.amount(value)
    private fun text(value:BigDecimal)=InventoryQuantities.text(value)
    private fun unit(j:JSONObject)=j.optString("unit")
    private fun identity(j:JSONObject)=JSONObject().also {m->listOf("unit","materialId","materialKind","tool","reusable","contentAmount","contentUnit").forEach {m.put(it,j.opt(it) ?: JSONObject.NULL)}}.toString()
    private fun validUnit(j:JSONObject){
        require(InventoryRules.convert(BigDecimal.ONE,unit(j),unit(j),InventoryRules.decimal(j.optString("contentAmount")),j.optString("contentUnit"),InventoryRules.decimal(j.optString("contentAmount")),j.optString("contentUnit"))!=null){"Revisa unidad o equivalencia del paquete"}
    }
    private fun consumable(j:JSONObject){require(j.optString("materialKind")==MaterialKind.CONSUMABLE.name && !j.optBoolean("tool") && !j.optBoolean("reusable")){"Solo se consumen materiales clasificados explícitamente como consumibles; revisa el tipo"}}
    private suspend fun insert(id:String,runId:String,kind:String,payload:JSONObject,revertsId:String?=null):InventoryOperationEntity {
        key(id);val body=payload.toString();require(body.length<=200000)
        val hash=InventorySnapshots.hash(body);val old=dao.operation(id)
        if(old!=null){require(old.kind==kind && old.runId==runId && old.payloadHash==hash && old.revertsId==revertsId){"La clave ya pertenece a otro contenido; no se aplicó"};return old}
        val now=System.currentTimeMillis();val row=InventoryOperationEntity(id,id,runId,kind,"PREPARED",body,hash,revertsId,now,now)
        dao.insertOperation(row);return row
    }
    override suspend fun prepareConsumption(request:ConsumptionRequest):InventoryOperation=db.withTransaction {
        require(request.reason.isNotBlank() && request.reason.length<=2000 && request.lines.size in 1..100)
        require(request.lines.map {it.inventoryId}.distinct().size==request.lines.size){"Un mismo material aparece dos veces"}
        dao.operation(request.operationId)?.let {existing->
            val view=InventorySnapshots.view(existing)
            val expected=request.lines.sortedBy {it.inventoryId}.map {ActualUsage(it.inventoryId,text(amount(it.amount)))}
            require(existing.kind=="CONSUMPTION" && view.runId==request.runId && view.reason==request.reason && view.releaseOwnReservations==request.releaseOwnReservations && view.lines==expected){"La clave ya pertenece a otro consumo"}
            return@withTransaction view
        }
        require(dao.prepared(request.runId).isEmpty()){"Ya existe un consumo pendiente; confirma o cancela ese mismo consumo"}
        val items=JSONArray()
        request.lines.sortedBy {it.inventoryId}.forEach {line->
            run(request.runId,if(request.lines.size==1)line.inventoryId else null)
            val j=JSONObject(stock(line.inventoryId).json);validUnit(j);consumable(j)
            val q=amount(line.amount);require(q.signum()>0)
            items.put(JSONObject().put("inventoryId",line.inventoryId).put("amount",text(q)).put("unit",unit(j)).put("materialId",j.optString("materialId")).put("identity",identity(j)))
        }
        val p=JSONObject().put("version",1).put("reason",request.reason).put("releaseOwnReservations",request.releaseOwnReservations).put("lines",items)
        InventorySnapshots.view(insert(request.operationId,request.runId,"CONSUMPTION",p))
    }
    override suspend fun cancelInventoryOperation(operationId:String)=db.withTransaction {
        val row=requireNotNull(dao.operation(operationId))
        require(row.status!="COMMITTED"){"La operación ya se confirmó; utiliza una reversión"}
        dao.updateOperation(row.copy(status="CANCELLED",updatedAt=System.currentTimeMillis()))
    }
    /** Import a legacy scalar hold once, conservatively retaining an unassigned owner. */
    private suspend fun adopt(row:WorkRecord):List<InventoryReservationEntity> {
        val existing=dao.reservations(row.id)
        if(existing.isNotEmpty())return existing
        val j=JSONObject(row.json);val q=InventoryRules.decimal(j.optString("reserved","0")) ?: return existing
        if(q.signum()>0){
            val owner=j.optString("reservationProjectId").ifBlank {"@legacy:${row.id}"}
            dao.putReservation(InventoryReservationEntity(reservationId(row.id,owner),row.id,owner,text(q),unit(j),"ACTIVE",System.currentTimeMillis()))
        }
        return dao.reservations(row.id)
    }
    private fun reservationId(stockId:String,owner:String)=UUID.nameUUIDFromBytes(("reservation:$stockId:$owner").toByteArray(Charsets.UTF_8)).toString()
    private fun total(rows:List<InventoryReservationEntity>)=rows.filter {it.status=="ACTIVE"}.fold(BigDecimal.ZERO){a,r->a+amount(r.quantity)}
    private fun materialize(row:WorkRecord,holds:List<InventoryReservationEntity>):WorkRecord {
        val active=holds.filter {it.status=="ACTIVE" && amount(it.quantity).signum()>0};val j=JSONObject(row.json)
        j.put("reserved",text(total(holds))).put("reservationProjectId",active.singleOrNull()?.ownerId.orEmpty())
        j.put("inventoryLedgerVersion",1).put("inventoryReservations",JSONArray().also {a->active.forEach {a.put(JSONObject().put("ownerId",it.ownerId).put("quantity",it.quantity))}})
        return row.copy(json=j.toString())
    }
    private suspend fun movement(op:InventoryOperationEntity,before:WorkRecord?,after:WorkRecord,holdsBefore:List<InventoryReservationEntity>,holdsAfter:List<InventoryReservationEntity>){
        val seq=(dao.latest(after.id)?.sequence ?: 0L)+1L;require(seq>0)
        dao.insertMovement(InventoryMovementEntity(UUID.nameUUIDFromBytes((op.id+":"+after.id).toByteArray(Charsets.UTF_8)).toString(),op.id,after.id,InventorySnapshots.state(before),InventorySnapshots.state(after),InventorySnapshots.reservations(holdsBefore),InventorySnapshots.reservations(holdsAfter),seq,System.currentTimeMillis()))
    }
    private suspend fun putStock(row:WorkRecord){
        require(row.json.length<=2_000_000)
        val old=requireNotNull(db.workspace().find(row.id))
        db.workspace().put(old.copy(json=row.json,revision=old.revision+1,updatedAt=System.currentTimeMillis()))
    }
    private fun history(row:WorkRecord,op:InventoryOperationEntity,before:String,after:String):WorkRecord {
        val j=JSONObject(row.json);val a=j.optJSONArray("movements") ?: JSONArray()
        a.put(JSONObject().put("id",op.id).put("kind",op.kind).put("at",java.time.Instant.now().toString()).put("reason",JSONObject(op.payload).optString("reason")).put("before",before).put("after",after).put("unit",unit(j)).put("revertsEventId",op.revertsId ?: JSONObject.NULL))
        return row.copy(json=j.put("movements",a).toString())
    }
    override suspend fun confirmInventoryOperation(operationId:String)=db.withTransaction {
        val op=requireNotNull(dao.operation(operationId)){"No existe el consumo pendiente"}
        require(InventorySnapshots.hash(op.payload)==op.payloadHash)
        if(op.status=="COMMITTED")return@withTransaction
        require(op.status=="PREPARED" && op.kind in setOf("CONSUMPTION","RESERVATION")){"Operación cancelada o no confirmable"}
        val p=JSONObject(op.payload);val lines=p.getJSONArray("lines")
        for(i in 0 until lines.length()){
            val line=lines.getJSONObject(i);val before=stock(line.getString("inventoryId"));val j=JSONObject(before.json)
            run(op.runId,if(op.kind=="CONSUMPTION" && lines.length()==1)before.id else null)
            require(unit(j)==line.getString("unit") && j.optString("materialId")==line.getString("materialId")){"La unidad o identidad cambió; cancela y revisa el consumo"}
            require(identity(j)==line.getString("identity")){"Cambió el tipo o equivalencia del material; revisa el comando"}
            validUnit(j);val balance=amount(j.optString("quantity"));val holdsBefore=adopt(before)
            require(total(holdsBefore).compareTo(amount(j.optString("reserved","0")))==0 && total(holdsBefore)<=balance){"Las reservas requieren revisión; no se descontó nada"}
            val now=System.currentTimeMillis()
            if(op.kind=="RESERVATION"){
                val q=amount(line.getString("amount"));val other=total(holdsBefore.filter {it.ownerId!=op.runId})
                require(q+other<=balance){"La reserva supera el saldo libre"}
                dao.putReservation(InventoryReservationEntity(reservationId(before.id,op.runId),before.id,op.runId,text(q),unit(j),if(q.signum()==0)"RELEASED"else "ACTIVE",now))
            }else {
                consumable(j);val q=amount(line.getString("amount"))
                val release=p.getBoolean("releaseOwnReservations")
                val protected=if(release)total(holdsBefore.filter {it.ownerId!=op.runId})else total(holdsBefore)
                j.put("quantity",text(InventoryQuantities.consume(balance,protected,q)))
                if(release)holdsBefore.filter {it.ownerId==op.runId && it.status=="ACTIVE"}.forEach {dao.putReservation(it.copy(status="RELEASED",updatedAt=now))}
            }
            val holdsAfter=dao.reservations(before.id)
            val after=history(materialize(before.copy(json=j.toString()),holdsAfter),op,JSONObject(before.json).optString("quantity"),j.optString("quantity"))
            movement(op,before,after,holdsBefore,holdsAfter);putStock(after)
        }
        dao.updateOperation(op.copy(status="COMMITTED",updatedAt=System.currentTimeMillis()))
    }
    override suspend fun setInventoryReservation(operationId:String,runId:String,inventoryId:String,quantity:String)=db.withTransaction {
        run(runId);val j=JSONObject(stock(inventoryId).json);validUnit(j);val q=amount(quantity)
        val p=JSONObject().put("version",1).put("reason","Reserva explícita del proyecto").put("lines",JSONArray().put(JSONObject().put("inventoryId",inventoryId).put("amount",text(q)).put("unit",unit(j)).put("materialId",j.optString("materialId")).put("identity",identity(j))))
        insert(operationId,runId,"RESERVATION",p);confirmInventoryOperation(operationId)
    }
    override suspend fun latestInventoryOperation(inventoryId:String)=dao.latest(inventoryId)?.operationId
    override suspend fun reverseInventoryOperation(operationId:String,originalId:String)=db.withTransaction {
        val old=requireNotNull(dao.operation(originalId));require(old.status=="COMMITTED" && old.kind!="REVERSAL")
        dao.reversal(originalId)?.let {return@withTransaction}
        val changes=dao.movements(originalId);require(changes.isNotEmpty())
        val op=insert(operationId,old.runId,"REVERSAL",JSONObject().put("version",1).put("reason","Reversión confirmada").put("lines",JSONArray()),originalId)
        for(m in changes){
            val row=stock(m.inventoryId);val beforeJson=JSONObject(m.beforeState)
            require(beforeJson.has("quantity") && !beforeJson.isNull("quantity") && dao.latest(row.id)?.operationId==old.id){"Hay movimientos posteriores o un alta; no se revierte automáticamente"}
            require(InventorySnapshots.state(row)==m.afterState){"El inventario cambió; revisa antes de revertir"}
            val holdsBefore=dao.reservations(row.id)
            require(InventorySnapshots.reservations(holdsBefore)==m.afterReservations){"Las reservas cambiaron"}
            val now=System.currentTimeMillis();holdsBefore.forEach {dao.putReservation(it.copy(status="RELEASED",updatedAt=now))}
            val previous=JSONArray(m.beforeReservations)
            for(i in 0 until previous.length())dao.putReservation(InventorySnapshots.reservation(previous.getJSONObject(i)).copy(updatedAt=now))
            val holdsAfter=dao.reservations(row.id);val j=JSONObject(row.json)
            // Keep all historical entries, notes and current non-stock fields.
            InventorySnapshots.fields.filter {it !in setOf("reserved","reservationProjectId")}.forEach {field->if(beforeJson.isNull(field))j.remove(field)else j.put(field,beforeJson.get(field))}
            require(amount(j.optString("quantity"))>=total(holdsAfter)){"La reversión no cubre las reservas"}
            val after=history(materialize(row.copy(json=j.toString()),holdsAfter),op,JSONObject(row.json).optString("quantity"),j.optString("quantity"))
            movement(op,row,after,holdsBefore,holdsAfter);putStock(after)
        }
        dao.updateOperation(op.copy(status="COMMITTED",updatedAt=System.currentTimeMillis()))
    }
    /** Called by every existing WorkspaceRepository stock write, inside its transaction. */
    internal suspend fun journalLegacy(id:String,all:List<WorkRecord>,proposed:List<WorkRecord>,force:Boolean=false):List<WorkRecord> {
        require(proposed.map {it.id}.distinct().size==proposed.size)
        val normalized=proposed.map {next->
            val old=all.find {it.id==next.id}
            if(next.kind!="inventory" && old?.kind!="inventory")next else {
                require(next.kind=="inventory")
                if(old!=null)require(next.revision==old.revision){"El inventario cambió; vuelve a abrir el formulario para no sobrescribirlo"}
                // A notes-only save must never overwrite the authoritative multi-owner projection.
                if(old!=null && JSONObject(old.json).optInt("inventoryLedgerVersion")==1 && InventorySnapshots.state(old)==InventorySnapshots.state(next))materialize(next,dao.reservations(next.id))else next
            }
        }
        val changed=normalized.filter {r->val old=all.find {it.id==r.id};(r.kind=="inventory" || old?.kind=="inventory") && InventorySnapshots.state(old)!=InventorySnapshots.state(r)}
        if(changed.isEmpty() && !force)return normalized
        val op=insert(id,"","LEGACY_MUTATION",JSONObject().put("version",1).put("reason","Ajuste o compra explícita del Taller").put("lines",JSONArray()))
        val replacements=mutableMapOf<String,WorkRecord>()
        for(next in changed){
            val old=all.find {it.id==next.id};require(next.kind=="inventory")
            val beforeHolds=if(old==null)emptyList()else adopt(old)
            val oldJson=JSONObject(old?.json ?: "{}");val j=JSONObject(next.json)
            require(!j.optString("quantity").trim().startsWith('-')){"No se permiten existencias negativas"}
            if(old!=null)require(next.revision==old.revision){"El inventario cambió; vuelve a abrir el formulario para no sobrescribirlo"}
            val reservationEdited=j.optString("reserved","0")!=oldJson.optString("reserved","0") || j.optString("reservationProjectId")!=oldJson.optString("reservationProjectId")
            val active=beforeHolds.filter {it.status=="ACTIVE"}
            if(active.isNotEmpty() && j.optString("reserved","0").toBigDecimalOrNull()?.signum()!=0)require(identity(j)==identity(oldJson)){"Libera las reservas antes de cambiar unidad, identidad o tipo"}
            if(old==null || reservationEdited){
                require(active.size<=1){"Hay reservas de varios proyectos; edítalas desde cada proyecto"}
                beforeHolds.forEach {dao.putReservation(it.copy(status="RELEASED",updatedAt=System.currentTimeMillis()))}
                val reserved=amount(j.optString("reserved","0"))
                if(reserved.signum()>0){val owner=j.optString("reservationProjectId").ifBlank {"@legacy:${next.id}"};dao.putReservation(InventoryReservationEntity(reservationId(next.id,owner),next.id,owner,text(reserved),unit(j),"ACTIVE",System.currentTimeMillis()))}
            }
            val afterHolds=dao.reservations(next.id)
            val q=InventoryRules.decimal(j.optString("quantity"))
            require(afterHolds.none {it.status=="ACTIVE"} || q!=null && q>=total(afterHolds)){"El saldo no cubre las reservas"}
            require(!next.archived || total(afterHolds).signum()==0){"Libera las reservas antes de archivar este material"}
            // Invalid legacy values remain repairable, but cannot enter the confirmed-consumption API.
            val after=if(q==null)next else materialize(next,afterHolds)
            movement(op,old,after,beforeHolds,afterHolds);replacements[next.id]=after
        }
        dao.updateOperation(op.copy(status="COMMITTED",updatedAt=System.currentTimeMillis()))
        return normalized.map {replacements[it.id] ?: it}
    }
    override suspend fun mutateInventory(operationId:String,change:(List<WorkRecord>)->List<WorkRecord>)=db.withTransaction {
        key(operationId);dao.operation(operationId)?.let {require(it.status=="COMMITTED");return@withTransaction}
        val rows=db.workspace().all().map {WorkRecord(it.id,it.kind,it.title,it.json,it.updatedAt,it.revision,it.archived)}
        val proposed=journalLegacy(operationId,rows,change(rows),force=true)
        proposed.forEach {r->require(r.id.isNotBlank() && r.json.length<=2_000_000);val old=db.workspace().find(r.id);db.workspace().put(WorkspaceEntity(r.id,r.kind,r.title,r.json,r.updatedAt,(old?.revision ?: 0)+1,r.archived))}
    }
}
