package pe.profelab.core.content

/** Additive private projections. IDs of EduStudent remain the permanent identity. */
data class EduEnrollment(val id:String,val studentId:String,val classroomId:String,val schoolYear:Int,val active:Boolean,val createdAt:Long,val archived:Boolean=false)
data class EduSubjectOffering(val id:String,val classroomId:String,val name:String,val archived:Boolean)
enum class EduInstrumentKind(val label:String) { CHECKLIST("Lista de cotejo"), DESCRIPTIVE_SCALE("Escala descriptiva"), ANALYTIC_RUBRIC("Rúbrica analítica") }
data class EduRubricCriterion(val id:String,val label:String,val descriptors:Map<String,String> = emptyMap())
data class EduAssessmentTemplate(val id:String,val templateKey:String,val version:Int,val subjectId:String,val competency:String,val grade:Int,val activityId:String,val kind:EduInstrumentKind,val criteria:List<EduRubricCriterion>,val sourceStatus:String="SUGERIDO_NO_HOMOLOGADO",val archived:Boolean=false)
enum class EduAchievementLevel { AD,A,B,C }
enum class EduJudgmentState { DRAFT,CONFIRMED,SUPERSEDED }
data class EduCompetencyJudgment(val id:String,val studentId:String,val enrollmentId:String,val templateId:String,val criterionId:String,val level:EduAchievementLevel?,val observationIds:Set<String>,val teacher:String,val description:String,val state:EduJudgmentState,val createdAt:Long,val supersedesId:String?=null,val archived:Boolean=false,val evidenceSummary:String="",val observationSnapshots:List<EduObservation> = emptyList()) {
    val evidenceIds get()=observationSnapshots.flatMap {it.evidenceIds}.toSet()
}
enum class EduFamilyChannel(val label:String) { WHATSAPP("WhatsApp"), SMS("SMS"), EMAIL("Correo") }
data class EduGuardianContact(val id:String,val studentId:String,val name:String,val relationship:String,val phone:String,val email:String,val preferredChannel:EduFamilyChannel,val authorized:Boolean,val scope:String,val validFrom:Long,val validUntil:Long,val verified:Boolean,val revokedAt:Long?,val archived:Boolean)
enum class EduShareState { PREPARED,EXTERNAL_APP_OPENED }
data class EduFamilyDraft(val id:String,val studentId:String,val guardianId:String,val observationIds:Set<String>,val body:String,val createdAt:Long,val state:EduShareState,val channel:EduFamilyChannel?,val archived:Boolean=false,val academicAssessmentIds:Set<String> = emptySet())
data class EduFamilyShareRequest(val draftId:String,val guardianId:String,val channel:EduFamilyChannel,val uri:String,val body:String)
enum class EduAcademicTaskKind(val label:String) { HOMEWORK("Tarea"),PRACTICE("Práctica"),EXAM("Examen"),PROJECT("Proyecto"),PRESENTATION("Exposición"),PARTICIPATION("Participación") }
enum class EduOriginalScale(val label:String) { AD_A_B_C("AD/A/B/C"),NUMERIC_0_20("Numérica 0–20 (escala elegida por el docente)"),DESCRIPTIVE("Descriptiva"),RUBRIC("Rúbrica") }
enum class EduAssessmentOrigin { MANUAL,WORKSHOP,SESSION }
data class EduAcademicAssessment(val id:String,val studentId:String,val enrollmentId:String,val subjectId:String,val competency:String,val criterion:String,val period:String,val taskKind:EduAcademicTaskKind,val scale:EduOriginalScale,val value:String,val evidence:String,val observation:String,val improvementAction:String,val teacher:String,val origin:EduAssessmentOrigin,val sourceRecordId:String?,val createdAt:Long,val archived:Boolean=false,val templateId:String?=null,val templateCriterionId:String?=null)
