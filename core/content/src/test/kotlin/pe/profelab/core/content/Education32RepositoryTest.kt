package pe.profelab.core.content

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import pe.profelab.core.model.WorkRecord
import pe.profelab.core.model.WorkspaceStore
import pe.profelab.core.model.newId

/** Synthetic fixtures only; real JSONObject and atomic fake WorkspaceStore, with rollback on errors. */
class Education32RepositoryTest {
    private class MemoryStore:WorkspaceStore {
        private val mutex=Mutex();private val state=MutableStateFlow<List<WorkRecord>>(emptyList())
        val rows get()=state.value.toList()
        override fun observeRecords():Flow<List<WorkRecord>> = state
        override suspend fun record(id:String)=mutex.withLock {state.value.singleOrNull {it.id==id}}
        override suspend fun saveRecord(record:WorkRecord)=saveBatch(listOf(record))
        override suspend fun archiveRecord(id:String)=editRecords {all->listOf(all.single {it.id==id}.copy(archived=true))}
        override suspend fun saveBatch(records:List<WorkRecord>)=editRecords {records}
        override suspend fun editRecords(change:(List<WorkRecord>)->List<WorkRecord>){mutex.withLock {
            val before=state.value.toList();val changed=change(before);require(changed.map {it.id}.distinct().size==changed.size)
            val ids=changed.map {it.id}.toSet();state.value=before.filter {it.id !in ids}+changed
        }}
    }
    private data class Fixture(val store:MemoryStore,val old:EducationRepository,val repo:Education32Repository,val room:String,val otherRoom:String,val nextYear:String,val student:String,val otherStudent:String,val subject:String,val otherSubject:String)
    private suspend fun fixture():Fixture {
        val store=MemoryStore();val old=EducationRepository(store);val repo=Education32Repository(store)
        val room=old.saveClassroom("Aula sintética",2,"A",2026);val other=old.saveClassroom("Aula sintética",2,"B",2026);val next=old.saveClassroom("Aula sintética",3,"A",2027)
        val student=old.saveStudent(room,"Alias sintético A");val otherStudent=old.saveStudent(other,"Alias sintético B")
        val subject=repo.saveOffering(room,"Matemática");val otherSubject=repo.saveOffering(other,"Comunicación")
        return Fixture(store,old,repo,room,other,next,student,otherStudent,subject,otherSubject)
    }
    private fun enrollment(f:Fixture,student:String=f.student)=Education32Data.enrollments(f.store.rows).first {it.studentId==student && it.active}
    private suspend fun academic(f:Fixture,student:String=f.student,enrollmentId:String=enrollment(f,student).id,subject:String=f.subject,scale:EduOriginalScale=EduOriginalScale.AD_A_B_C,value:String="A",origin:EduAssessmentOrigin=EduAssessmentOrigin.MANUAL,source:String?=null)=f.repo.saveAcademicAssessment(student,enrollmentId,subject,"Representa cantidades","Explica su procedimiento","Primer periodo",EduAcademicTaskKind.PRACTICE,scale,value,"Producción individual revisada por el docente","Explica con material concreto","Practicar otra representación","Docente sintético",origin,source)
    private suspend fun run(f:Fixture,classroom:String=f.room,student:String=f.student):String {
        val work=WorkRecord(newId(),"project","Actividad sintética",JSONObject().put("activityId","qa-activity").put("contentVersion","qa-v1").put("steps",JSONArray().put(JSONObject().put("id","step-0").put("title","Explora"))).put("observationCriteria",JSONArray().put(JSONObject().put("criterionId","qa:criterion").put("label","Explica su procedimiento"))).toString(),1)
        f.store.saveRecord(work);return f.old.createRun(work,classroom,setOf(student),emptySet(),1)
    }
    private suspend fun guardian(f:Fixture,student:String=f.student,authorized:Boolean=true,until:Long=System.currentTimeMillis()+60000)=f.repo.saveGuardian(student,"Contacto sintético","Tutor","+51 900 000 000","guardian@example.invalid",EduFamilyChannel.EMAIL,authorized,"LEARNING_REPORT",0,until,authorized)
    private suspend fun rejects(block:suspend ()->Unit){assertTrue("The invalid transaction must fail",runCatching {block()}.isFailure)}

