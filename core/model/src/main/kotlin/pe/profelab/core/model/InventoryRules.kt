package pe.profelab.core.model

import java.math.BigDecimal
import java.text.Normalizer

data class Supply(val name:String,val quantity:BigDecimal?,val unit:String,val tool:Boolean=false,val reusable:Boolean=false,
    val materialId:String="",val contentAmount:BigDecimal?=null,val contentUnit:String="")
data class Stock(val name:String,val quantity:BigDecimal,val reserved:BigDecimal,val unit:String,val tool:Boolean=false,val reusable:Boolean=false,
    val materialId:String="",val contentAmount:BigDecimal?=null,val contentUnit:String="")
data class MissingSupply(val name:String,val quantity:BigDecimal?,val unit:String,val tool:Boolean,val reusable:Boolean=false,val materialId:String="")
object InventoryRules {
    fun canonical(text:String)=Normalizer.normalize(text.lowercase(java.util.Locale.ROOT).trim(),Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"),"").replace(Regex("\\s+")," ")
    fun decimal(text:String):BigDecimal? = text.trim().takeIf {it.matches(Regex("[+]?(?:[0-9]+(?:[.,][0-9]+)?|[.,][0-9]+)"))}?.replace(',','.')?.toBigDecimalOrNull()?.takeIf {it.precision()<=18 && it.scale()<=8}
    fun unit(text:String)=when(canonical(text)){
        "unidad","unidades","ud","uds","pieza","piezas"->"unidad";"hoja","hojas"->"hoja"
        "m","metro","metros"->"m";"cm","centimetro","centimetros"->"cm"
        "ml","mililitro","mililitros"->"ml";"l","litro","litros"->"l"
        "g","gramo","gramos"->"g";"kg","kilogramo","kilogramos"->"kg"
        "paquete","paquetes","pack"->"paquete";"plancha","planchas"->"plancha";"tablero","tableros"->"tablero"
        else->canonical(text)
    }
    private data class Measure(val base:String,val factor:BigDecimal)
    private fun measure(inputUnit:String,contentAmount:BigDecimal?=null,contentUnit:String=""):Measure? {
        val u=unit(inputUnit)
        if(u.isBlank() || u.contains("ajustar") || u.contains("referencial") || u.contains('|'))return null
        if(u=="paquete") {
            if(contentAmount==null || contentAmount.signum()<=0 || unit(contentUnit)=="paquete")return null
            val content=measure(contentUnit) ?: return null
            return Measure(content.base,content.factor*contentAmount)
        }
        return when(u){"m"->Measure("cm",BigDecimal("100"));"kg"->Measure("g",BigDecimal("1000"));"l"->Measure("ml",BigDecimal("1000"));else->Measure(u,BigDecimal.ONE)}
    }
    fun convert(amount:BigDecimal,from:String,to:String,fromContent:BigDecimal?=null,fromContentUnit:String="",toContent:BigDecimal?=null,toContentUnit:String=""):BigDecimal? {
        val a=measure(from,fromContent,fromContentUnit) ?: return null;val b=measure(to,toContent,toContentUnit) ?: return null
        if(a.base!=b.base)return null
        return (amount*a.factor).divide(b.factor,8,java.math.RoundingMode.DOWN).stripTrailingZeros().let {if(it.scale()<0)it.setScale(0)else it}
    }
    private fun identity(name:String,id:String)=id.ifBlank {legacyMaterialId(name)}
    private fun key(s:Supply):String {val m=measure(s.unit,s.contentAmount,s.contentUnit);return listOf(identity(s.name,s.materialId),m?.base ?: "review:${unit(s.unit)}",s.tool,s.reusable).joinToString(":")}
    fun consolidate(requirements:List<Supply>):List<Supply> = requirements.groupBy(::key).values.map {group->
        val first=group.first();val amounts=group.map {s->s.quantity?.takeIf {it.signum()>0}?.let {convert(it,s.unit,first.unit,s.contentAmount,s.contentUnit,first.contentAmount,first.contentUnit)}}
        first.copy(quantity=if(amounts.any {it==null})null else if(first.tool || first.reusable)amounts.filterNotNull().maxOrNull()else amounts.filterNotNull().fold(BigDecimal.ZERO,BigDecimal::add))
    }
    fun assess(requirements:List<Supply>,inventory:List<Stock>):List<RequirementCoverage> = consolidate(requirements).map {r->
        val q=r.quantity
        if(q==null || q.signum()<=0 || measure(r.unit,r.contentAmount,r.contentUnit)==null)RequirementCoverage(r,null,null,"Completar cantidad, unidad o equivalencia de paquete")
        else {
            val matching=inventory.filter {identity(it.name,it.materialId)==identity(r.name,r.materialId) && it.tool==r.tool && it.reusable==r.reusable}
            val converted=matching.mapNotNull {s->if(s.quantity.signum()<0 || s.reserved.signum()<0 || s.reserved>s.quantity)null else convert((s.quantity-s.reserved).max(BigDecimal.ZERO),s.unit,r.unit,s.contentAmount,s.contentUnit,r.contentAmount,r.contentUnit)}
            val available=converted.fold(BigDecimal.ZERO,BigDecimal::add)
            val review=if(matching.isNotEmpty() && converted.isEmpty())"Unidades incompatibles o equivalencia pendiente; revisar manualmente"else ""
            RequirementCoverage(r,available,(q-available).max(BigDecimal.ZERO),review)
        }
    }
    fun missing(requirements:List<Supply>,inventory:List<Stock>):List<MissingSupply> = assess(requirements,inventory).filterNot {it.covered}.map {MissingSupply(it.requirement.name,if(it.review.isNotEmpty())null else it.missing,it.requirement.unit,it.requirement.tool,it.requirement.reusable,it.requirement.materialId)}
    fun shopping(projects:List<List<Supply>>,inventory:List<Stock>)=missing(projects.flatten(),inventory)
}
