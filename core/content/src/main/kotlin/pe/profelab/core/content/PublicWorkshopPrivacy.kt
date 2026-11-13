package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject

/** Removes the defined private education contract from all levels of a public content item. */
object PublicWorkshopPrivacy {
    private val fields=setOf("teacherPreparation","educationRunId","classroomAssociation","classroomId","classroomIds","studentId","studentIds",
        "studentNames","students","classrooms","teamId","teamIds","teams","participantIds","participants","evaluations","educationObservations",
        "displayName","classroomLabel","evidenceOwnerProjectId")
    fun sanitize(value:JSONObject):JSONObject {
        fun clean(raw:Any?,depth:Int):Any? {
            require(depth<=40){"Contenido anidado demasiado profundo"}
            return when(raw){
                is JSONObject->JSONObject().also {out->raw.keys().forEach {key->if(key !in fields && !key.startsWith("edu_"))out.put(key,clean(raw.get(key),depth+1))}}
                is JSONArray->JSONArray().also {out->for(i in 0 until raw.length())out.put(clean(raw.get(i),depth+1))}
                else->raw
            }
        }
        return clean(value,0) as JSONObject
    }
}
