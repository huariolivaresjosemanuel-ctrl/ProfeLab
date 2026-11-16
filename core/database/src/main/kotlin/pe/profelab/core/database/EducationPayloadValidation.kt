package pe.profelab.core.database

import org.json.JSONObject
import pe.profelab.core.model.JsonLimits
import pe.profelab.core.model.WorkRecord

/** Authentication alone does not establish that a legacy/decrypted educational payload is readable. */
internal fun validateEducationPayload(record:WorkRecord){
    JsonLimits.requireBounded(record.json)
    val json=JSONObject(record.json)
    if(record.kind.startsWith("edu_")){
        val version=json.get("schemaVersion")
        require(version is Number && version.toDouble()==1.0){"Unsupported private schema"}
    }
}
