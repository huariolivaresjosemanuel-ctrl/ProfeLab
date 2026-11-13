package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.newId
import java.util.UUID

/** Atomic private education graph on Room3 workspace_records; no new SQL table or public seed. */
class EducationRepository(private val store:WorkspaceStore) {
    private fun text(value:String,max:Int)=value.trim().also {require(it.isNotBlank() && it.length<=max){"Revisa el texto y su longitud"}}
    private fun validId(value:String)=value.also {require(runCatching {UUID.fromString(it).toString()==it}.getOrDefault(false)){"Identificador privado inválido"}}
    private fun row(id:String,kind:String,title:String,data:JSONObject,old:WorkRecord?=null):WorkRecord {
        val merged=JSONObject(old?.json ?: "{}");data.keys().forEach {key->merged.put(key,data.get(key))}
        return WorkRecord(id,kind,title,merged.put("schemaVersion",1).toString(),System.currentTimeMillis(),old?.revision ?: 1,old?.archived ?: false)
    }
    private fun existing(all:List<WorkRecord>,id:String,kind:String):WorkRecord?=all.singleOrNull {it.id==id}?.also {require(it.kind==kind && !it.archived){"Registro archivado o de otro tipo"}}
    private fun classroom(all:List<WorkRecord>,id:String)=EducationData.classrooms(all).single {it.id==id}.also {require(!it.archived){"El aula está archivada"}}