    @Test fun legacyProjectionIsStableAndDoesNotWriteOnRead():Unit=runBlocking {
        val f=fixture();val before=f.store.rows;val first=Education32Data.enrollments(before);val second=Education32Data.enrollments(before)
        assertEquals(first,second);assertEquals(before,f.store.rows);assertEquals(2,first.size);assertEquals(0,before.count {it.kind=="edu_enrollment"});assertEquals(f.student,Education32Data.studentsInClassroom(before,f.room).single().id)
    }
    @Test fun movingWithinAYearMaterializesLegacyWithoutChangingIdentityOrRunSnapshot():Unit=runBlocking {
        val f=fixture();val run=run(f);val snapshot=requireNotNull(f.store.record(run)).json;val origin=enrollment(f).id
        val current=f.repo.enrollStudent(f.student,f.otherRoom);val all=f.store.rows
        assertEquals(2,EducationData.students(all).size);assertEquals(f.room,EducationData.students(all).single {it.id==f.student}.classroomId)
        assertFalse(Education32Data.enrollments(all).single {it.id==origin}.active);assertTrue(Education32Data.enrollments(all).single {it.id==current}.active)
        assertEquals(snapshot,requireNotNull(f.store.record(run)).json);assertTrue(Education32Data.studentsInClassroom(all,f.room).none {it.id==f.student});assertTrue(Education32Data.studentsInClassroom(all,f.otherRoom).any {it.id==f.student})
        val runInNewRoom=run(f,f.otherRoom,f.student);assertEquals(f.student,EducationData.runs(f.store.rows).single {it.id==runInNewRoom}.participants.single().studentId)
        f.old.saveStudent(f.otherRoom,"Alias corregido",f.student);assertEquals(f.room,EducationData.students(f.store.rows).single {it.id==f.student}.classroomId)
    }
    @Test fun nextYearEnrollmentKeepsPreviousYearAndAreasReuseOneIdentity():Unit=runBlocking {
        val f=fixture();val original=enrollment(f);f.repo.enrollStudent(f.student,f.nextYear);f.repo.saveOffering(f.room,"Ciencia y Tecnología")
        val all=f.store.rows;assertTrue(Education32Data.enrollments(all).any {it.id==original.id && it.schoolYear==2026});assertTrue(Education32Data.enrollments(all).any {it.studentId==f.student && it.schoolYear==2027})
        assertEquals(1,EducationData.students(all).count {it.id==f.student});assertEquals(2,Education32Data.offerings(all).count {it.classroomId==f.room})
    }
    @Test fun manualAssessmentNeedsNoWorkshopAndPreservesEveryOriginalScale():Unit=runBlocking {
        val f=fixture();val values=listOf(EduOriginalScale.AD_A_B_C to "B",EduOriginalScale.NUMERIC_0_20 to "15,50",EduOriginalScale.DESCRIPTIVE to "Explica con apoyo visual",EduOriginalScale.RUBRIC to "Criterio 1: describe su estrategia")
        values.forEach {(scale,value)->academic(f,scale=scale,value=value)}
        val saved=Education32Data.academicAssessments(f.store.rows)
        assertEquals(values.toSet(),saved.map {it.scale to it.value}.toSet());assertTrue(saved.all {it.origin==EduAssessmentOrigin.MANUAL && it.sourceRecordId==null});assertTrue(EducationData.runs(f.store.rows).isEmpty());assertEquals(1,f.store.rows.count {it.kind=="edu_enrollment"})
    }
    @Test fun invalidGradesAndForeignSubjectsRollbackWithoutPartialMigration():Unit=runBlocking {
        val f=fixture();val before=f.store.rows
        rejects {academic(f,scale=EduOriginalScale.NUMERIC_0_20,value="21")};rejects {academic(f,scale=EduOriginalScale.AD_A_B_C,value="18")};rejects {academic(f,subject=f.otherSubject)}
        assertEquals(before,f.store.rows)
    }
    @Test fun sourceLinksRequireExistingCompatibleRecordsWithoutDuplicatingSessions():Unit=runBlocking {
        val f=fixture();val session=WorkRecord(newId(),"session","Sesión sintética","{}",1);f.store.saveRecord(session)
        academic(f,origin=EduAssessmentOrigin.SESSION,source=session.id);assertEquals(1,f.store.rows.count {it.kind=="session"})
        rejects {academic(f,origin=EduAssessmentOrigin.SESSION)};rejects {academic(f,origin=EduAssessmentOrigin.WORKSHOP,source=session.id)};rejects {academic(f,source=session.id)}
        val run=run(f);academic(f,origin=EduAssessmentOrigin.WORKSHOP,source=run)
        assertEquals(setOf(session.id,run),Education32Data.academicAssessments(f.store.rows).mapNotNull {it.sourceRecordId}.toSet())
    }
    @Test fun classroomArchivingKeepsAcademicHistoryAndPreventsChangingHistoricYear():Unit=runBlocking {
        val f=fixture();val assessment=academic(f)
        rejects {f.old.saveClassroom("Aula sintética",3,"A",2027,f.room)}
        f.old.archive(f.room);assertEquals(assessment,Education32Data.academicAssessments(f.store.rows).single().id);Education32Data.validateGraph(f.store.rows)
    }
    @Test fun instrumentsAreImmutableVersionedAndDoNotAssignLevelsFromQuickStatus():Unit=runBlocking {
        val f=fixture();val criterion=EduRubricCriterion("qa:criterion","Explica cantidades")
        val first=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"",EduInstrumentKind.CHECKLIST,listOf(criterion))
        val firstBody=requireNotNull(f.store.record(first)).json
        val second=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"",EduInstrumentKind.DESCRIPTIVE_SCALE,listOf(criterion.copy(label="Explica y representa")),first)
        val templates=Education32Data.templates(f.store.rows);assertEquals(setOf(1,2),templates.map {it.version}.toSet());assertEquals(1,templates.map {it.templateKey}.distinct().size);assertEquals(firstBody,requireNotNull(f.store.record(first)).json)
        assertEquals("SUGERIDO_NO_HOMOLOGADO",templates.first().sourceStatus);assertTrue(Education32Data.judgments(f.store.rows).isEmpty())
        f.repo.saveJudgment(f.student,enrollment(f).id,second,criterion.id,null,emptySet(),"","",false)
        assertNull(Education32Data.judgments(f.store.rows).single().level)
    }
    @Test fun teamAndNotObservedCannotBecomeConfirmedIndividualJudgment():Unit=runBlocking {
        val f=fixture();val run=run(f);val criterion=EduCriterion("qa:criterion","Explica cantidades")
        val template=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"qa-activity",EduInstrumentKind.CHECKLIST,listOf(EduRubricCriterion(criterion.id,criterion.label)))
        val general=f.old.saveObservation(run,EduTarget(EduTargetKind.ACTIVITY),criterion,EduStatus.AUTONOMOUS,"Registro general")
        val notObserved=f.old.saveObservation(run,EduTarget(EduTargetKind.STUDENT,f.student),criterion,EduStatus.NOT_OBSERVED,"")
        rejects {f.repo.saveJudgment(f.student,enrollment(f).id,template,criterion.id,EduAchievementLevel.A,setOf(general),"Docente sintético","Descripción",true)}
        rejects {f.repo.saveJudgment(f.student,enrollment(f).id,template,criterion.id,EduAchievementLevel.C,setOf(notObserved),"Docente sintético","Descripción",true)}
        assertTrue(Education32Data.judgments(f.store.rows).isEmpty())
    }
    @Test fun teacherConfirmationStoresEvidenceSnapshotAndReviewSupersedesWithoutRewritingIt():Unit=runBlocking {
        val f=fixture();val run=run(f);val criterion=EduCriterion("qa:criterion","Explica cantidades")
        val photo=newId();val runData=EducationData.runs(f.store.rows).single {it.id==run};val work=requireNotNull(f.store.record(runData.personalProjectId))
        f.store.saveRecord(work.copy(json=JSONObject(work.json).put("attachments",JSONArray().put(JSONObject().put("id",photo).put("mime","image/png"))).toString()))
        val template=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"qa-activity",EduInstrumentKind.CHECKLIST,listOf(EduRubricCriterion(criterion.id,criterion.label)))
        val observed=f.old.saveObservation(run,EduTarget(EduTargetKind.STUDENT,f.student),criterion,EduStatus.WITH_SUPPORT,"Evidencia original",evidenceIds=setOf(photo))
        val first=f.repo.saveJudgment(f.student,enrollment(f).id,template,criterion.id,EduAchievementLevel.A,setOf(observed),"Docente sintético","Decisión manual documentada",true)
        f.old.saveObservation(run,EduTarget(EduTargetKind.STUDENT,f.student),criterion,EduStatus.AUTONOMOUS,"Nueva observación editada",id=observed)
        val second=f.repo.saveJudgment(f.student,enrollment(f).id,template,criterion.id,EduAchievementLevel.AD,setOf(observed),"Docente sintético","Revisión posterior",true,first)
        val judgments=Education32Data.judgments(f.store.rows);assertTrue(judgments.single {it.id==first}.evidenceSummary.contains("Evidencia original"));assertEquals(setOf(photo),judgments.single {it.id==first}.evidenceIds);assertEquals("Evidencia original",judgments.single {it.id==first}.observationSnapshots.single().note);assertEquals(EduAchievementLevel.A,judgments.single {it.id==first}.level);assertEquals(EduJudgmentState.SUPERSEDED,judgments.single {it.id==first}.state);assertEquals(first,judgments.single {it.id==second}.supersedesId)
    }
    @Test fun familyReportAcceptsManualAcademicEvidenceWithoutWorkshopAndNeverClaimsDelivery():Unit=runBlocking {
        val f=fixture();val academic=academic(f);val contact=guardian(f)
        val draft=f.repo.saveDraft(f.student,contact,emptySet(),"Actividad revisada. Sugerencia para casa.",academicAssessmentIds=setOf(academic))
        rejects {f.repo.prepareShare(draft,EduFamilyChannel.EMAIL,false)}
        val request=f.repo.prepareShare(draft,EduFamilyChannel.EMAIL,true)
        assertTrue(request.uri.startsWith("mailto:guardian%40example.invalid?"));assertEquals(EduShareState.PREPARED,Education32Data.drafts(f.store.rows).single().state)
        f.repo.markExternalOpened(draft,EduFamilyChannel.EMAIL,true);assertEquals(EduShareState.EXTERNAL_APP_OPENED,Education32Data.drafts(f.store.rows).single().state);assertEquals(setOf(academic),Education32Data.drafts(f.store.rows).single().academicAssessmentIds)
    }
    @Test fun unauthorizedExpiredRevokedAndArchivedContactsFailClosed():Unit=runBlocking {
        val f=fixture();val contacts=listOf(guardian(f,authorized=false),guardian(f,until=1),guardian(f),guardian(f))
        f.repo.revokeGuardian(contacts[2]);f.repo.archive(contacts[3])
        contacts.take(3).forEach {contact->val draft=f.repo.saveDraft(f.student,contact,emptySet(),"Texto sintético revisado");rejects {f.repo.prepareShare(draft,EduFamilyChannel.SMS,true)}}
        rejects {f.repo.saveDraft(f.student,contacts[3],emptySet(),"Texto sintético revisado")}
    }
    @Test fun otherStudentRecordsAndStaleAuthorizationCannotLeakIntoShare():Unit=runBlocking {
        val f=fixture();val own=academic(f);val other=academic(f,student=f.otherStudent,subject=f.otherSubject);val contact=guardian(f)
        rejects {f.repo.saveDraft(f.student,contact,emptySet(),"Texto sintético",academicAssessmentIds=setOf(other))}
        rejects {f.repo.saveDraft(f.otherStudent,contact,emptySet(),"Texto sintético")}
        val draft=f.repo.saveDraft(f.student,contact,emptySet(),"Texto sintético",academicAssessmentIds=setOf(own))
        f.repo.prepareShare(draft,EduFamilyChannel.EMAIL,true);f.repo.revokeGuardian(contact);rejects {f.repo.markExternalOpened(draft,EduFamilyChannel.EMAIL,true)}
        assertEquals(EduShareState.PREPARED,Education32Data.drafts(f.store.rows).single().state)
    }
    @Test fun malformedRestoreCannotIntroduceCrossStudentAssessmentOrForeignReport():Unit=runBlocking {
        val f=fixture();val academic=academic(f);val contact=guardian(f);val draft=f.repo.saveDraft(f.student,contact,emptySet(),"Texto sintético",academicAssessmentIds=setOf(academic))
        val all=f.store.rows
        val badAssessment=all.map {r->if(r.id==academic)r.copy(json=JSONObject(r.json).put("studentId",f.otherStudent).toString())else r}
        assertTrue(runCatching {Education32Data.validateGraph(badAssessment)}.isFailure)
        val badDraft=all.map {r->if(r.id==draft)r.copy(json=JSONObject(r.json).put("studentId",f.otherStudent).toString())else r}
        assertTrue(runCatching {Education32Data.validateGraph(badDraft)}.isFailure)
    }
    @Test fun manualAssessmentCanReferenceAnImmutableInstrumentWithoutAnyWorkshop():Unit=runBlocking {
        val f=fixture();val criterion=EduRubricCriterion("qa:manual","Explica su procedimiento")
        val template=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"",EduInstrumentKind.CHECKLIST,listOf(criterion))
        val id=f.repo.saveAcademicAssessment(f.student,enrollment(f).id,f.subject,"Representa cantidades",criterion.label,"P1",EduAcademicTaskKind.HOMEWORK,EduOriginalScale.DESCRIPTIVE,"Explica usando material concreto","Trabajo individual revisado","","Practicar otra representación","Docente sintético",templateId=template,templateCriterionId=criterion.id)
        val saved=Education32Data.academicAssessments(f.store.rows).single();assertEquals(id,saved.id);assertEquals(template,saved.templateId);assertEquals(criterion.id,saved.templateCriterionId);assertTrue(EducationData.runs(f.store.rows).isEmpty())
        rejects {f.repo.saveAcademicAssessment(f.student,enrollment(f).id,f.subject,"Otra competencia",criterion.label,"P1",EduAcademicTaskKind.HOMEWORK,EduOriginalScale.AD_A_B_C,"A","Trabajo individual revisado","","","Docente sintético",templateId=template,templateCriterionId=criterion.id)}
    }
    @Test fun confirmingADraftPreservesItsUngradedPastAndRequiresHumanEvidence():Unit=runBlocking {
        val f=fixture();val criterion=EduCriterion("qa:criterion","Explica cantidades")
        val template=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"",EduInstrumentKind.CHECKLIST,listOf(EduRubricCriterion(criterion.id,criterion.label)))
        val draft=f.repo.saveJudgment(f.student,enrollment(f).id,template,criterion.id,null,emptySet(),"","Pendiente de revisión",false)
        val run=run(f);val observed=f.old.saveObservation(run,EduTarget(EduTargetKind.STUDENT,f.student),criterion,EduStatus.IN_PROGRESS,"Evidencia individual")
        f.repo.saveJudgment(f.student,enrollment(f).id,template,criterion.id,EduAchievementLevel.B,setOf(observed),"Docente sintético","Revisión documentada",true,draft)
        val previous=Education32Data.judgments(f.store.rows).single {it.id==draft};assertNull(previous.level);assertEquals(EduJudgmentState.SUPERSEDED,previous.state)
        assertEquals(EduAchievementLevel.B,Education32Data.judgments(f.store.rows).single {it.state==EduJudgmentState.CONFIRMED}.level)
    }
    @Test fun aMalformedRubricOrDuplicateActiveEnrollmentIsRejectedDuringRestore():Unit=runBlocking {
        val f=fixture();val descriptors=mapOf("AD" to "Explica y justifica","A" to "Explica","B" to "Explica con apoyo","C" to "Requiere acompañamiento")
        val template=f.repo.saveTemplate(f.subject,"Representa cantidades",2,"",EduInstrumentKind.ANALYTIC_RUBRIC,listOf(EduRubricCriterion("qa:criterion","Explica cantidades",descriptors)))
        val malformed=f.store.rows.map {r->if(r.id==template){val j=JSONObject(r.json);j.getJSONArray("criteria").getJSONObject(0).getJSONObject("descriptors").remove("AD");r.copy(json=j.toString())}else r}
        assertTrue(runCatching {Education32Data.validateGraph(malformed)}.isFailure)
        f.repo.enrollStudent(f.student,f.otherRoom)
        val duplicate=f.store.rows.map {r->if(r.kind=="edu_enrollment" && JSONObject(r.json).getString("classroomId")==f.room)r.copy(json=JSONObject(r.json).put("active",true).toString())else r}
        assertTrue(runCatching {Education32Data.validateGraph(duplicate)}.isFailure)
    }
}
