package pe.profelab.core.content

import org.json.JSONObject
import pe.profelab.core.model.*
import java.math.BigDecimal

/** Personal preparation only. Editorial definitions and inventory never change here. */
object WorkshopPreparation {
    fun mergeExternalRun(snapshot:JSONObject,stored:JSONObject):JSONObject {
        val result=JSONObject(snapshot.toString());val actual=stored.optJSONObject("teacherPreparation") ?: return result
        val run=actual.optString("educationRunId");if(run.isBlank())return result
        val desired=result.optJSONObject("teacherPreparation") ?: JSONObject();val changed=desired.optString("educationRunId")!=run
        desired.put("educationRunId",run)
        if(changed)listOf("participantCount","teamCount","participation").forEach {key->if(actual.has(key))desired.put(key,actual.get(key))}
        return result.put("teacherPreparation",desired)
    }
    private fun personal(record:WorkRecord){require(record.kind in setOf("project","experiment") && !record.archived){"Comienza una copia personal para guardar la preparación"}}
    fun participation(record:WorkRecord,mode:String,participants:Int,teams:Int):JSONObject {
        personal(record);require(mode in setOf("INDIVIDUAL","STUDENTS"));require(participants in 1..100 && teams in 1..participants)
        val data=JSONObject(record.json);val prep=data.optJSONObject("teacherPreparation") ?: JSONObject()
        prep.put("participation",mode).put("participantCount",if(mode=="INDIVIDUAL")1 else participants).put("teamCount",if(mode=="INDIVIDUAL")1 else teams)
        return data.put("teacherPreparation",prep)
    }
    fun materialRule(record:WorkRecord,catalog:List<MaterialDefinition>,lineId:String,basis:String,baseQuantity:String,confirmed:Boolean):JSONObject {
        personal(record);require(confirmed){"Confirma la regla antes de calcular cantidades"}
        require(basis in setOf("COMMON","STUDENT","TEAM","ORIGINAL"))
        val data=WorkshopCodec.normalize(JSONObject(record.json),catalog);val materials=data.getJSONArray("materials")
        val lines=WorkshopCodec.materials(data,catalog);val index=lines.indexOfFirst {it.id==lineId};require(index>=0)
        val line=lines[index];val prep=data.optJSONObject("teacherPreparation") ?: JSONObject()
        val rules=prep.optJSONObject("materialRules") ?: JSONObject();val previous=rules.optJSONObject(lineId)
        val original=previous?.optString("originalQuantity") ?: line.amount
        val base=InventoryRules.decimal(if(basis=="ORIGINAL")original else baseQuantity)
        require(base!=null && base.signum()>0 && base.precision()<=12 && base.scale()<=6){"Escribe una cantidad positiva válida"}
        require(line.unit.isNotBlank() && line.warning.isBlank() && !line.unit.contains("referencial",true) && !line.unit.contains("ajustar",true)){"Define primero una unidad y requisito sin ambigüedad en el editor"}
        val multiplier=when(basis){"STUDENT"->prep.optInt("participantCount",1);"TEAM"->prep.optInt("teamCount",1);else->1};require(multiplier in 1..100)
        val amount=base.multiply(BigDecimal(multiplier)).stripTrailingZeros().toPlainString()
        materials.getJSONObject(index).put("amount",amount)
        rules.put(lineId,JSONObject().put("basis",basis).put("baseQuantity",base.toPlainString()).put("originalQuantity",original)
            .put("appliedQuantity",amount).put("unit",line.unit).put("multiplier",multiplier).put("confirmedByTeacher",true))
        return data.put("teacherPreparation",prep.put("materialRules",rules))
    }
}
