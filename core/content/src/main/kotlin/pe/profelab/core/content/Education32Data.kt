package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.education.EducationFamilyPolicy
import pe.profelab.core.model.education.EducationAssessmentPolicy
import java.util.UUID

/** Read projections over the existing private transactional store; reading never performs migration. */
object Education32Data {
    val kinds=setOf("edu_enrollment","edu_subject","edu_assessment_template","edu_judgment","edu_guardian","edu_family_draft","edu_academic_assessment")
    internal fun legacyEnrollmentId(student:EduStudent)=UUID.nameUUIDFromBytes("profelab:enrollment:${student.id}:${student.classroomId}".toByteArray(Charsets.UTF_8)).toString()
    private fun <T> rows(all:List<WorkRecord>,kind:String,decode:(WorkRecord,JSONObject)->T):List<T> = all.filter {it.kind==kind}.mapNotNull {row->runCatching {val j=JSONObject(row.json);require(j.getInt("schemaVersion")==1);decode(row,j)}.getOrNull()}
    private fun explicitEnrollments(all:List<WorkRecord>)=rows(all,"edu_enrollment"){r,j->EduEnrollment(r.id,j.getString("studentId"),j.getString("classroomId"),j.getInt("schoolYear"),j.getBoolean("active"),j.getLong("createdAt"),r.archived)}
    fun enrollments(all:List<WorkRecord>):List<EduEnrollment> {
        val saved=explicitEnrollments(all);val savedIds=saved.map {it.id}.toSet();val rooms=EducationData.classrooms(all).associateBy {it.id}
        return saved+EducationData.students(all).mapNotNull {student->
            val room=rooms[student.classroomId] ?: return@mapNotNull null
            val id=legacyEnrollmentId(student)
            if(id in savedIds)null else EduEnrollment(id,student.id,room.id,room.schoolYear,true,0,student.archived)
        }
    }
    fun belongsToClassroom(all:List<WorkRecord>,studentId:String,classroomId:String,activeOnly:Boolean=true)=enrollments(all).any {it.studentId==studentId && it.classroomId==classroomId && (!activeOnly || (it.active && !it.archived))}
    fun studentsInClassroom(all:List<WorkRecord>,classroomId:String):List<EduStudent> {
        val ids=enrollments(all).filter {it.classroomId==classroomId && it.active && !it.archived}.map {it.studentId}.toSet()
        return EducationData.students(all).filter {!it.archived && it.id in ids}.map {it.copy(classroomId=classroomId)}
    }
    fun offerings(all:List<WorkRecord>)=rows(all,"edu_subject"){r,j->EduSubjectOffering(r.id,j.getString("classroomId"),j.getString("name"),r.archived)}
    fun templates(all:List<WorkRecord>)=rows(all,"edu_assessment_template"){r,j->
        val criteria=j.getJSONArray("criteria")
        EduAssessmentTemplate(r.id,j.getString("templateKey"),j.getInt("version"),j.getString("subjectId"),j.getString("competency"),j.getInt("grade"),j.getString("activityId"),EduInstrumentKind.valueOf(j.getString("kind")),
            (0 until criteria.length()).map {index->val c=criteria.getJSONObject(index);val d=c.getJSONObject("descriptors");EduRubricCriterion(c.getString("id"),c.getString("label"),d.keys().asSequence().associateWith {d.getString(it)})},j.getString("sourceStatus"),r.archived)
    }
    private fun observationSnapshots(j:JSONObject):List<EduObservation> {
        val array=j.optJSONArray("observationSnapshots") ?: return emptyList()
        return (0 until array.length()).map {index->val snapshot=array.getJSONObject(index);EducationData.observations(listOf(WorkRecord(snapshot.getString("id"),"edu_observation","",snapshot.toString(),0))).single()}
    }
    fun judgments(all:List<WorkRecord>)=rows(all,"edu_judgment"){r,j->EduCompetencyJudgment(r.id,j.getString("studentId"),j.getString("enrollmentId"),j.getString("templateId"),j.getString("criterionId"),j.optString("level").takeIf {it.isNotBlank()}?.let(EduAchievementLevel::valueOf),EducationData.strings(j.getJSONArray("observationIds")),j.getString("teacher"),j.getString("description"),EduJudgmentState.valueOf(j.getString("state")),j.getLong("createdAt"),j.optString("supersedesId").takeIf {it.isNotBlank()},r.archived,j.optString("evidenceSummary"),observationSnapshots(j))}
    fun guardians(all:List<WorkRecord>)=rows(all,"edu_guardian"){r,j->EduGuardianContact(r.id,j.getString("studentId"),j.getString("name"),j.getString("relationship"),j.getString("phone"),j.getString("email"),EduFamilyChannel.valueOf(j.getString("preferredChannel")),j.getBoolean("authorized"),j.getString("scope"),j.getLong("validFrom"),j.getLong("validUntil"),j.getBoolean("verified"),j.optLong("revokedAt",0).takeIf {it>0},r.archived)}
    fun drafts(all:List<WorkRecord>)=rows(all,"edu_family_draft"){r,j->EduFamilyDraft(r.id,j.getString("studentId"),j.getString("guardianId"),EducationData.strings(j.getJSONArray("observationIds")),j.getString("body"),j.getLong("createdAt"),EduShareState.valueOf(j.getString("state")),j.optString("channel").takeIf {it.isNotBlank()}?.let(EduFamilyChannel::valueOf),r.archived,EducationData.strings(j.optJSONArray("academicAssessmentIds")))}
    fun academicAssessments(all:List<WorkRecord>)=rows(all,"edu_academic_assessment"){r,j->EduAcademicAssessment(r.id,j.getString("studentId"),j.getString("enrollmentId"),j.getString("subjectId"),j.getString("competency"),j.getString("criterion"),j.getString("period"),EduAcademicTaskKind.valueOf(j.getString("taskKind")),EduOriginalScale.valueOf(j.getString("scale")),j.getString("value"),j.getString("evidence"),j.getString("observation"),j.getString("improvementAction"),j.getString("teacher"),EduAssessmentOrigin.valueOf(j.getString("origin")),j.optString("sourceRecordId").takeIf {it.isNotBlank()},j.getLong("createdAt"),r.archived,j.optString("templateId").takeIf {it.isNotBlank()},j.optString("templateCriterionId").takeIf {it.isNotBlank()})}
    fun timeline(all:List<WorkRecord>,studentId:String):List<EduObservation> = EducationData.observations(all).filter {it.target==EduTarget(EduTargetKind.STUDENT,studentId) && !it.archived}.sortedByDescending {it.at}
    fun shareBlockReason(contact:EduGuardianContact?,now:Long=System.currentTimeMillis()):String? = if(contact==null)"Selecciona y revisa un contacto autorizado." else EducationFamilyPolicy.authorizationBlock(contact.authorized,contact.verified,contact.scope,contact.validFrom,contact.validUntil,contact.revokedAt,contact.archived,now)

