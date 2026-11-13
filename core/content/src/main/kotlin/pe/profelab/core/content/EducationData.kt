package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord

/** Bounded projections skip malformed unrelated rows; mutations always validate the current transaction. */
object EducationData {
    val kinds=setOf("edu_classroom","edu_student","edu_team","edu_run","edu_observation","edu_phrase")
    val suggestedPhrases=listOf("Participa activamente.","Requiere apoyo para comprender instrucciones.","Organiza los materiales.","Colabora con sus compañeros.","Necesita practicar la explicación del procedimiento.","Muestra autonomía.","Necesita practicar el respeto de turnos.")
    fun strings(array:JSONArray?):Set<String> = if(array==null)emptySet()else (0 until array.length()).map {array.getString(it)}.toSet()
    private fun <T> rows(all:List<WorkRecord>,kind:String,decode:(WorkRecord,JSONObject)->T):List<T> = all.filter {it.kind==kind}.mapNotNull {r->runCatching {val j=JSONObject(r.json);require(j.getInt("schemaVersion")==1);decode(r,j)}.getOrNull()}
    fun classrooms(all:List<WorkRecord>)=rows(all,"edu_classroom"){r,j->EduClassroom(r.id,j.getString("name"),j.getInt("grade"),j.getString("section"),j.getInt("schoolYear"),r.archived)}
    fun students(all:List<WorkRecord>)=rows(all,"edu_student"){r,j->EduStudent(r.id,j.getString("classroomId"),j.getString("displayName"),r.archived)}
    fun teams(all:List<WorkRecord>)=rows(all,"edu_team"){r,j->EduTeam(r.id,j.getString("classroomId"),j.getString("name"),strings(j.getJSONArray("studentIds")),r.archived)}
    fun runs(all:List<WorkRecord>)=rows(all,"edu_run"){r,j->
        val participants=j.getJSONArray("participants");val teams=j.getJSONArray("teams")
        EduRun(r.id,j.getString("personalProjectId"),j.getString("activityId"),j.getString("contentVersion"),j.getString("activityTitle"),j.optString("classroomId").takeIf {it.isNotBlank()},j.getInt("participantCount"),
            (0 until participants.length()).map {i->val x=participants.getJSONObject(i);EduParticipantSnapshot(x.getString("studentId"),x.getString("displayName"))},
            (0 until teams.length()).map {i->val x=teams.getJSONObject(i);EduTeamSnapshot(x.getString("teamId"),x.getString("name"),strings(x.getJSONArray("studentIds")))},strings(j.getJSONArray("stepIds")),j.getLong("createdAt"),r.archived,j.optString("classroomLabel"))
    }
    fun observations(all:List<WorkRecord>)=rows(all,"edu_observation"){r,j->EduObservation(r.id,j.optString("classroomId").takeIf {it.isNotBlank()},j.getString("runId"),EduTarget(EduTargetKind.valueOf(j.getString("targetKind")),j.optString("targetId").takeIf {it.isNotBlank()}),EduCriterion(j.getString("criterionId"),j.getString("criterionLabel")),EduStatus.valueOf(j.getString("status")),j.getString("note"),j.optString("stepId").takeIf {it.isNotBlank()},j.getLong("at"),strings(j.optJSONArray("evidenceIds")),r.archived,j.optString("evidenceOwnerProjectId"))}
    fun phrases(all:List<WorkRecord>)=rows(all,"edu_phrase"){r,j->EduPhrase(r.id,j.getString("text"),r.archived)}
    fun selectedRun(record:WorkRecord,all:List<WorkRecord>):EduRun? {
        if(record.archived || record.kind !in setOf("project","experiment"))return null
        val preparation=runCatching {JSONObject(record.json).optJSONObject("teacherPreparation")}.getOrNull()
        if(preparation?.optString("participation")!="STUDENTS")return null
        val selected=preparation?.optString("educationRunId")
        return runs(all).singleOrNull {it.id==selected && it.personalProjectId==record.id && !it.archived}
    }
}
