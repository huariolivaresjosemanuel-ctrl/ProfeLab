package pe.profelab.core.content

import org.json.JSONArray
import org.json.JSONObject
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.education.EducationFamilyPolicy
import pe.profelab.core.model.education.EducationAssessmentPolicy
import pe.profelab.core.model.newId
import java.util.UUID

/** Additive education operations on the same Room-backed WorkspaceStore transaction boundary. */
class Education32Repository(private val store:WorkspaceStore) {
    private fun text(value:String,max:Int,blank:Boolean=false)=value.trim().also {require(it.length<=max && (blank || it.isNotBlank())){"Revisa el texto y su longitud"}}
    private fun uuid(value:String)=value.also {require(runCatching {UUID.fromString(it).toString()==it}.getOrDefault(false)){"Identificador privado inválido"}}
    private fun row(id:String,kind:String,title:String,data:JSONObject,old:WorkRecord?=null):WorkRecord {
        uuid(id);require(old==null || old.kind==kind)
        val merged=JSONObject(old?.json ?: "{}");data.keys().forEach {key->merged.put(key,data.get(key))}
        return WorkRecord(id,kind,title,merged.put("schemaVersion",1).toString(),System.currentTimeMillis(),(old?.revision ?: 0)+1,old?.archived ?: false)
    }
    private fun existing(all:List<WorkRecord>,id:String,kind:String):WorkRecord? = all.singleOrNull {it.id==id}?.also {require(it.kind==kind && !it.archived){"Registro archivado o de otro tipo"}}
    private fun student(all:List<WorkRecord>,id:String)=EducationData.students(all).single {it.id==id}.also {require(!it.archived){"El estudiante está archivado"}}
    private fun classroom(all:List<WorkRecord>,id:String)=EducationData.classrooms(all).single {it.id==id}.also {require(!it.archived){"El aula está archivada"}}
    private fun enrollmentRow(value:EduEnrollment,old:WorkRecord?=null)=row(value.id,"edu_enrollment","Matrícula privada",JSONObject().put("studentId",value.studentId).put("classroomId",value.classroomId).put("schoolYear",value.schoolYear).put("active",value.active).put("createdAt",value.createdAt),old)
    private fun materializeLegacy(all:List<WorkRecord>,studentId:String):List<WorkRecord> {
        val person=student(all,studentId);val legacy=Education32Data.enrollments(all).single {it.id==Education32Data.legacyEnrollmentId(person)}
        return if(all.any {it.id==legacy.id})emptyList() else listOf(enrollmentRow(legacy))
    }
    private suspend fun write(change:(List<WorkRecord>)->List<WorkRecord>) {
        store.editRecords {all->val changed=change(all);require(changed.map {it.id}.distinct().size==changed.size)
            val replaced=changed.map {it.id}.toSet();Education32Data.validateGraph(all.filter {it.id !in replaced}+changed);changed}
    }

