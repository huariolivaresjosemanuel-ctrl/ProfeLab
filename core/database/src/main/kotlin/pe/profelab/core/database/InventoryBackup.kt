package pe.profelab.core.database

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*
import java.math.BigDecimal

/** Validates complete aggregates BEFORE files or database rows are restored. */
internal object InventoryBackup {
    val tables=listOf("inventory_operations","inventory_movements","inventory_reservations")
    private fun objects(a:JSONArray)=(0 until a.length()).map {a.getJSONObject(it)}
    private fun rows(data:JSONObject,table:String)=objects(data.getJSONArray(table))
    private fun same(a:JSONObject,b:JSONObject):Boolean = a.keys().asSequence().toSet()==b.keys().asSequence().toSet() && a.keys().asSequence().all {a.opt(it).toString()==b.opt(it).toString()}
    private fun local(db:ProfeDatabase,table:String,id:String):JSONObject? {
        require(table in tables)
        return db.openHelper.writableDatabase.query("SELECT * FROM $table WHERE id=?",arrayOf(id)).use {c->
            if(!c.moveToFirst())null else JSONObject().also {j->c.columnNames.forEachIndexed {i,n->j.put(n,if(c.isNull(i))JSONObject.NULL else when(c.getType(i)){android.database.Cursor.FIELD_TYPE_INTEGER->c.getLong(i);else->c.getString(i)})}}
        }
    }
    fun validate(data:JSONObject){
        if(data.getInt("schema")==2)return
        val stocks=rows(data,"workspace_records").filter {it.getString("kind")=="inventory"}.associateBy {it.getString("id")}
        val ops=rows(data,"inventory_operations");val movements=rows(data,"inventory_movements");val holds=rows(data,"inventory_reservations")
        tables.forEach {t->val r=rows(data,t);require(r.map {it.getString("id")}.distinct().size==r.size){"IDs repetidos en el respaldo de inventario"}}
        require(ops.map {it.getString("idempotencyKey")}.distinct().size==ops.size)
        val map=ops.associateBy {it.getString("id")}
        val reversed=ops.filter {!it.isNull("revertsId")}.map {it.getString("revertsId")};require(reversed.distinct().size==reversed.size && reversed.all {it in map})
        ops.forEach {op->
            require(op.getString("id")==op.getString("idempotencyKey"))
            require(op.getString("kind") in setOf("LEGACY_MUTATION","CONSUMPTION","RESERVATION","REVERSAL"))
            require(op.getString("status") in setOf("PREPARED","COMMITTED","CANCELLED"))
            val payload=op.getString("payload");JsonLimits.requireBounded(payload,maxCharacters=200000)
            require(InventorySnapshots.hash(payload)==op.getString("payloadHash")){"Comando de inventario alterado"}
            val p=JSONObject(payload);require(p.getInt("version")==1 && p.getJSONArray("lines").length()<=100)
            if(op.getString("kind") in setOf("CONSUMPTION","RESERVATION")){
                val lines=objects(p.getJSONArray("lines"));require(lines.isNotEmpty() && lines.map {it.getString("inventoryId")}.distinct().size==lines.size)
                lines.forEach {l->require(l.getString("inventoryId") in stocks);val q=InventoryQuantities.amount(l.getString("amount"));require(q.signum()>=0 && (op.getString("kind")!="CONSUMPTION" || q.signum()>0))}
            }
        }
        require(ops.filter {it.getString("kind")=="CONSUMPTION" && it.getString("status")=="PREPARED"}.groupBy {it.getString("runId")}.values.all {it.size==1})
        require(movements.map {it.getString("inventoryId") to it.getLong("sequence")}.distinct().size==movements.size)
        movements.groupBy {it.getString("inventoryId")}.values.forEach {history->
            require(history.map {it.getLong("sequence")}.sorted()==(1L..history.size.toLong()).toList()){"El historial de inventario tiene movimientos omitidos"}
        }
        movements.forEach {m->
            require(m.getString("inventoryId") in stocks && m.getLong("sequence")>0)
            require(map[m.getString("operationId")]?.getString("status")=="COMMITTED")
            listOf("beforeState","afterState").forEach {key->val s=m.getString(key);JsonLimits.requireBounded(s,maxCharacters=10000);JSONObject(s)}
            listOf("beforeReservations","afterReservations").forEach {key->val s=m.getString(key);JsonLimits.requireBounded(s,maxCharacters=200000);JSONArray(s)}
        }
        ops.filter {it.getString("status")=="COMMITTED" && it.getString("kind") in setOf("CONSUMPTION","RESERVATION")}.forEach {op->
            val lines=objects(JSONObject(op.getString("payload")).getJSONArray("lines"))
            val effects=movements.filter {it.getString("operationId")==op.getString("id")}
            require(effects.map {it.getString("inventoryId")}.toSet()==lines.map {it.getString("inventoryId")}.toSet()){"Consumo confirmado sin todos sus movimientos"}
            if(op.getString("kind")=="CONSUMPTION")lines.forEach {line->
                val effect=effects.single {it.getString("inventoryId")==line.getString("inventoryId")}
                val before=JSONObject(effect.getString("beforeState"));val after=JSONObject(effect.getString("afterState"))
                require(before.optString("materialKind")=="CONSUMABLE" && !before.optBoolean("tool") && !before.optBoolean("reusable"))
                require(before.getString("unit")==line.getString("unit") && after.getString("unit")==line.getString("unit"))
                val delta=InventoryQuantities.amount(before.getString("quantity"))-InventoryQuantities.amount(after.getString("quantity"))
                require(delta.compareTo(InventoryQuantities.amount(line.getString("amount")))==0){"El consumo no coincide con el cambio de saldo"}
            }
        }
        holds.forEach {h->
            require(h.getString("inventoryId") in stocks && h.getString("status") in setOf("ACTIVE","RELEASED"))
            require(InventoryQuantities.amount(h.getString("quantity")).signum()>=0)
        }
        require(holds.map {it.getString("inventoryId") to it.getString("ownerId")}.distinct().size==holds.size)
        stocks.forEach {(id,row)->
            val j=JSONObject(row.getString("json"));val active=holds.filter {it.getString("inventoryId")==id && it.getString("status")=="ACTIVE"}
            if(active.isNotEmpty()){
                val sum=active.fold(BigDecimal.ZERO){a,h->require(h.getString("unit")==j.optString("unit"));a+InventoryQuantities.amount(h.getString("quantity"))}
                require(sum.compareTo(InventoryQuantities.amount(j.optString("reserved","0")))==0 && sum<=InventoryQuantities.amount(j.optString("quantity"))){"Reservas y saldo del respaldo no coinciden"}
            }
            val last=movements.filter {it.getString("inventoryId")==id}.maxByOrNull {it.getLong("sequence")}
            if(last!=null){
                val state=InventorySnapshots.state(WorkRecord(id,"inventory",row.getString("title"),row.getString("json"),row.getLong("updatedAt"),row.getInt("revision"),row.getInt("archived")!=0))
                require(state==last.getString("afterState")){"El saldo no corresponde al último movimiento del respaldo"}
                val expected=objects(JSONArray(last.getString("afterReservations"))).sortedBy {it.getString("id")}
                val actual=holds.filter {it.getString("inventoryId")==id}.sortedBy {it.getString("id")}
                require(expected.size==actual.size && expected.zip(actual).all {(a,b)->same(a,b)}){"El historial de reservas está incompleto"}
                if(j.optInt("inventoryLedgerVersion")==1){
                    val projection=objects(j.getJSONArray("inventoryReservations"))
                    require(projection.map {it.getString("ownerId")}.distinct().size==projection.size)
                    val projected=projection.associate {it.getString("ownerId") to InventoryQuantities.amount(it.getString("quantity"))}
                    val authoritative=active.associate {it.getString("ownerId") to InventoryQuantities.amount(it.getString("quantity"))}
                    require(projected.keys==authoritative.keys && projected.all {(owner,q)->q.compareTo(requireNotNull(authoritative[owner]))==0}){"La proyección de reservas no coincide con el ledger"}
                }
            }
        }
    }
    /** Conservative merge: never combine new imported events with an existing local stock. */
    suspend fun checkConflicts(data:JSONObject,db:ProfeDatabase){
        if(data.getInt("schema")==2)return
        val newOps=rows(data,"inventory_operations").filter {local(db,"inventory_operations",it.getString("id"))==null}.map {it.getString("id")}.toSet()
        tables.forEach {table->rows(data,table).forEach {incoming->
            val old=local(db,table,incoming.getString("id"))
            if(old!=null)require(same(old,incoming)){"Conflicto de historial de inventario: restaura en una copia vacía o conserva el estado local"}
        }}
        val pendingReferences=rows(data,"inventory_operations").filter {it.getString("id") in newOps}.flatMap {objects(JSONObject(it.getString("payload")).getJSONArray("lines")).map {line->line.getString("inventoryId")}}
        val touched=rows(data,"inventory_movements").filter {it.getString("operationId") in newOps}.map {it.getString("inventoryId")}.toSet()+pendingReferences+
            rows(data,"inventory_reservations").filter {local(db,"inventory_reservations",it.getString("id"))==null}.map {it.getString("inventoryId")}
        touched.forEach {id->require(db.workspace().find(id)==null){"El respaldo añade movimientos a un stock local existente; no se mezclará parcialmente"}}
        // Even a repeated backup cannot fill a missing piece of an already committed aggregate.
        rows(data,"inventory_movements").forEach {m->
            if(local(db,"inventory_operations",m.getString("operationId"))!=null)require(local(db,"inventory_movements",m.getString("id"))!=null){"Agregado local incompleto; no se repara aplicando una mitad"}
        }
    }
}
