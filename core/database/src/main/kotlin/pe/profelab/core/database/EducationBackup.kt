package pe.profelab.core.database

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.JsonLimits
import java.util.UUID

/** Private aggregate validation before file copies and again inside the restore transaction. */
internal object EducationBackup {
    private val kinds=setOf("edu_classroom","edu_student","edu_team","edu_run","edu_observation","edu_phrase")
    private data class Row(val id:String,val kind:String,val body:JSONObject)
    private fun rows(data:JSONObject):List<Row> {
        val array=data.getJSONArray("workspace_records")
        return (0 until array.length()).map {i->val row=array.getJSONObject(i);val body=row.getString("json")
            JsonLimits.requireBounded(body);Row(row.getString("id"),row.getString("kind"),JSONObject(body))}
    }
    private fun uuid(value:String):String=value.also {require(runCatching {UUID.fromString(it).toString()==it}.getOrDefault(false)){"Identificador educativo privado inválido"}}
    private fun text(j:JSONObject,key:String,max:Int,blank:Boolean=false):String {
        require(j.get(key) is String);return j.getString(key).also {require(it.length<=max && (blank || it.isNotBlank()))}
    }
    private fun number(j:JSONObject,key:String,min:Long,max:Long):Long {
        val value=j.get(key);require(value is Number)
        val n=value.toDouble();require(n.isFinite() && n==value.toLong().toDouble() && value.toLong() in min..max)
        return value.toLong()
    }
    private fun strings(j:JSONObject,key:String,max:Int,privateIds:Boolean=false):Set<String> {
        val values=j.getJSONArray(key);require(values.length()<=max)
        val ids=(0 until values.length()).map {i->require(values.get(i) is String);values.getString(i).also {require(it.isNotBlank() && it.length<=4000);if(privateIds)uuid(it)}}
        require(ids.distinct().size==ids.size);return ids.toSet()
    }
    private fun same(a:Any?,b:Any?):Boolean=when {
        a is JSONObject && b is JSONObject->a.keys().asSequence().toSet()==b.keys().asSequence().toSet() && a.keys().asSequence().all {same(a.get(it),b.get(it))}
        a is JSONArray && b is JSONArray->a.length()==b.length() && (0 until a.length()).all {same(a.get(it),b.get(it))}
        else->a==b
    }
    fun validate(data:JSONObject){validateRows(rows(data))}
    private fun validateRows(rows:List<Row>){
        require(rows.map {it.id}.distinct().size==rows.size){"Identificadores repetidos en el respaldo"}
        val all=rows.associateBy {it.id};val privateRows=rows.filter {it.kind.startsWith("edu_")}
        fun parent(id:String,kind:String):Row=requireNotNull(all[id]).also {require(it.kind==kind){"Relación educativa de otro tipo"}}
        fun student(id:String,classroomId:String){require(parent(id,"edu_student").body.getString("classroomId")==classroomId){"Un estudiante pertenece a otra aula"}}
        privateRows.forEach {row->
            require(row.kind in kinds){"Tipo educativo no compatible con esta versión"};uuid(row.id);number(row.body,"schemaVersion",1,1)
            val j=row.body
            when(row.kind){
                "edu_classroom"->{text(j,"name",100);number(j,"grade",1,6);text(j,"section",30);number(j,"schoolYear",2000,2200)}
                "edu_student"->{parent(text(j,"classroomId",100),"edu_classroom");text(j,"displayName",100)}
                "edu_team"->{val group=text(j,"classroomId",100);parent(group,"edu_classroom");text(j,"name",100)
                    val members=strings(j,"studentIds",1000,true);require(members.isNotEmpty());members.forEach {student(it,group)}}
                "edu_run"->{
                    val work=requireNotNull(all[text(j,"personalProjectId",160)])
                    require(work.kind in setOf("project","experiment"))
                    val activity=text(j,"activityId",180);text(j,"contentVersion",100);text(j,"activityTitle",10000,true)
                    val workActivity=work.body.optString("activityId").ifBlank {work.body.optString("forkedFromTemplateId").ifBlank {work.id}}
                    require(activity==workActivity){"La ejecución no corresponde a su proyecto personal"}
                    val group=text(j,"classroomId",100,true);if(group.isNotBlank())parent(group,"edu_classroom")
                    val count=number(j,"participantCount",1,100);number(j,"createdAt",0,Long.MAX_VALUE);val stepIds=strings(j,"stepIds",1000)
                    if(j.has("lastVisitedStepId")){val last=text(j,"lastVisitedStepId",4000,true);require(last.isBlank() || last in stepIds)}
                    if(j.has("stepProgress")){
                        val progress=j.getJSONObject("stepProgress");require(progress.length()<=stepIds.size)
                        progress.keys().forEach {stepId->
                            require(stepId in stepIds){"El progreso contiene un paso ajeno a esta ejecución"}
                            val state=progress.getJSONObject(stepId)
                            if(state.has("done"))require(state.get("done") is Boolean)
                            if(state.has("notes"))text(state,"notes",4000,true)
                        }
                    }
                    val participants=j.getJSONArray("participants");require(participants.length()<=1000)
                    val ids=(0 until participants.length()).map {i->val p=participants.getJSONObject(i);text(p,"displayName",100);uuid(text(p,"studentId",100))}
                    require(ids.distinct().size==ids.size && count>=ids.size)
                    ids.forEach {student(it,group)}
                    val teams=j.getJSONArray("teams");require(teams.length()<=1000);val teamIds=mutableSetOf<String>()
                    for(i in 0 until teams.length()){
                        val team=teams.getJSONObject(i);val id=uuid(text(team,"teamId",100));require(teamIds.add(id));text(team,"name",100)
                        require(parent(id,"edu_team").body.getString("classroomId")==group){"Equipo de otra aula"}
                        require(strings(team,"studentIds",1000,true).all {it in ids}){"La captura de equipo contiene participantes ajenos"}
                    }
                    if(group.isBlank())require(ids.isEmpty() && teamIds.isEmpty())
                }
                "edu_observation"->{
                    val run=parent(text(j,"runId",100),"edu_run").body
                    require(text(j,"classroomId",100,true)==run.getString("classroomId")){"Observación de otra aula"}
                    val target=text(j,"targetId",100,true)
                    when(text(j,"targetKind",20)){
                        "STUDENT"->require((0 until run.getJSONArray("participants").length()).any {run.getJSONArray("participants").getJSONObject(it).getString("studentId")==target})
                        "TEAM"->require((0 until run.getJSONArray("teams").length()).any {run.getJSONArray("teams").getJSONObject(it).getString("teamId")==target})
                        "ACTIVITY"->require(target.isBlank())
                        else->error("Destino de observación no compatible")
                    }
                    val step=text(j,"stepId",4000,true);require(step.isBlank() || step in strings(run,"stepIds",1000))
                    text(j,"criterionId",180);text(j,"criterionLabel",1000);text(j,"note",6000,true);number(j,"at",0,Long.MAX_VALUE)
                    require(text(j,"status",30) in setOf("AUTONOMOUS","WITH_SUPPORT","IN_PROGRESS","NOT_OBSERVED"))
                    val evidence=strings(j,"evidenceIds",20)
                    evidence.forEach {require(it.matches(Regex("[a-zA-Z0-9-]+"))){"Una evidencia debe ser un archivo privado, no una ruta editorial"}}
                    // The source link may have been removed later; retain the historic evaluation.
                    if(evidence.isNotEmpty() || j.has("evidenceOwnerProjectId"))require(text(j,"evidenceOwnerProjectId",160)==run.getString("personalProjectId")){"La evidencia histórica pertenece a otro proyecto"}
                }
                "edu_phrase"->text(j,"text",1000)
            }
        }
        rows.filter {it.kind in setOf("project","experiment")}.forEach {project->
            val selected=project.body.optJSONObject("teacherPreparation")?.optString("educationRunId").orEmpty()
            if(selected.isNotBlank())require(parent(selected,"edu_run").body.getString("personalProjectId")==project.id){"La preparación apunta a otra ejecución"}
        }
    }

    fun checkConflicts(data:JSONObject,db:ProfeDatabase){
        val incoming=rows(data);val current=mutableListOf<Row>()
        db.openHelper.writableDatabase.query("SELECT id,kind,json FROM workspace_records").use {c->while(c.moveToNext()){
            val body=c.getString(2);JsonLimits.requireBounded(body);current+=Row(c.getString(0),c.getString(1),JSONObject(body))
        }}
        val byId=current.associateBy {it.id}
        incoming.forEach {row->byId[row.id]?.let {old->
            if(row.kind.startsWith("edu_") || old.kind.startsWith("edu_"))require(row.kind==old.kind && same(row.body,old.body)){"Conflicto entre registros educativos privados; no se fusionan relaciones distintas"}
        }}
        val merged=current+incoming.filter {it.id !in byId}
        validateRows(merged)
    }
}
