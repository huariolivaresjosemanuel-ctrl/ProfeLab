package pe.profelab.feature.taller

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*
import java.math.BigDecimal

fun supplies(data:JSONObject):List<Supply> {
    val a=data.optJSONArray("materials") ?: JSONArray()
    return (0 until a.length()).map {i->val m=a.getJSONObject(i);Supply(m.optString("name"),m.optString("amount").toBigDecimalOrNull(),m.optString("unit"),m.optBoolean("tool"))}
}
fun stock(records:List<WorkRecord>,forProjects:Set<String> = emptySet())=records.filter {it.kind=="inventory" && !it.archived}.mapNotNull {
    val j=JSONObject(it.json);val quantity=j.optString("quantity").toBigDecimalOrNull() ?: return@mapNotNull null
    Stock(it.title,quantity,if(j.optString("reservationProjectId") in forProjects)BigDecimal.ZERO else j.optString("reserved").toBigDecimalOrNull() ?: BigDecimal.ZERO,j.optString("unit"),j.optBoolean("tool"))
}
fun materialText(data:JSONObject)=supplies(data).joinToString("\n") {"${it.name}|${it.quantity ?: ""}|${it.unit}|${if(it.tool) "herramienta" else "consumible"}"}
fun parseMaterials(text:String):JSONArray {
    val a=JSONArray();text.lines().filter {it.isNotBlank()}.forEach {line->val p=line.split('|');a.put(JSONObject().put("name",p[0].trim()).put("amount",p.getOrNull(1)?.trim().orEmpty()).put("unit",p.getOrNull(2)?.trim().orEmpty()).put("tool",p.getOrNull(3)?.trim()=="herramienta"))};return a
}
