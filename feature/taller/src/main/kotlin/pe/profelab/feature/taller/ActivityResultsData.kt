package pe.profelab.feature.taller

import org.json.JSONObject
import pe.profelab.core.model.WorkRecord

/** Edit the historical fields in-place; records, attachments and unknown extensions stay intact. */
object ActivityResultsData {
    const val MAX_TEXT=20000
    val fields=setOf("results","analysis","limitations","conclusion")
    fun update(record:WorkRecord,field:String,value:String):JSONObject {
        require(record.kind in setOf("project","experiment") && !record.archived){"Comienza una copia personal para guardar resultados"}
        require(field in fields && value.length<=MAX_TEXT){"Revisa el campo o la longitud del texto"}
        return JSONObject(record.json).put(field,value)
    }
}
