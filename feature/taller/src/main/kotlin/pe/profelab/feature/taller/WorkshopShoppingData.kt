package pe.profelab.feature.taller

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.content.WorkshopCodec
import pe.profelab.core.model.*

data class ShoppingLine(val key:String,val supply:Supply,val amount:String,val review:String="",val manual:Boolean=false)
fun shoppingRecord(rows:List<WorkRecord>)=rows.firstOrNull {it.kind=="shopping_v2" && !it.archived}
    ?: WorkRecord(newId(),"shopping_v2","Compras del Taller",JSONObject().put("payloadVersion",2).toString(),System.currentTimeMillis())
fun supplyJson(s:Supply)=WorkshopCodec.materialJson(MaterialLine(s.materialId.ifBlank {legacyMaterialId(s.name)},s.materialId.ifBlank {legacyMaterialId(s.name)},s.name,s.quantity?.toPlainString().orEmpty(),s.unit,when{ s.tool->MaterialKind.TOOL;s.reusable->MaterialKind.REUSABLE;else->MaterialKind.CONSUMABLE},s.contentAmount?.toPlainString().orEmpty(),s.contentUnit))
fun shoppingKey(s:Supply)=listOf(s.materialId.ifBlank {legacyMaterialId(s.name)},InventoryRules.unit(s.unit),s.tool,s.reusable).joinToString("|")
fun shoppingLines(basket:JSONObject,rows:List<WorkRecord>,catalog:List<MaterialDefinition>):List<ShoppingLine> {
    val selections=basket.optJSONArray("selections") ?: JSONArray();val requirements=mutableListOf<Supply>();val owners=mutableSetOf<String>()
    for(i in 0 until selections.length()){
        val selection=selections.getJSONObject(i);val id=selection.optString("projectId");owners+=id
        val active=rows.find {it.id==id && !it.archived && it.kind in setOf("project","experiment")}
        requirements+=if(active!=null)WorkshopCodec.requirements(JSONObject(active.json),catalog)else WorkshopCodec.materials(JSONObject().put("materials",selection.optJSONArray("requirements") ?: JSONArray()),catalog).map {it.supply()}
    }
    val excluded=WorkshopCodec.strings(basket.opt("excluded")).toSet();val stocked=WorkshopCodec.strings(basket.opt("stocked")).toSet()
    val overrides=basket.optJSONObject("overrides") ?: JSONObject()
    val calculated=InventoryRules.assess(requirements,WorkshopCodec.inventory(rows,catalog,owners)).filterNot {it.covered}.map {c->
        val key=shoppingKey(c.requirement);ShoppingLine(key,c.requirement,overrides.optString(key,c.missing?.toPlainString().orEmpty()),c.review)
    }.filter {it.key !in excluded}
    val manual=basket.optJSONArray("manual") ?: JSONArray()
    val custom=(0 until manual.length()).map {i->val line=manual.getJSONObject(i);val m=WorkshopCodec.material(line,catalog,i);ShoppingLine("manual:${line.getString("id")}",m.supply(),m.amount,manual=true)}.filter {it.key !in stocked && it.key !in excluded}
    return calculated+custom
}
