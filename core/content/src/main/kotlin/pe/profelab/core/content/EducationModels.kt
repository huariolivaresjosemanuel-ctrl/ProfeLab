package pe.profelab.core.content

/** Private teaching records. They are never catalog definitions or public pack resources. */
data class EduClassroom(val id:String,val name:String,val grade:Int,val section:String,val schoolYear:Int,val archived:Boolean)
data class EduStudent(val id:String,val classroomId:String,val displayName:String,val archived:Boolean)
data class EduTeam(val id:String,val classroomId:String,val name:String,val studentIds:Set<String>,val archived:Boolean)
data class EduParticipantSnapshot(val studentId:String,val displayName:String)
data class EduTeamSnapshot(val teamId:String,val name:String,val studentIds:Set<String>)
data class EduRun(val id:String,val personalProjectId:String,val activityId:String,val contentVersion:String,val activityTitle:String,
    val classroomId:String?,val participantCount:Int,val participants:List<EduParticipantSnapshot>,val teams:List<EduTeamSnapshot>,
    val stepIds:Set<String>,val createdAt:Long,val archived:Boolean,val classroomLabel:String="") {
    val participantIds get()=participants.map {it.studentId}.toSet()
}
enum class EduStatus(val label:String) {
    AUTONOMOUS("Lo realizó de manera autónoma"),WITH_SUPPORT("Lo realizó con apoyo"),IN_PROGRESS("Está en proceso"),NOT_OBSERVED("No observado")
}
enum class EduTargetKind { STUDENT, TEAM, ACTIVITY }
data class EduTarget(val kind:EduTargetKind,val id:String?=null)
data class EduCriterion(val id:String,val label:String)
data class EduObservation(val id:String,val classroomId:String?,val runId:String,val target:EduTarget,val criterion:EduCriterion,
    val status:EduStatus,val note:String,val stepId:String?,val at:Long,val evidenceIds:Set<String>,val archived:Boolean,val evidenceOwnerProjectId:String="")
data class EduPhrase(val id:String,val text:String,val archived:Boolean)
