package pe.profelab.core.model

import java.math.BigDecimal
import java.text.Normalizer

data class Supply(val name:String,val quantity:BigDecimal?,val unit:String,val tool:Boolean=false)
data class Stock(val name:String,val quantity:BigDecimal,val reserved:BigDecimal,val unit:String,val tool:Boolean=false)
data class MissingSupply(val name:String,val quantity:BigDecimal?,val unit:String,val tool:Boolean)
object InventoryRules {
    fun canonical(text:String)=Normalizer.normalize(text.lowercase().trim(),Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"),"").replace(Regex("\\s+")," ")
    fun unit(text:String)=when(canonical(text)){"unidad","unidades","pieza","piezas"->"unidad";"ml","mililitros"->"ml";"g","gramos"->"g";else->canonical(text)}
    private fun consolidate(requirements:List<Supply>)=requirements.groupBy {Triple(canonical(it.name),unit(it.unit),it.tool)}.values.map {group->
        val first=group.first()
        first.copy(quantity=if(group.any {it.quantity==null || it.quantity<=BigDecimal.ZERO})null else if(first.tool)group.maxOf {it.quantity!!}else group.fold(BigDecimal.ZERO){sum,item->sum+item.quantity!!})
    }
    fun missing(requirements:List<Supply>,inventory:List<Stock>):List<MissingSupply> = consolidate(requirements).mapNotNull { r ->
        val quantity=r.quantity
        if(quantity==null || quantity<=BigDecimal.ZERO || r.unit.isBlank() || r.unit.contains("ajustar")) return@mapNotNull MissingSupply(r.name,null,r.unit,r.tool)
        val available=inventory.filter {canonical(it.name)==canonical(r.name) && unit(it.unit)==unit(r.unit) && it.tool==r.tool}
            .fold(BigDecimal.ZERO){sum,s->sum+(s.quantity-s.reserved).max(BigDecimal.ZERO)}
        val deficit=(quantity-available).max(BigDecimal.ZERO)
        if(deficit>BigDecimal.ZERO) MissingSupply(r.name,deficit,r.unit,r.tool) else null
    }
    fun shopping(projects:List<List<Supply>>,inventory:List<Stock>):List<MissingSupply> {
        return missing(consolidate(projects.flatten()),inventory)
    }
}