    /** Validates new private graph types before backup restore or writes; historical consents may expire. */
    fun validateGraph(all:List<WorkRecord>) {
        require(all.map {it.id}.distinct().size==all.size){"Identidades privadas repetidas"}
        val relevant=all.filter {it.kind in kinds};val byId=all.associateBy {it.id}
        fun parent(id:String,kind:String)=requireNotNull(byId[id]).also {require(it.kind==kind){"Relación educativa incompatible"}}
        fun bounded(j:JSONObject,key:String,max:Int,blank:Boolean=false):String {require(j.get(key) is String);return j.getString(key).also {require(it.length<=max && (blank || it.isNotBlank()))}}
        fun ids(j:JSONObject,key:String,max:Int):Set<String> {val array=j.getJSONArray(key);require(array.length()<=max);val result=(0 until array.length()).map {require(array.get(it) is String);array.getString(it).also {id->require(runCatching {UUID.fromString(id).toString()==id}.getOrDefault(false))}};require(result.distinct().size==result.size);return result.toSet()}
        fun long(j:JSONObject,key:String):Long {val n=j.get(key);require(n is Number && n.toDouble().isFinite() && n.toDouble()==n.toLong().toDouble() && n.toLong()>=0);return n.toLong()}
        relevant.forEach {row->
            require(runCatching {UUID.fromString(row.id).toString()==row.id}.getOrDefault(false));val j=JSONObject(row.json);require(j.getInt("schemaVersion")==1)
            when(row.kind){
                "edu_enrollment"->{parent(bounded(j,"studentId",100),"edu_student");val room=JSONObject(parent(bounded(j,"classroomId",100),"edu_classroom").json);require(j.getInt("schoolYear")==room.getInt("schoolYear"));require(j.get("active") is Boolean);long(j,"createdAt")}
                "edu_subject"->{parent(bounded(j,"classroomId",100),"edu_classroom");bounded(j,"name",100)}
                "edu_assessment_template"->{
                    val subject=JSONObject(parent(bounded(j,"subjectId",100),"edu_subject").json);val room=JSONObject(parent(subject.getString("classroomId"),"edu_classroom").json)
                    require(UUID.fromString(bounded(j,"templateKey",100)).toString()==j.getString("templateKey"));require(j.getInt("version") in 1..100000 && j.getInt("grade")==room.getInt("grade"))
                    bounded(j,"competency",1000);bounded(j,"activityId",180,true);require(j.getString("kind") in EduInstrumentKind.entries.map {it.name});require(j.getString("sourceStatus")=="SUGERIDO_NO_HOMOLOGADO")
                    val c=j.getJSONArray("criteria");require(c.length() in 1..30);val criterionIds=mutableSetOf<String>()
                    for(i in 0 until c.length()){val criterion=c.getJSONObject(i);require(criterionIds.add(bounded(criterion,"id",180)));bounded(criterion,"label",1000);val d=criterion.getJSONObject("descriptors");require(d.length()<=10);d.keys().forEach {key->require(key.length<=80);bounded(d,key,2000)};if(j.getString("kind")==EduInstrumentKind.ANALYTIC_RUBRIC.name)require(EduAchievementLevel.entries.all {d.has(it.name)})}
                }
                "edu_judgment"->{bounded(j,"studentId",100);bounded(j,"enrollmentId",100);bounded(j,"templateId",100);bounded(j,"criterionId",180);bounded(j,"teacher",100,true);bounded(j,"description",6000,true);long(j,"createdAt");ids(j,"observationIds",30);require(j.getString("state") in EduJudgmentState.entries.map {it.name});val level=bounded(j,"level",10,true);require(level.isBlank() || level in EduAchievementLevel.entries.map {it.name});bounded(j,"supersedesId",100,true);if(j.has("evidenceSummary"))bounded(j,"evidenceSummary",250000,true)}
                "edu_guardian"->{parent(bounded(j,"studentId",100),"edu_student");bounded(j,"name",100);bounded(j,"relationship",100);require(EducationFamilyPolicy.validPhone(bounded(j,"phone",30,true)) && EducationFamilyPolicy.validEmail(bounded(j,"email",254,true)));require(j.getString("preferredChannel") in EduFamilyChannel.entries.map {it.name});require(j.get("authorized") is Boolean && j.get("verified") is Boolean);bounded(j,"scope",100);val from=long(j,"validFrom");require(long(j,"validUntil")>=from);long(j,"revokedAt")}
                "edu_family_draft"->{parent(bounded(j,"studentId",100),"edu_student");parent(bounded(j,"guardianId",100),"edu_guardian");require(EducationFamilyPolicy.safeBody(bounded(j,"body",10000)));ids(j,"observationIds",30);if(j.has("academicAssessmentIds"))ids(j,"academicAssessmentIds",30);long(j,"createdAt");require(j.getString("state") in EduShareState.entries.map {it.name});val channel=bounded(j,"channel",20,true);require(channel.isBlank() || channel in EduFamilyChannel.entries.map {it.name})}
                "edu_academic_assessment"->{parent(bounded(j,"studentId",100),"edu_student");bounded(j,"enrollmentId",100);parent(bounded(j,"subjectId",100),"edu_subject");bounded(j,"competency",1000);bounded(j,"criterion",1000);bounded(j,"period",100);require(j.getString("taskKind") in EduAcademicTaskKind.entries.map {it.name});require(EducationAssessmentPolicy.validOriginalValue(bounded(j,"scale",30),bounded(j,"value",2000)));bounded(j,"evidence",6000);bounded(j,"observation",6000,true);bounded(j,"improvementAction",6000,true);bounded(j,"teacher",100);require(j.getString("origin") in EduAssessmentOrigin.entries.map {it.name});bounded(j,"sourceRecordId",100,true);long(j,"createdAt")}
            }
        }
        require(explicitEnrollments(all).size==relevant.count {it.kind=="edu_enrollment"})
        val enrollments=enrollments(all);require(enrollments.filter {it.active && !it.archived}.groupBy {it.studentId to it.schoolYear}.values.all {it.size<=1}){"Matrículas activas duplicadas en el mismo año"}
        val offerings=offerings(all).associateBy {it.id};val templates=templates(all);val byTemplate=templates.associateBy {it.id};require(templates.groupBy {it.templateKey to it.version}.values.all {it.size==1})
        templates.groupBy {it.templateKey}.values.forEach {versions->require(versions.map {it.subjectId}.distinct().size==1)}
        val observations=EducationData.observations(all).associateBy {it.id};val judgments=judgments(all);val byJudgment=judgments.associateBy {it.id}
        require(judgments.size==relevant.count {it.kind=="edu_judgment"}){"Valoración o snapshot no compatible"}
        judgments.forEach {j->
            parent(j.studentId,"edu_student");val enrollment=enrollments.single {it.id==j.enrollmentId};require(enrollment.studentId==j.studentId)
            val template=requireNotNull(byTemplate[j.templateId]);require(requireNotNull(offerings[template.subjectId]).classroomId==enrollment.classroomId && template.criteria.any {it.id==j.criterionId})
            j.observationIds.forEach {id->val observation=requireNotNull(observations[id]);require(observation.target==EduTarget(EduTargetKind.STUDENT,j.studentId) && observation.classroomId==enrollment.classroomId)}
            if(JSONObject(requireNotNull(byId[j.id]).json).has("observationSnapshots")){
                require(j.observationSnapshots.size<=30 && j.observationSnapshots.map {it.id}.toSet()==j.observationIds && j.observationSnapshots.size==j.observationIds.size){"Snapshot de evidencia incompleto"}
                j.observationSnapshots.forEach {snapshot->
                    val run=EducationData.runs(all).single {it.id==snapshot.runId}
                    require(snapshot.target==EduTarget(EduTargetKind.STUDENT,j.studentId) && j.studentId in run.participantIds && snapshot.classroomId==enrollment.classroomId && run.classroomId==enrollment.classroomId && snapshot.criterion.id==j.criterionId)
                    require(snapshot.criterion.label.length in 1..1000 && snapshot.note.length<=6000 && snapshot.at>=0 && (snapshot.stepId==null || snapshot.stepId in run.stepIds))
                    require(snapshot.evidenceIds.size<=20 && snapshot.evidenceIds.all {it.matches(Regex("[a-zA-Z0-9-]+"))})
                    require(snapshot.evidenceIds.isEmpty() && snapshot.evidenceOwnerProjectId.isBlank() || snapshot.evidenceOwnerProjectId==run.personalProjectId)
                    if(j.state==EduJudgmentState.CONFIRMED)require(snapshot.status!=EduStatus.NOT_OBSERVED)
                }
            }
            if(j.state==EduJudgmentState.CONFIRMED)require(j.level!=null && j.observationIds.isNotEmpty() && j.teacher.isNotBlank() && j.description.isNotBlank())
            j.supersedesId?.let {id->val old=requireNotNull(byJudgment[id]);require(old.studentId==j.studentId && old.enrollmentId==j.enrollmentId && old.criterionId==j.criterionId && id!=j.id && old.createdAt<=j.createdAt && old.state==EduJudgmentState.SUPERSEDED)}
            val visited=mutableSetOf(j.id);var predecessor=j.supersedesId
            while(predecessor!=null){require(visited.add(predecessor)){"Ciclo en revisiones de valoración"};predecessor=requireNotNull(byJudgment[predecessor]).supersedesId}
        }
        require(judgments.mapNotNull {it.supersedesId}.groupingBy {it}.eachCount().values.all {it==1}){"Revisiones divergentes"}
        val guardians=guardians(all).associateBy {it.id}
        val academic=academicAssessments(all).associateBy {it.id}
        drafts(all).forEach {draft->require(requireNotNull(guardians[draft.guardianId]).studentId==draft.studentId);draft.observationIds.forEach {id->require(requireNotNull(observations[id]).target==EduTarget(EduTargetKind.STUDENT,draft.studentId))};draft.academicAssessmentIds.forEach {id->require(requireNotNull(academic[id]).studentId==draft.studentId)}}
        academicAssessments(all).forEach {assessment->
            val enrollment=enrollments.single {it.id==assessment.enrollmentId};require(enrollment.studentId==assessment.studentId && requireNotNull(offerings[assessment.subjectId]).classroomId==enrollment.classroomId)
            require((assessment.templateId==null)==(assessment.templateCriterionId==null)){"El criterio requiere su versión de instrumento"}
            assessment.templateId?.let {templateId->val template=requireNotNull(byTemplate[templateId]);require(template.subjectId==assessment.subjectId && template.competency==assessment.competency && template.criteria.any {it.id==assessment.templateCriterionId && it.label==assessment.criterion}){"El instrumento no corresponde a esta evaluación"}}
            require((assessment.origin==EduAssessmentOrigin.MANUAL)==(assessment.sourceRecordId==null)){"Falta el vínculo al registro de origen"}
            assessment.sourceRecordId?.let {source->val record=requireNotNull(byId[source]);when(assessment.origin){
                EduAssessmentOrigin.MANUAL->error("Un registro manual no tiene origen externo")
                EduAssessmentOrigin.WORKSHOP->require(record.kind=="edu_run" && EducationData.runs(all).single {it.id==source}.let {it.classroomId==enrollment.classroomId && assessment.studentId in it.participantIds})
                EduAssessmentOrigin.SESSION->require(record.kind=="session")
            }}
        }
    }
}
