package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.*

/** Versioned, additive payload adapter. Read operations never mutate the stored V1 JSON. */
object WorkshopCodec {
    const val PAYLOAD_VERSION=2
    val templates=setOf("CONSTRUCTION","DIDACTIC","EXPERIMENT","CREATIVE","ACADEMIC")
    val blocks=listOf("purpose","inquiry","materials","preparation","steps","classroom","variants","safety","sources","evidence")
    fun strings(value:Any?):List<String> = when(value){is JSONArray->(0 until value.length()).map {value.optString(it)}.filter {it.isNotBlank()};is String->value.takeIf {it.isNotBlank()}?.let {listOf(it)} ?: emptyList();else->emptyList()}
    fun text(value:Any?)=strings(value).joinToString("\n")
    fun kind(j:JSONObject):MaterialKind = when(j.optString("materialKind").uppercase()){
        "TOOL","HERRAMIENTA"->MaterialKind.TOOL;"REUSABLE","REUTILIZABLE"->MaterialKind.REUSABLE
        else->if(j.optBoolean("tool"))MaterialKind.TOOL else if(j.optBoolean("reusable"))MaterialKind.REUSABLE else MaterialKind.CONSUMABLE
    }
    fun definition(j:JSONObject)=MaterialDefinition(j.getString("id"),j.optString("name",j.optString("title")),strings(j.opt("synonyms")),j.optString("category","Otros"),j.optString("unit","unidad"),kind(j),strings(j.opt("warnings")),alternatives(j.optJSONArray("alternatives") ?: JSONArray()))
    fun resolve(name:String,catalog:List<MaterialDefinition>):MaterialDefinition? {
        val normalized=InventoryRules.canonical(name)
        return catalog.singleOrNull {d->(listOf(d.name)+d.synonyms).any {InventoryRules.canonical(it)==normalized}}
    }
    private fun alternatives(array:JSONArray):List<MaterialAlternative> = (0 until array.length()).mapNotNull {i->
            val a=array.optJSONObject(i) ?: return@mapNotNull null
            val ratio=InventoryRules.decimal(a.optString("ratio")) ?: return@mapNotNull null
            if(ratio.signum()<=0)return@mapNotNull null
            MaterialAlternative(a.optString("materialId"),a.optString("name"),ratio,a.optString("unit"),a.optString("reason"),a.optString("source"),a.optBoolean("verified"))
        }
    fun material(j:JSONObject,catalog:List<MaterialDefinition>,index:Int=0):MaterialLine {
        val name=j.optString("name").ifBlank {"Material por revisar"};val definition=catalog.find {it.id==j.optString("materialId")} ?: resolve(name,catalog)
        val type=if(j.has("materialKind") || j.has("tool") || j.has("reusable"))kind(j)else definition?.kind ?: MaterialKind.CONSUMABLE
        val options=(if(j.has("alternatives"))alternatives(j.optJSONArray("alternatives") ?: JSONArray())else definition?.alternatives.orEmpty()).map {a->a.copy(verified=a.verified && catalog.any {it.id==a.materialId && it.kind==type})}
        val issue=j.optString("legacyIssue").ifBlank {when{j.optString("name").isBlank()->"Falta nombre de material";j.has("materialKind") && j.optString("materialKind") !in MaterialKind.entries.map {it.name}->"Revisar tipo de uso heredado";else->""}}
        return MaterialLine(j.optString("id").ifBlank {"requirement-$index"},j.optString("materialId").ifBlank {definition?.id ?: legacyMaterialId(name)},name,j.optString("amount",j.optString("quantity")),j.optString("unit"),type,j.optString("contentAmount"),j.optString("contentUnit"),options,issue)
    }
    fun materialJson(line:MaterialLine):JSONObject = JSONObject().put("id",line.id).put("materialId",line.materialId).put("name",line.name).put("amount",line.amount).put("unit",line.unit)
        .put("materialKind",line.kind.name).put("tool",line.kind==MaterialKind.TOOL).put("reusable",line.kind==MaterialKind.REUSABLE).put("contentAmount",line.contentAmount).put("contentUnit",line.contentUnit).put("legacyIssue",line.warning)
        .put("alternatives",JSONArray().also {array->line.alternatives.forEach {a->array.put(JSONObject().put("materialId",a.materialId).put("name",a.name).put("ratio",a.ratio.toPlainString()).put("unit",a.unit).put("reason",a.reason).put("source",a.source).put("verified",a.verified))}})
    fun materials(data:JSONObject,catalog:List<MaterialDefinition>):List<MaterialLine> {
        val value=data.opt("materials")
        val entries=when(value){is JSONArray->(0 until value.length()).map {value.get(it)};is String->value.lines().filter {it.isNotBlank()};else->emptyList()}
        val parsed=entries.flatMapIndexed {i,entry->when(entry){
            is JSONObject->listOf(material(entry,catalog,i))
            is String->LegacyMaterialParser.parse(entry).mapIndexed {n,old->MaterialLine("legacy-$i-$n",resolve(old.name,catalog)?.id ?: legacyMaterialId(old.name),old.name,old.amount,old.unit,old.kind,warning=old.issue)}
            else->listOf(MaterialLine("legacy-$i",legacyMaterialId("Revisar $i"),"Material por revisar","","",MaterialKind.CONSUMABLE,warning="Formato antiguo no reconocido; original conservado"))
        }}
        val duplicates=parsed.groupingBy {it.id}.eachCount().filterValues {it>1}.keys
        val used=mutableSetOf<String>()
        return parsed.mapIndexed {i,line->var id=if(line.id in duplicates)"${line.id}-$i"else line.id;while(!used.add(id))id+="-";if(id!=line.id)line.copy(id=id,warning="Identificador repetido; revisa este requisito")else line}
    }
    fun normalize(data:JSONObject,catalog:List<MaterialDefinition>):JSONObject {
        val copy=JSONObject(data.toString())
        if(!copy.has("legacyMaterialsOriginal") && copy.has("materials"))copy.put("legacyMaterialsOriginal",copy.get("materials"))
        val lines=materials(copy,catalog);val array=JSONArray();lines.forEach {array.put(materialJson(it))}
        copy.put("materials",array).put("workshopPayloadVersion",PAYLOAD_VERSION)
        val steps=copy.optJSONArray("steps") ?: JSONArray();val safe=JSONArray()
        val stepIds=mutableSetOf<String>()
        for(i in 0 until steps.length()){val value=steps.get(i);val step=if(value is JSONObject)JSONObject(value.toString())else JSONObject().put("title",value.toString());var id=step.optString("id").ifBlank {"step-$i"};while(!stepIds.add(id))id+="-$i";step.put("id",id);safe.put(step)}
        copy.put("steps",safe)
        return copy
    }
    fun requirements(data:JSONObject,catalog:List<MaterialDefinition>):List<Supply> {
        val selected=data.optJSONObject("selectedAlternatives") ?: JSONObject()
        return materials(data,catalog).map {line->
            val choice=selected.optString(line.id)
            val option=line.alternatives.firstOrNull {it.materialId==choice && it.verified && it.source.isNotBlank() && it.reason.isNotBlank()}
            if(option==null)line.supply() else line.supply().copy(materialId=option.materialId,name=option.name,quantity=line.supply().quantity?.times(option.ratio),unit=option.unit,contentAmount=null,contentUnit="")
        }
    }
    fun inventory(rows:List<WorkRecord>,catalog:List<MaterialDefinition>,owners:Set<String> = emptySet()):List<Stock> = rows.filter {it.kind=="inventory" && !it.archived}.mapNotNull {r->
        val j=JSONObject(r.json);val quantity=InventoryRules.decimal(j.optString("quantity")) ?: return@mapNotNull null
        val reserved=InventoryRules.decimal(j.optString("reserved","0")) ?: return@mapNotNull null;val type=kind(j)
        val breakdown=j.optJSONArray("inventoryReservations")
        val own=if(j.optInt("inventoryLedgerVersion")==1 && breakdown!=null)(0 until breakdown.length()).fold(java.math.BigDecimal.ZERO){sum,i->val hold=breakdown.optJSONObject(i);sum+if(hold?.optString("ownerId") in owners)InventoryRules.decimal(hold?.optString("quantity").orEmpty()).orZero()else java.math.BigDecimal.ZERO}else if(j.optString("reservationProjectId") in owners)reserved else java.math.BigDecimal.ZERO
        Stock(r.title,quantity,(reserved-own).max(java.math.BigDecimal.ZERO),j.optString("unit"),type==MaterialKind.TOOL,type==MaterialKind.REUSABLE,j.optString("materialId").ifBlank {resolve(r.title,catalog)?.id ?: legacyMaterialId(r.title)},InventoryRules.decimal(j.optString("contentAmount")),j.optString("contentUnit"))
    }
    private fun java.math.BigDecimal?.orZero()=this ?: java.math.BigDecimal.ZERO
    fun safety(data:JSONObject):SafetyStatus {
        val j=data.optJSONObject("safety") ?: JSONObject();val level=(j.optString("riskLevel")+j.optString("level")).uppercase()
        if(level.contains("PROHIBITED") || level.contains("FORBIDDEN") || j.optBoolean("notRecommended"))return SafetyStatus.FORBIDDEN
        if(!data.optBoolean("safetyReviewedByUser"))return SafetyStatus.PENDING
        return if(j.optBoolean("adultRequired") || level.contains("ADULT"))SafetyStatus.ADULT else SafetyStatus.REVIEWED
    }
    fun assess(record:WorkRecord,rows:List<WorkRecord>,catalog:List<MaterialDefinition>):WorkshopAssessment {
        val data=JSONObject(record.json)
        return WorkshopAssessment(coverage(requirements(data,catalog),rows,catalog,setOf(record.id)),safety(data))
    }
    fun coverage(needs:List<Supply>,rows:List<WorkRecord>,catalog:List<MaterialDefinition>,owners:Set<String> = emptySet(),shopping:Boolean=false):List<RequirementCoverage> {
        val stocks=inventory(rows,catalog,owners)
        val base=if(shopping)InventoryRules.assess(needs,stocks)else InventoryAvailability.evaluate(needs,stocks)
        return base.map {c->
            val uncertain=rows.any {r->
                if(r.kind!="inventory" || r.archived)false else {
                    val j=JSONObject(r.json)
                    val id=j.optString("materialId").ifBlank {resolve(r.title,catalog)?.id ?: legacyMaterialId(r.title)}
                    val needId=c.requirement.materialId.ifBlank {legacyMaterialId(c.requirement.name)}
                    val quantity=InventoryRules.decimal(j.optString("quantity"));val reserved=InventoryRules.decimal(j.optString("reserved","0"))
                    val explicitType=j.optString("materialKind") in MaterialKind.entries.map {it.name} || j.optBoolean("tool") || j.optBoolean("reusable")
                    id==needId && (!explicitType || quantity==null || reserved==null || reserved>quantity)
                }
            }
            if(uncertain)c.copy(available=null,missing=null,review="Revisa el tipo, saldo o reserva del material heredado; no se supone disponibilidad")
            else if(shopping && stocks.isEmpty() && c.review.isBlank())c.copy(review="No constan existencias: se indica la cantidad total necesaria, no un faltante verificado. Confírmala antes de comprar.")else c
        }
    }
}
