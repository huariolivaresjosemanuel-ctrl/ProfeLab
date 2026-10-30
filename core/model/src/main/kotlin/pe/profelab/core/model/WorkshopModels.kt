package pe.profelab.core.model

import java.math.BigDecimal
import java.util.UUID

enum class MaterialKind(val label:String) { CONSUMABLE("Consumible"), REUSABLE("Reutilizable"), TOOL("Herramienta") }
data class MaterialDefinition(val id:String,val name:String,val synonyms:List<String> = emptyList(),val category:String="Otros",val unit:String="unidad",val kind:MaterialKind=MaterialKind.CONSUMABLE,val warnings:List<String> = emptyList(),val alternatives:List<MaterialAlternative> = emptyList())
data class MaterialAlternative(val materialId:String,val name:String,val ratio:BigDecimal,val unit:String,val reason:String,val source:String,val verified:Boolean)
data class MaterialLine(val id:String,val materialId:String,val name:String,val amount:String,val unit:String,val kind:MaterialKind,
    val contentAmount:String="",val contentUnit:String="",val alternatives:List<MaterialAlternative> = emptyList(),val warning:String="") {
    fun supply()=Supply(name,if(warning.isBlank())InventoryRules.decimal(amount)else null,unit,kind==MaterialKind.TOOL,kind==MaterialKind.REUSABLE,materialId,InventoryRules.decimal(contentAmount),contentUnit)
}
data class RequirementCoverage(val requirement:Supply,val available:BigDecimal?,val missing:BigDecimal?,val review:String="") {
    val covered:Boolean get()=review.isEmpty() && missing?.signum()==0
}
enum class SafetyStatus(val label:String) { PENDING("Seguridad pendiente de revisión"), ADULT("Requiere preparación adulta"), REVIEWED("Revisada por mí; supervisar"), FORBIDDEN("No recomendado por seguridad") }
data class WorkshopAssessment(val coverage:List<RequirementCoverage>,val safety:SafetyStatus) {
    val complete get()=coverage.isNotEmpty() && coverage.all {it.covered}
    val covered get()=coverage.count {it.covered}
    val score:Double get()=if(coverage.isEmpty())0.0 else coverage.map {c->val q=c.requirement.quantity;val a=c.available;if(q==null || q.signum()<=0 || a==null || c.review.isNotBlank())0.0 else a.divide(q,java.math.MathContext.DECIMAL64).toDouble().coerceIn(0.0,1.0)}.average()
    val recommendation get()=when {safety==SafetyStatus.FORBIDDEN->"No recomendado por seguridad";safety==SafetyStatus.PENDING->"Revisión de seguridad pendiente";!complete->"Me faltan materiales";safety==SafetyStatus.ADULT->"Requiere preparación adulta";else->"Puedo hacerlo"}
}
fun legacyMaterialId(name:String)="local:"+UUID.nameUUIDFromBytes(InventoryRules.canonical(name).toByteArray(Charsets.UTF_8))

/** Lossless parsing result: malformed input is retained for a repair UI, never guessed as available stock. */
data class LegacyMaterial(val name:String,val amount:String,val unit:String,val kind:MaterialKind,val issue:String="",val original:String="")
object LegacyMaterialParser {
    fun parse(text:String):List<LegacyMaterial> = text.lineSequence().filter {it.isNotBlank()}.map {line->
        val p=line.split('|');val name=p.getOrElse(0){""}.trim();val amount=p.getOrElse(1){""}.trim();val unit=p.getOrElse(2){""}.trim()
        val category=InventoryRules.canonical(p.getOrElse(3){"consumible"})
        val kind=when(category){"herramienta","tool"->MaterialKind.TOOL;"reutilizable","reusable"->MaterialKind.REUSABLE;else->MaterialKind.CONSUMABLE}
        val issue=when {p.size!=4->"Registro antiguo incompleto o con separadores adicionales";name.isBlank()->"Falta nombre";InventoryRules.decimal(amount)?.signum()!=1->"Revisar cantidad";unit.isBlank()->"Falta unidad";category !in setOf("consumible","consumable","reutilizable","reusable","herramienta","tool")->"Revisar tipo de material";else->""}
        LegacyMaterial(name,amount,unit,kind,issue,line)
    }.toList()
}