    suspend fun saveClassroom(name:String,grade:Int,section:String,schoolYear:Int,id:String=newId()):String {
        validId(id);val label=text(name,100);val group=text(section,30);require(grade in 1..6 && schoolYear in 2000..2200)
        store.editRecords {all->val old=existing(all,id,"edu_classroom")
            require(EducationData.classrooms(all).none {it.id!=id && !it.archived && it.grade==grade && it.schoolYear==schoolYear && it.section.equals(group,true) && it.name.equals(label,true)}){"Esta aula ya existe"}
            listOf(row(id,"edu_classroom",label,JSONObject().put("name",label).put("grade",grade).put("section",group).put("schoolYear",schoolYear),old))
        };return id
    }
    suspend fun saveStudent(classroomId:String,name:String,id:String=newId()):String {
        validId(id);val label=text(name,100)
        store.editRecords {all->classroom(all,classroomId);val old=existing(all,id,"edu_student")
            if(old!=null)require(JSONObject(old.json).getString("classroomId")==classroomId){"No se traslada un estudiante reescribiendo su historial"}
            require(EducationData.students(all).none {it.id!=id && !it.archived && it.classroomId==classroomId && it.displayName.equals(label,true)}){"Ese nombre ya está registrado en el aula; reutiliza el registro existente"}
            listOf(row(id,"edu_student",label,JSONObject().put("classroomId",classroomId).put("displayName",label),old))
        };return id
    }
    suspend fun saveTeam(classroomId:String,name:String,studentIds:Set<String>,id:String=newId()):String {
        validId(id);val label=text(name,100);require(studentIds.isNotEmpty() && studentIds.size<=100)
        store.editRecords {all->classroom(all,classroomId);val old=existing(all,id,"edu_team")
            if(old!=null)require(JSONObject(old.json).getString("classroomId")==classroomId)
            require(studentIds.all {studentId->EducationData.students(all).any {it.id==studentId && it.classroomId==classroomId && !it.archived}}){"Los integrantes deben pertenecer al aula activa"}
            listOf(row(id,"edu_team",label,JSONObject().put("classroomId",classroomId).put("name",label).put("studentIds",JSONArray(studentIds.sorted())),old))
        };return id
    }
    suspend fun createRun(work:WorkRecord,classroomId:String?,participantIds:Set<String>,teamIds:Set<String>,participantCount:Int,id:String=newId()):String {
        validId(id);require(participantCount in 1..100){"Revisa la cantidad de participantes (1–100)"}
        store.editRecords {all->
            val personal=all.single {it.id==work.id};require(personal.kind in setOf("project","experiment") && !personal.archived){"Comienza una copia personal antes de preparar una ejecución"}
            val room=classroomId?.let {classroom(all,it)}
            if(classroomId==null)require(participantIds.isEmpty() && teamIds.isEmpty())
            val teams=EducationData.teams(all).filter {it.id in teamIds};require(teams.size==teamIds.size && teams.all {!it.archived && it.classroomId==classroomId}){"Equipo de otra aula o archivado"}
            val ids=participantIds+teams.flatMap {it.studentIds};val students=EducationData.students(all).filter {it.id in ids}
            require(students.size==ids.size && students.all {!it.archived && it.classroomId==classroomId}){"Hay participantes ajenos al aula o archivados"}
            require(participantCount>=ids.size){"La cantidad no puede ser menor que los participantes seleccionados"}
            val view=WorkshopFlowAdapter.read(personal);val old=existing(all,id,"edu_run")
            if(old!=null){val previous=EducationData.runs(listOf(old)).single();require(previous.personalProjectId==personal.id && previous.classroomId==classroomId && previous.participantIds==ids && previous.participantCount==participantCount && previous.teams.map {it.teamId}.toSet()==teamIds){"La ejecución existente no se reemplaza con otros participantes"}}
            val body=JSONObject().put("personalProjectId",personal.id).put("activityId",view.activityId).put("contentVersion",view.contentVersion).put("activityTitle",personal.title)
                .put("classroomId",classroomId ?: "").put("classroomLabel",room?.let {"${it.name} · ${it.grade}.º ${it.section} · ${it.schoolYear}"} ?: "Sin nombres").put("participantCount",participantCount).put("createdAt",System.currentTimeMillis())
                .put("participants",JSONArray().also {a->students.sortedBy {it.id}.forEach {a.put(JSONObject().put("studentId",it.id).put("displayName",it.displayName))}})
                .put("teams",JSONArray().also {a->teams.sortedBy {it.id}.forEach {a.put(JSONObject().put("teamId",it.id).put("name",it.name).put("studentIds",JSONArray(it.studentIds.sorted())))}})
                .put("stepIds",JSONArray(view.steps.map {it.id})).put("stepProgress",JSONObject()).put("lastVisitedStepId","")
            val j=JSONObject(personal.json);val preparation=j.optJSONObject("teacherPreparation") ?: JSONObject()
            preparation.put("educationRunId",id).put("participation","STUDENTS").put("participantCount",participantCount)
            preparation.put("teamCount",maxOf(teams.size,1))
            j.put("teacherPreparation",preparation)
            listOfNotNull(if(old==null)row(id,"edu_run",personal.title,body)else null,personal.copy(json=j.toString(),updatedAt=System.currentTimeMillis()))
        };return id
    }
    suspend fun selectRun(personalProjectId:String,runId:String) {
        store.editRecords {all->val run=EducationData.runs(all).single {it.id==runId && it.personalProjectId==personalProjectId && !it.archived};val personal=all.single {it.id==personalProjectId};require(personal.kind in setOf("project","experiment") && !personal.archived)
            val j=JSONObject(personal.json);val prep=j.optJSONObject("teacherPreparation") ?: JSONObject();prep.put("educationRunId",run.id).put("participantCount",run.participantCount).put("participation","STUDENTS").put("teamCount",maxOf(run.teams.size,1))
            listOf(personal.copy(json=j.put("teacherPreparation",prep).toString(),updatedAt=System.currentTimeMillis()))
        }
    }
    suspend fun saveObservation(runId:String,target:EduTarget,criterion:EduCriterion,status:EduStatus,note:String,stepId:String?=null,id:String=newId(),evidenceIds:Set<String> = emptySet()):String {
        validId(id);val label=text(criterion.label,1000);require(criterion.id.isNotBlank() && criterion.id.length<=180 && note.length<=6000 && evidenceIds.size<=20)
        store.editRecords {all->val run=EducationData.runs(all).single {it.id==runId && !it.archived};val old=existing(all,id,"edu_observation")
            when(target.kind){EduTargetKind.STUDENT->require(target.id in run.participantIds);EduTargetKind.TEAM->require(run.teams.any {it.teamId==target.id});EduTargetKind.ACTIVITY->require(target.id==null)}
            require(stepId==null || stepId in run.stepIds){"El paso no pertenece a esta ejecución"}
            val previous=old?.let {EducationData.observations(listOf(it)).single()}
            if(previous!=null)require(previous.runId==runId && previous.target==target && previous.evidenceOwnerProjectId==run.personalProjectId){"Una observación no se reasigna a otra persona o aula"}
            val source=all.singleOrNull {it.id==run.personalProjectId};val allowed=source?.let {WorkshopEvidenceData.privateImageIds(JSONObject(it.json))}.orEmpty()+previous?.evidenceIds.orEmpty()
            require(evidenceIds.all {it in allowed}){"Solo se pueden vincular evidencias privadas de este proyecto"}
            val body=JSONObject().put("runId",run.id).put("classroomId",run.classroomId ?: "").put("targetKind",target.kind.name).put("targetId",target.id ?: "")
                .put("criterionId",criterion.id).put("criterionLabel",label).put("status",status.name).put("note",note.trim()).put("stepId",stepId ?: "").put("at",old?.let {JSONObject(it.json).getLong("at")} ?: System.currentTimeMillis()).put("evidenceOwnerProjectId",run.personalProjectId).put("evidenceIds",JSONArray(evidenceIds.sorted()))
            listOf(row(id,"edu_observation","Observación formativa",body,old))
        };return id
    }
    suspend fun savePhrase(value:String,id:String=newId()):String {validId(id);val phrase=text(value,1000);store.editRecords {all->val old=existing(all,id,"edu_phrase");listOf(row(id,"edu_phrase","Frase docente",JSONObject().put("text",phrase),old))};return id}
    suspend fun archive(id:String){store.editRecords {all->val old=all.single {it.id==id};require(old.kind in EducationData.kinds);listOf(old.copy(archived=true,updatedAt=System.currentTimeMillis()))}}
}
