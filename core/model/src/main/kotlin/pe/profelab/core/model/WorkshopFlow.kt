package pe.profelab.core.model

/** IDs are references only. No students, classrooms or private evaluations are seeded. */
enum class WorkshopWindow { DETAIL, MATERIALS, PURPOSE, STEPS, CLASSROOM }
enum class WorkshopPerspective(val label:String) { PRACTICAL("Práctico"), TEACHER("Docente"), ACADEMIC("Académico") }
data class ObservationCriterion(val id:String,val label:String,val reviewStatus:String="EDITORIAL_PROPOSAL")
data class ActivityRunReference(val activityId:String,val activityRunId:String?,val contentVersion:String,val criterionIds:List<String>)
data class FutureClassroomAssociation(val activity:ActivityRunReference,val groupId:String?=null,val studentId:String?=null,
    val competencyRef:String?=null,val evidenceIds:List<String> = emptyList())
data class ActivityStepView(val id:String,val title:String,val instruction:String,val done:Boolean,val imageFileIds:List<String>,val notes:String)
data class ActivityViewState(val activityId:String,val recordId:String,val contentVersion:String,val categoryId:String,
    val title:String,val summary:String,val personal:Boolean,val experiment:Boolean,val steps:List<ActivityStepView>,
    val lastVisitedStepId:String?,val perspective:WorkshopPerspective,val criteria:List<ObservationCriterion>) {
    val completedSteps get()=if(personal)steps.count {it.done}else 0
    val progress get()=if(steps.isEmpty())0f else completedSteps.toFloat()/steps.size
    val resumeIndex get()=steps.indexOfFirst {it.id==lastVisitedStepId}.takeIf {it>=0} ?: steps.indexOfFirst {!it.done}.coerceAtLeast(0)
    fun reference()=ActivityRunReference(activityId,recordId.takeIf {personal},contentVersion,criteria.map {it.id})
}