    suspend fun enrollStudent(studentId:String,classroomId:String,id:String=newId()):String {
        uuid(id);var result=id
        write {all->
            student(all,studentId);val room=classroom(all,classroomId);val initial=materializeLegacy(all,studentId);val source=all+initial
            val memberships=Education32Data.enrollments(source).filter {it.studentId==studentId}
            val target=memberships.singleOrNull {it.classroomId==classroomId && !it.archived};result=target?.id ?: id
            if(target==null)require(all.none {it.id==id}){"El identificador ya existe"}
            val updates=memberships.filter {it.schoolYear==room.schoolYear && it.active && it.id!=result}.map {enrollmentRow(it.copy(active=false),source.singleOrNull {r->r.id==it.id})}
            val value=target?.copy(active=true) ?: EduEnrollment(result,studentId,room.id,room.schoolYear,true,System.currentTimeMillis())
            (initial+updates+enrollmentRow(value,source.singleOrNull {it.id==result})).associateBy {it.id}.values.toList()
        };return result
    }
    suspend fun saveAcademicAssessment(studentId:String,enrollmentId:String,subjectId:String,competency:String,criterion:String,period:String,taskKind:EduAcademicTaskKind,scale:EduOriginalScale,value:String,evidence:String,observation:String,improvementAction:String,teacher:String,origin:EduAssessmentOrigin=EduAssessmentOrigin.MANUAL,sourceRecordId:String?=null,id:String=newId(),templateId:String?=null,templateCriterionId:String?=null):String {
        uuid(id);val competencyText=text(competency,1000);val criterionText=text(criterion,1000);val periodText=text(period,100);val originalValue=text(value,2000);val evidenceText=text(evidence,6000);val note=text(observation,6000,true);val action=text(improvementAction,6000,true);val author=text(teacher,100)
        require(EducationAssessmentPolicy.validOriginalValue(scale.name,originalValue)){"Revisa el valor en la escala original; las escalas no se convierten"}
        write {all->
            student(all,studentId);require(all.none {it.id==id}){"Una evaluación histórica no se sobreescribe: registra una nueva revisión"}
            val enrollment=Education32Data.enrollments(all).single {it.id==enrollmentId && it.studentId==studentId && !it.archived}
            val subject=Education32Data.offerings(all).single {it.id==subjectId && !it.archived};require(subject.classroomId==enrollment.classroomId){"El área no corresponde a la matrícula elegida"}
            require((templateId==null)==(templateCriterionId==null)){"Selecciona un criterio de la versión de instrumento"}
            templateId?.let {key->val template=Education32Data.templates(all).single {it.id==key && !it.archived};require(template.subjectId==subjectId && template.competency==competencyText && template.criteria.any {it.id==templateCriterionId && it.label==criterionText}){"El instrumento y criterio no corresponden al área y competencia elegidas"}}
            require((origin==EduAssessmentOrigin.MANUAL)==(sourceRecordId==null)){"Selecciona el registro existente de Taller o Sesiones; un registro manual no requiere origen externo"}
            if(sourceRecordId!=null){val source=all.single {it.id==sourceRecordId && !it.archived};when(origin){
                EduAssessmentOrigin.MANUAL->error("El registro manual no requiere una copia de Taller o Sesiones")
                EduAssessmentOrigin.WORKSHOP->require(source.kind=="edu_run" && EducationData.runs(all).single {it.id==sourceRecordId}.let {it.classroomId==enrollment.classroomId && studentId in it.participantIds}){"La ejecución no corresponde a este alumno y aula"}
                EduAssessmentOrigin.SESSION->require(source.kind=="session"){"Selecciona una sesión existente sin duplicarla"}
            }}
            val body=JSONObject().put("studentId",studentId).put("enrollmentId",enrollmentId).put("subjectId",subjectId).put("competency",competencyText).put("criterion",criterionText).put("period",periodText).put("taskKind",taskKind.name).put("scale",scale.name).put("value",originalValue).put("evidence",evidenceText).put("observation",note).put("improvementAction",action).put("teacher",author).put("origin",origin.name).put("sourceRecordId",sourceRecordId ?: "").put("templateId",templateId ?: "").put("templateCriterionId",templateCriterionId ?: "").put("createdAt",System.currentTimeMillis())
            materializeLegacy(all,studentId)+row(id,"edu_academic_assessment","Evaluación académica privada",body)
        };return id
    }
    suspend fun saveOffering(classroomId:String,name:String,id:String=newId()):String {
        uuid(id);val label=text(name,100)
        write {all->classroom(all,classroomId);val old=existing(all,id,"edu_subject");if(old!=null)require(JSONObject(old.json).getString("classroomId")==classroomId)
            require(Education32Data.offerings(all).none {it.id!=id && it.classroomId==classroomId && !it.archived && it.name.equals(label,true)}){"El área ya está asignada al aula"}
            listOf(row(id,"edu_subject",label,JSONObject().put("classroomId",classroomId).put("name",label),old))
        };return id
    }
    suspend fun saveTemplate(subjectId:String,competency:String,grade:Int,activityId:String,kind:EduInstrumentKind,criteria:List<EduRubricCriterion>,previousTemplateId:String?=null):String {
        val id=newId();val competencyText=text(competency,1000);text(activityId,180,true);require(grade in 1..6 && criteria.size in 1..30)
        require(criteria.map {it.id}.distinct().size==criteria.size)
        criteria.forEach {c->text(c.id,180);text(c.label,1000);require(c.descriptors.size<=10);c.descriptors.forEach {(key,value)->text(key,80);text(value,2000)}}
        if(kind==EduInstrumentKind.ANALYTIC_RUBRIC)require(criteria.all {it.descriptors.keys.containsAll(EduAchievementLevel.entries.map {level->level.name})}){"Completa los descriptores AD, A, B y C de cada criterio"}
        write {all->
            val subject=Education32Data.offerings(all).single {it.id==subjectId && !it.archived};val room=classroom(all,subject.classroomId);require(grade==room.grade){"El grado debe corresponder al aula del área"}
            val previous=previousTemplateId?.let {key->Education32Data.templates(all).single {it.id==key && !it.archived}.also {require(it.subjectId==subjectId)}}
            val key=previous?.templateKey ?: newId();val version=(Education32Data.templates(all).filter {it.templateKey==key}.maxOfOrNull {it.version} ?: 0)+1
            val values=JSONArray();criteria.forEach {c->values.put(JSONObject().put("id",c.id).put("label",c.label.trim()).put("descriptors",JSONObject(c.descriptors)))}
            listOf(row(id,"edu_assessment_template","Instrumento privado v$version",JSONObject().put("templateKey",key).put("version",version).put("subjectId",subjectId).put("competency",competencyText).put("grade",grade).put("activityId",activityId.trim()).put("kind",kind.name).put("criteria",values).put("sourceStatus","SUGERIDO_NO_HOMOLOGADO")))
        };return id
    }
    suspend fun saveJudgment(studentId:String,enrollmentId:String,templateId:String,criterionId:String,level:EduAchievementLevel?,observationIds:Set<String>,teacher:String,description:String,confirmed:Boolean,supersedesId:String?=null):String {
        val id=newId();val teacherText=text(teacher,100,true);val descriptionText=text(description,6000,true);require(observationIds.size<=30)
        if(confirmed)require(level!=null && observationIds.isNotEmpty() && teacherText.isNotBlank() && descriptionText.isNotBlank()){"La confirmación requiere decisión docente, evidencia individual, autor y descripción"}
        write {all->
            student(all,studentId);val enrollment=Education32Data.enrollments(all).single {it.id==enrollmentId && it.studentId==studentId && !it.archived};val template=Education32Data.templates(all).single {it.id==templateId && !it.archived}
            val subject=Education32Data.offerings(all).single {it.id==template.subjectId};require(subject.classroomId==enrollment.classroomId && template.criteria.any {it.id==criterionId}){"El instrumento no corresponde al aula o criterio"}
            val evidence=EducationData.observations(all).filter {it.id in observationIds};require(evidence.size==observationIds.size)
            evidence.forEach {o->require(!o.archived && o.target==EduTarget(EduTargetKind.STUDENT,studentId) && o.classroomId==enrollment.classroomId && o.criterion.id==criterionId){"Selecciona evidencia individual del mismo estudiante, aula y criterio"};if(confirmed)require(o.status!=EduStatus.NOT_OBSERVED){"No observado no demuestra un nivel de aprendizaje"}}
            val previous=supersedesId?.let {key->Education32Data.judgments(all).single {it.id==key && !it.archived}.also {require(it.studentId==studentId && it.enrollmentId==enrollmentId && it.criterionId==criterionId && it.state!=EduJudgmentState.SUPERSEDED)}}
            require(previous==null || confirmed){"Confirma la revisión antes de sustituir una valoración"}
            val summary=evidence.sortedBy {it.at}.joinToString("\n\n"){"${it.criterion.label}: ${it.status.label}. ${it.note}\nRegistro: ${it.id} · ${it.at}"}
            val body=JSONObject().put("studentId",studentId).put("enrollmentId",enrollmentId).put("templateId",templateId).put("criterionId",criterionId).put("level",level?.name ?: "").put("observationIds",JSONArray(observationIds.sorted())).put("teacher",teacherText).put("description",descriptionText).put("state",if(confirmed)EduJudgmentState.CONFIRMED.name else EduJudgmentState.DRAFT.name).put("createdAt",System.currentTimeMillis()).put("supersedesId",supersedesId ?: "").put("evidenceSummary",summary)
                .put("observationSnapshots",JSONArray().also {array->evidence.forEach {observation->array.put(JSONObject(all.single {it.id==observation.id}.json).put("id",observation.id))}})
            val old=previous?.let {prior->val original=all.single {it.id==prior.id};row(prior.id,"edu_judgment",original.title,JSONObject().put("state",EduJudgmentState.SUPERSEDED.name),original)}
            materializeLegacy(all,studentId)+listOfNotNull(old,row(id,"edu_judgment","Valoración docente privada",body))
        };return id
    }
    suspend fun saveGuardian(studentId:String,name:String,relationship:String,phone:String,email:String,preferredChannel:EduFamilyChannel,authorized:Boolean,scope:String,validFrom:Long,validUntil:Long,verified:Boolean,id:String=newId()):String {
        uuid(id);val label=text(name,100);val relation=text(relationship,100);val tel=text(phone,30,true);val mail=text(email,254,true);val purpose=text(scope,100)
        require(EducationFamilyPolicy.validPhone(tel) && EducationFamilyPolicy.validEmail(mail)){"Revisa el teléfono o correo de un solo destinatario"};require(validFrom>=0 && validUntil>=validFrom)
        if(authorized)require(verified && purpose=="LEARNING_REPORT"){"Verifica el permiso y su alcance antes de autorizar"}
        write {all->student(all,studentId);val old=existing(all,id,"edu_guardian");if(old!=null)require(JSONObject(old.json).getString("studentId")==studentId){"Un contacto no se reasigna a otro estudiante"}
            val revoked=old?.let {JSONObject(it.json).optLong("revokedAt",0)} ?: 0
            require(revoked==0L || !authorized){"Para una nueva autorización tras revocación crea un contacto nuevo y verifica el permiso"}
            listOf(row(id,"edu_guardian","Contacto familiar privado",JSONObject().put("studentId",studentId).put("name",label).put("relationship",relation).put("phone",tel).put("email",mail).put("preferredChannel",preferredChannel.name).put("authorized",authorized).put("scope",purpose).put("validFrom",validFrom).put("validUntil",validUntil).put("verified",verified).put("revokedAt",revoked),old))
        };return id
    }
    suspend fun revokeGuardian(id:String){write {all->val old=requireNotNull(existing(all,id,"edu_guardian"));listOf(row(id,"edu_guardian",old.title,JSONObject().put("authorized",false).put("revokedAt",System.currentTimeMillis()),old))}}
    suspend fun saveDraft(studentId:String,guardianId:String,observationIds:Set<String>,body:String,id:String=newId(),academicAssessmentIds:Set<String> = emptySet()):String {
        uuid(id);require(observationIds.size<=30 && academicAssessmentIds.size<=30 && EducationFamilyPolicy.safeBody(body)){"Revisa el contenido individual del borrador"}
        write {all->student(all,studentId);val guardian=Education32Data.guardians(all).single {it.id==guardianId && it.studentId==studentId && !it.archived}
            val observations=Education32Data.timeline(all,studentId).filter {it.id in observationIds};require(observations.size==observationIds.size){"El informe contiene referencias de otro estudiante o archivadas"}
            val assessments=Education32Data.academicAssessments(all).filter {it.id in academicAssessmentIds && it.studentId==studentId && !it.archived};require(assessments.size==academicAssessmentIds.size){"El informe contiene evaluaciones de otro estudiante o archivadas"}
            val old=existing(all,id,"edu_family_draft");if(old!=null){val previous=JSONObject(old.json);require(previous.getString("studentId")==studentId && previous.getString("guardianId")==guardian.id){"El borrador no se reasigna a otro alumno o destinatario"}}
            listOf(row(id,"edu_family_draft","Borrador familiar privado",JSONObject().put("studentId",studentId).put("guardianId",guardian.id).put("observationIds",JSONArray(observationIds.sorted())).put("academicAssessmentIds",JSONArray(academicAssessmentIds.sorted())).put("body",body.trim()).put("createdAt",System.currentTimeMillis()).put("state",EduShareState.PREPARED.name).put("channel",""),old))
        };return id
    }
    private fun shareRequest(all:List<WorkRecord>,draftId:String,channel:EduFamilyChannel,reviewed:Boolean):EduFamilyShareRequest {
        require(reviewed){"Revisa y confirma el destinatario y la vista previa antes de abrir otra app"}
        val draft=Education32Data.drafts(all).single {it.id==draftId && !it.archived};student(all,draft.studentId)
        val guardian=Education32Data.guardians(all).single {it.id==draft.guardianId && it.studentId==draft.studentId}
        require(Education32Data.shareBlockReason(guardian)==null){Education32Data.shareBlockReason(guardian).orEmpty()}
        require(draft.observationIds.all {id->Education32Data.timeline(all,draft.studentId).any {it.id==id}}){"Revisa las evidencias seleccionadas: alguna cambió o fue archivada"}
        require(draft.academicAssessmentIds.all {id->Education32Data.academicAssessments(all).any {it.id==id && it.studentId==draft.studentId && !it.archived}}){"Revisa las evaluaciones seleccionadas: alguna fue archivada"}
        return EduFamilyShareRequest(draft.id,guardian.id,channel,EducationFamilyPolicy.externalUri(channel.name,guardian.phone,guardian.email,draft.body),draft.body)
    }
    suspend fun prepareShare(draftId:String,channel:EduFamilyChannel,reviewed:Boolean):EduFamilyShareRequest {
        var request:EduFamilyShareRequest?=null
        store.editRecords {all->request=shareRequest(all,draftId,channel,reviewed);emptyList()}
        return requireNotNull(request)
    }
    /** Call only after Android accepted the external Intent. This never asserts sent/delivered/read. */
    suspend fun markExternalOpened(draftId:String,channel:EduFamilyChannel,reviewed:Boolean){write {all->shareRequest(all,draftId,channel,reviewed);val old=all.single {it.id==draftId};listOf(row(old.id,"edu_family_draft",old.title,JSONObject().put("state",EduShareState.EXTERNAL_APP_OPENED.name).put("channel",channel.name),old))}}
    suspend fun archive(id:String){write {all->val old=all.single {it.id==id};require(old.kind in Education32Data.kinds && old.kind!="edu_enrollment"){"Las matrículas históricas se conservan"};listOf(old.copy(archived=true,updatedAt=System.currentTimeMillis()))}}
}
